package com.example.cameraswitch.Capturing

import com.example.cameraswitch.UtilsAndCons.Constants
import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.location.Location
import android.location.LocationManager
import android.media.ExifInterface
import android.media.projection.MediaProjectionManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalZeroShutterLag
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.cameraswitch.PagesandActivites.Permissions

import com.example.cameraswitch.Service.AccessService
import com.example.cameraswitch.R
import com.example.cameraswitch.Receivers.NetworkChangeReceiver
import com.example.cameraswitch.Workers.SessionManager
import com.example.cameraswitch.UtilsAndCons.SharedPreferencesUtils
import com.example.cameraswitch.Workers.SavingCamToTemp
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex


import okhttp3.*
import okio.IOException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit


class BackCameraButton: AppCompatActivity() {
//  ML Modell picture size
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var outputDirectory: File
    private lateinit var storageRef: StorageReference
    private var callingPackageName: String? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastKnownLocation: Location? = null
    var lastReturnedValue: Boolean = false
    var returnedFromCamera: Boolean = false
    var triggered: Boolean = false
    private lateinit var uploadButton :Button
    private lateinit var preview: Preview
    private lateinit var retakePicture: Button
    private lateinit var captureButton: Button
    var retakeBool: Boolean = false
    private var  SKIP_LAST_STAGE_UPDATE = "SKIP_LAST_STAGE_UPDATE"
    private val PREFS_NAME = "AppPrefs"
    private lateinit var sessionManager: SessionManager
    private var osmData: Map<String, String?> = mapOf()
   // private val CAPTURED_IMAGE_PATH_KEY = "capturedImagePath"
   private lateinit var handler: Handler
    private var checkTriggerRunnable: Runnable? = null
    var metadata:Map<String, String> = mapOf()
    private var isImageReady = false
    var isCameraReady:Boolean=false
    private val backCameraScope = CoroutineScope(Dispatchers.IO)
    var typepic:String=""
    var startedcamera:Boolean=false
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)


     /*   val networkReceiver = NetworkChangeReceiver()
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)

        registerReceiver(networkReceiver, intentFilter,Context.RECEIVER_NOT_EXPORTED)*/
     //   sessionManager.getSessionCounter()
    /*    if (sessionCounter >= 4) {
            Log.d("SessionReset", "Resetting resources after session $sessionCounter")

            resetExecutor()

        }*/

        triggered=true
        Log.d("Debug", "in BackCamera")
        Log.d("DebugCameraTrigger", " in BackCamera")
        sessionManager = SessionManager(applicationContext)

        retakePicture=findViewById<Button>(R.id.retake)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        registerReceiver(uploadSuccessReceiver, IntentFilter("com.example.cameraswitch.UPLOAD_SUCCESS"),Context.RECEIVER_NOT_EXPORTED)
      //  callingPackageName = intent.getStringExtra("callingPackageName")
        val packageName = intent.getStringExtra("callingPackageName")
        setCallingPackageName(packageName ?: "unknown")
        Log.d("Receive PackageName", " in BackButton receiving $callingPackageName")
     //   returnedFromCamera = intent.getBooleanExtra("returnedFromCamera", false)
    //    Log.d("Returned from Camera (Back)", " in BackButton receiving $returnedFromCamera")
      //  intent.putExtra("returnedFromCamera", true)

        // Request camera permissions
       // requestPermissions()

        if (!allPermissionsGranted()) {
            requestPermissions()
        } else if (allPermissionsGranted() && isLocationEnabled()) {

            //   ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
            Log.d("Debug", "All permissions given")
         //   backCameraScope.launch {
         //   stopCamera()
          //  resetExecutor()
            startCamera()
      //  }
            getLastLocation()
            requestNewLocationData()

        } else {
            Log.d("Debug", "All permissions not given")
            Log.d("Location", "Please enable location services")
            Toast.makeText(this, "Please enable location services", Toast.LENGTH_SHORT).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))

            requestPermissions()
        }

        captureButton = findViewById<Button>(R.id.take)
        captureButton.setOnClickListener {
         //  backCameraScope.launch {
                takePhoto()
        //    }
        }
        uploadButton =findViewById<Button>(R.id.upload)


        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Initialize Firebase Storage
        val storage = FirebaseStorage.getInstance()
        storageRef = storage.reference

       // fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
      //  getLastLocation()

        FirebaseFirestore.getInstance().firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()

        activityContext = this

        handler = Handler(Looper.getMainLooper())
        // Start checking the trigger
      //  checkForUploadTrigger()
    //    uploadTriggerReceiver
    }



    private fun resetExecutor() {
        if (::cameraExecutor.isInitialized && !cameraExecutor.isShutdown) {
            try {
                cameraExecutor.shutdownNow()
                if (!cameraExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    Log.w("CameraExecutor", "Executor did not terminate gracefully.")
                }
            } catch (e: InterruptedException) {
                Log.e("CameraExecutor", "Error during executor shutdown", e)
            }
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
    }





    /*  private val uploadTriggerReceiver = object : BroadcastReceiver() {
          override fun onReceive(context: Context?, intent: Intent?) {
              if (intent?.action == "com.example.cameraswitch.UPLOAD_TRIGGER") {
                  Log.d("UploadFromTemp", "Upload trigger received")
                  val imagePath = getCapturedImagePath(this@BackCameraButton)
                  if (imagePath != null) {
                      val imageFile = File(imagePath)
                      if (imageFile.exists()) {
                          uploadImageToFirebaseRules(null, imageFile)
                      }
                  }
              }
          }
      }*/
 /*   private fun checkForUploadTrigger() {
        Log.d("AccessService", "checkforuploadTrigger")
        checkTriggerRunnable = Runnable {
            val sharedPreferences = getSharedPreferences("UploadTriggerPrefs", Context.MODE_PRIVATE)
            val trigger = sharedPreferences.getBoolean("triggerUpload", false)

            if (trigger) {
                sharedPreferences.edit()
                    .putBoolean("triggerUpload", false) // Reset the trigger
                    .apply()

                // Perform the upload logic
                val imagePath = getCapturedImagePath(this)
                if (imagePath != null) {
                    val imageFile = File(imagePath)
                    if (imageFile.exists()) {
                        uploadImageToFirebaseRules(null, imageFile)
                    }
                }
            }

            // Continue polling
            handler.postDelayed(checkTriggerRunnable!!, 5000) // Check every 5 seconds
        }

        handler.postDelayed(checkTriggerRunnable!!, 5000)
    }*/

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        Toast.makeText(this, "Please take a Picture first", Toast.LENGTH_SHORT).show()
    }
    private fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("Location", "Location permission not granted")
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                if (location != null) {
                    lastKnownLocation = location
                    Log.d("Location", "Got location: ${location.latitude}, ${location.longitude}")
                } else {
                    Log.e("Location", "Location was null")
                    // Possible reasons: location is disabled in settings, or the device never recorded location data
                }
            }
        }.addOnFailureListener { exception ->
            Log.e("Location", "Failed to get location", exception)
        }
    }
   /* private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
*/
 /*  private fun allPermissionsGranted() =
       REQUIRED_PERMISSIONS.all {
           ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
       }*/

    private fun requestScreenCapturePermission() {
        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = projectionManager.createScreenCaptureIntent()
        startActivityForResult(captureIntent, REQUEST_SCREEN_CAPTURE)
    }

    private fun allPermissionsGranted(): Boolean {
       val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
           REQUIRED_PERMISSIONS_TIRAMISU
       } else {
           REQUIRED_PERMISSIONS_BELOW_TIRAMISU
       }
       return requiredPermissions.all {
           ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
       }
   }
 //   private fun saveImageToTempFolder(context: Context, bitmap: Bitmap, metadata: Map<String, String>): File? {
 private fun triggerSaveImageWorker(photoFile: File, metadata: Map<String, String>,typepic:String) {
     val sessionManager=SessionManager(applicationContext)
     sessionManager.incrementSessionCounter()
     val metadataJson = JSONObject(metadata).toString()

     val inputData = Data.Builder()
         .putString("image_path", photoFile.absolutePath)
         .putString("metadata", metadataJson)
         .putString("type", typepic)
         .build()

     val saveImageWorkRequest = OneTimeWorkRequestBuilder<SavingCamToTemp>()
         .setInputData(inputData)
         .build()

     WorkManager.getInstance(applicationContext).enqueue(saveImageWorkRequest)
 }

   fun saveImageToTempFolder(context: Context, bitmap: Bitmap, metadata: Map<String, String>) {
        CoroutineScope(Dispatchers.IO).launch{
        sessionManager.incrementSessionCounter()
        Log.d("UploadFromTemp", "saveImageToTemp Back")
        Log.d("UploadFromTemp", "Metadata : $metadata")
        Log.d("Metadata", "in UploadtoTemp ; $metadata")

     //   return@launch
            try {

            val sessionCounter = sessionManager.getSessionCounter()
            val dateFormat = SimpleDateFormat("ddMMyyy_HHmmss", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HHmmss", Locale.getDefault())
            val currentTime = System.currentTimeMillis()
            val formattedDate = dateFormat.format(Date(currentTime))
            val formattedTime = timeFormat.format(Date(currentTime))
            val participantId = SharedPreferencesUtils.getParticipantId(context)
           // val tempFile = File(tempDir, "back_s${sessionCounter}_p$participantId")
          //  val tempDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "temp_images/session_$sessionCounter/camera")
            val tempDir = File(context.filesDir, "temp_images/session_$sessionCounter")

            if (!tempDir.exists() && !tempDir.mkdirs()) {
                Log.e("FileError", "Failed to create temp directory")
                return@launch // null
            }
            val tempFile=File(tempDir, "back_s_${sessionCounter}_p${participantId}_${formattedDate}_${formattedTime}")
            Log.d("UploadFromTemp", "Temp directory path: ${tempDir.absolutePath}")
            FileOutputStream(tempFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                        //  val resized= resizeBitmap(tempFile)
             //   resized?.let { Companion.compressBitmap(it) }
            }

            val metadata = mutableMapOf<String, String>().apply {
                put("sessionCounter", sessionManager.getSessionCounter().toString())
                put("participantId", SharedPreferencesUtils.getParticipantId(context).orEmpty())
            }

            addMetadataToImage(tempFile, metadata)
            debugMetadata(tempFile)
            // Save the path in SharedPreferences
         //   val sharedPref = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
           // sharedPref.edit().putString(Constants.CAPTURED_IMAGE_PATH_KEY, tempFile.absolutePath).apply()
          //  saveCapturedImagePath(context,tempFile.absolutePath,"camera")
           // saveCapturedImagePath(context,tempFile.absolutePath)

            tempFile
        } catch (e: IOException) {
            Log.e("FileError", "Error saving image to temp folder", e)
            null
        }
            }

    }


    private fun saveCapturedImagePath(context: Context, imagePath: String) {
        val sharedPref = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val imagePathSet = sharedPref.getStringSet(Constants.CAPTURED_IMAGE_PATH_KEY, mutableSetOf())!!
        imagePathSet.add(imagePath)
        sharedPref.edit().putStringSet(Constants.CAPTURED_IMAGE_PATH_KEY, imagePathSet).apply()
    }


    /*private fun startCamera() {
      Log.d("DEBUG", "Starting camera...")

      /*    if (imageCapture!=null && ::preview.isInitialized) {
            // Camera already initialized, no need to bind again
            return
        }*/

      val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
      cameraProviderFuture.addListener({
          val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
          //      cameraExecutor.execute {
          // Camera setup code
          if (::preview.isInitialized) return@addListener

          preview = Preview.Builder().build().also {
              it.setSurfaceProvider(findViewById<PreviewView>(R.id.previewView).surfaceProvider)
              Log.d("DEBUG", "Preview set")
          }

          imageCapture =
              ImageCapture.Builder()
                  .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                  .setTargetResolution(
                      Size(640, 480)
                  )
                  .build()


          val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
      //    backCameraScope.launch {
              try {
                  cameraProvider.unbindAll()
                  cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                  onCameraReady()
                  Log.d("DEBUG", "Camera bound to lifecycle")
              } catch (exc: Exception) {
                  Log.e("DEBUG", "Failed to initialize camera", exc)
              }
       //   }
          //  }
      }, ContextCompat.getMainExecutor(this))

    }*/
    private var cameraProvider: ProcessCameraProvider? = null
    private fun logActiveThreads() {
        val activeThreadCount = Thread.activeCount()
        Log.d("ThreadUsage", "Active threads: $activeThreadCount")
    }
    private fun logCameraProviderStatus() {
        if (cameraProvider == null) {
            Log.d("CameraProvider", "CameraProvider is null.")
        } else {
            Log.d("CameraProvider", "CameraProvider is initialized.")
        }
    }


    @SuppressLint("SuspiciousIndentation")
    private fun startCamera() {
       // logExecution("Camera Initialization") {
       // logMemoryUsage()
        logActiveThreads()
        logCameraProviderStatus()
        stopCamera()
        resetExecutor()
        startedcamera=true
            if (cameraProvider == null) {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
                cameraProviderFuture.addListener({
                    cameraProvider = cameraProviderFuture.get()
                    setupCamera()
                }, ContextCompat.getMainExecutor(this))
            } else {
                setupCamera()
            }
    //    }
    }

    @OptIn(ExperimentalZeroShutterLag::class) private fun setupCamera() {
      //  logExecution("Camera Binding") {
            logMemoryUsage()
            val cameraProvider = cameraProvider ?: return
            if (::preview.isInitialized) return // Avoid redundant setup

            preview = Preview.Builder().build().also {
                it.setSurfaceProvider(findViewById<PreviewView>(R.id.previewView).surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetResolution(Size(640, 480))
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            //    preWarmCamera()
                //onCameraReady()
            } catch (exc: Exception) {
                Log.e("CameraSetup", "Use case binding failed", exc)
       //     }
        }
    }
    private fun preWarmCamera() {
        val dummyFile = File(outputDirectory, "warmup.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(dummyFile).build()

        imageCapture?.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.d("PreWarm", "Warm-up capture failed but not critical")
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    dummyFile.delete() // Clean up the dummy file
                    Log.d("PreWarm", "Warm-up complete")
                }
            }
        )
    }

    private fun onCameraReady() {
        isCameraReady = true // Update the camera ready state
       /// progressBar.visibility = View.GONE // Hide the loading indicator
        captureButton.visibility = View.VISIBLE // Enable the capture button
        captureButton.isEnabled = true
    }

    private fun stopCamera() {
       /* val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll() // Unbind all use cases
            cameraExecutor.shutdown()
        }, ContextCompat.getMainExecutor(this))*/

        if (::cameraExecutor.isInitialized && !cameraExecutor.isShutdown) {
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
        }

    }
    private fun minimizeApp() {
        moveTaskToBack(true)
    }
    @SuppressLint("ServiceCast")
    private fun returnToPreviousApp() {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val recentTasks = activityManager.appTasks

        for (task in recentTasks) {
            if (task.taskInfo.baseIntent.`package` != packageName) {
                task.moveToFront() // Bring the previous app to the foreground
                break
            }
        }

        // Optionally, minimize this app after switching
        moveTaskToBack(true)
    }
    private fun stopCameraAndReturnToPreviousApp() {
        stopCamera() // Stop and unbind the camera lifecycle
    //    returnToPreviousApp() // Go back to the previous app
        minimizeApp()
       // finishAffinity()
    }
    private val captureSemaphore = Semaphore(1)
    private fun takePhoto() {
     /*   if (!captureSemaphore.tryAcquire()) {
            Log.d("TakePhoto", "Capture in progress, skipping this request")
            return
        }*/
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        //Companion.SharedResources.resourceMutex.withLock {
   //     logExecution("Image Capture") {
        logMemoryUsage()
            imageCapture.takePicture(
                outputOptions, ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                    //    captureSemaphore.release()
                        Log.d("Debug", "Error capturing")

                        Toast.makeText(baseContext, "Error capturing image", Toast.LENGTH_SHORT)
                            .show()
                    }

                    @SuppressLint("SuspiciousIndentation")
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {

                        Log.d("Debug", "Image captured successfully")

                            val savedUri = outputFileResults.savedUri ?: Uri.fromFile(photoFile)
                            //  showImageView(savedUri)
                         //   showImageView2(savedUri)

                        stopCameraAndReturnToPreviousApp()


                                //   CoroutineScope(Dispatchers.Main).launch {



                          /*  toggleCameraPreview(false)
                            uploadButton.visibility = View.VISIBLE
                            captureButton.visibility = View.GONE
                            retakePicture.visibility = View.VISIBLE
                            retakePicture.setOnClickListener {
                                toggleCameraPreview(true)
                                uploadButton.visibility = View.GONE
                                retakePicture.visibility = View.GONE

                                captureButton.visibility = View.VISIBLE
                            }

                            if (retakeBool) {


                            }*/
                            uploadButton.setOnClickListener {

                            stopCameraAndReturnToPreviousApp()
                            }
                   //     }
                        triggerSaveImageWorker(photoFile,metadata,"back")
                        onUploadSuccess()

                         /*
                                                    backCameraScope.launch {

                                                        if (lastKnownLocation == null) {
                                                            requestNewLocationData()
                                                            Log.d("Location", "Waiting for Location")
                                                            return@launch // Optionally, you might want to return here or disable photo capture until location is confirmed.
                                                        }
                                                        // Upload the captured image to Firebase
                                                        lastKnownLocation?.let {
                                                            val data =
                                                                "Location at time of photo: Latitude = ${it.latitude}, Longitude = ${it.longitude}\n"
                                                            /**after getting the location, fetch address of it and the nearby places that are relevant aka OSM Data**/
                                                            fetchAddressFromLocation(
                                                                it, photoFile,
                                                                onAddressFetched = { address ->
                                                                    // Save the address to the image or use it elsewhere
                                                                    metadata = mutableMapOf<String, String>().apply {
                                                                        put("address", "$address") // Example of dynamic address
                                                                    }

                                                                    addMetadataToImage(photoFile, metadata)
                                                                    debugMetadata(photoFile)
                                                                    Log.d("FetchedAddress", "Address: $address")
                                                                    //**TODO: add the address to metadata of saved imaged to temp **/
                                                                },
                                                            )
                                                            fetchOSMData(it.latitude, it.longitude, data, photoFile)

                                                        }
                                                        /**  CoroutineScope(Dispatchers.IO).launch {
                                                        processAndSaveImage(photoFile)
                                                        }**/
                                                        /** --> Attempt to process and save the image on another thread, still makes it slow**/
                                                        saveLocationToPreferences(applicationContext, lastKnownLocation!!)
                                                        // }
                                                        uploadButton.setOnClickListener {
                                                            //  finish()
                                                            //  finishMainCameraActivity()
                                                            stopCameraAndReturnToPreviousApp()


                                                            /**start Screencapservice**/
                                                         /*   Permissions.getMyServiceIntent()?.let {
                                                                startService(it)
                                                            }*/
                                                            /*    if(checkImageSaved(applicationContext)){
                                                            Permissions.getMyServiceIntent()?.let {
                                                                startService(it)
                                                            }
                                                        }*/


                                                            val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath) // Load the captured image as Bitmap

                                                            // Save to temp folder
                                                            val tempFile = saveImageToTempFolder(applicationContext, bitmap, metadata)
                                                            if (tempFile != null) {
                                                                Log.d(
                                                                    "BackCameraButton",
                                                                    "Image saved to temp folder: ${tempFile.absolutePath}"
                                                                )
                                                            } else {
                                                                Log.e("BackCameraButton", "Failed to save image to temp folder")
                                                            }

                                                            /**to test if metadata gets saved at all **/
                                                            /*   val testMetadata = mapOf("testKey" to "testValue")
                                                    addMetadataToImage(photoFile, testMetadata)

                                                    val metadataAfterSave = extractExifMetadata(photoFile)
                                                    Log.d("TestMetadata", "Metadata after adding test data: $metadataAfterSave")
                            */
                                                            /*edited*/
                                                         //  onUploadSuccess()


                                                           /* if(checkImageSaved(applicationContext)) {
                                                                Permissions.getMyServiceIntent()?.let {
                                                                    startService(it)
                                                                }
                                                            }*/

                                                        }
                                                    //    onUploadSuccess()

                                                    }*/
                        //  uploadImageToFirebase(photoFile,lastKnownLocation)
                    } //onsave

                }

            )
  //      saveImageToTempFolder(applicationContext,)
       // }


    }
   fun handleafterupload(photoFile: File) {
       CoroutineScope(Dispatchers.Main).launch {

           //  showImageView(savedUri)

           toggleCameraPreview(false)
           uploadButton.visibility = View.VISIBLE
           captureButton.visibility = View.GONE
           retakePicture.visibility = View.VISIBLE
           retakePicture.setOnClickListener {
               toggleCameraPreview(true)
               uploadButton.visibility = View.GONE
               retakePicture.visibility = View.GONE

               captureButton.visibility = View.VISIBLE
           }

           if (retakeBool) {


           }
       }

backCameraScope.launch {

           if (lastKnownLocation == null) {
               requestNewLocationData()
               Log.d("Location", "Waiting for Location")
               return@launch // Optionally, you might want to return here or disable photo capture until location is confirmed.
           }
           // Upload the captured image to Firebase
           lastKnownLocation?.let {
               val data =
                   "Location at time of photo: Latitude = ${it.latitude}, Longitude = ${it.longitude}\n"
               /**after getting the location, fetch address of it and the nearby places that are relevant aka OSM Data**/
               fetchAddressFromLocation(
                   it, photoFile,
                   onAddressFetched = { address ->
                       // Save the address to the image or use it elsewhere
                       metadata = mutableMapOf<String, String>().apply {
                           put("address", "$address") // Example of dynamic address
                       }

                       addMetadataToImage(photoFile, metadata)
                       debugMetadata(photoFile)
                       Log.d("FetchedAddress", "Address: $address")
                       //**TODO: add the address to metadata of saved imaged to temp **/
                   },
               )
               fetchOSMData(it.latitude, it.longitude, data, photoFile)

           }
           /**  CoroutineScope(Dispatchers.IO).launch {
           processAndSaveImage(photoFile)
           }**/
           /** --> Attempt to process and save the image on another thread, still makes it slow**/
           saveLocationToPreferences(applicationContext, lastKnownLocation!!)
           // }
           uploadButton.setOnClickListener {
               //  finish()
               //  finishMainCameraActivity()
               stopCameraAndReturnToPreviousApp()


               /**start Screencapservice**/
               /*   Permissions.getMyServiceIntent()?.let {
                      startService(it)
                  }*/
               /*    if(checkImageSaved(applicationContext)){
               Permissions.getMyServiceIntent()?.let {
                   startService(it)
               }
           }*/

/*
               val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath) // Load the captured image as Bitmap

               // Save to temp folder
               val tempFile = saveImageToTempFolder(applicationContext, bitmap, metadata)
               if (tempFile != null) {
                   Log.d(
                       "BackCameraButton",
                       "Image saved to temp folder: ${tempFile.absolutePath}"
                   )
               } else {
                   Log.e("BackCameraButton", "Failed to save image to temp folder")
               }*/

               /**to test if metadata gets saved at all **/
               /*   val testMetadata = mapOf("testKey" to "testValue")
       addMetadataToImage(photoFile, testMetadata)

       val metadataAfterSave = extractExifMetadata(photoFile)
       Log.d("TestMetadata", "Metadata after adding test data: $metadataAfterSave")
*/
               /*edited*/
               //  onUploadSuccess()


               /* if(checkImageSaved(applicationContext)) {
                    Permissions.getMyServiceIntent()?.let {
                        startService(it)
                    }
                }*/

           }
    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
    val tempFile = saveImageToTempFolder(applicationContext, bitmap, metadata)
    if (tempFile != null) {
        Log.d(
            "BackCameraButton", ""
       //     "Image saved to temp folder: ${tempFile.absolutePath}"
        )
    } else {
        Log.e("BackCameraButton", "Failed to save image to temp folder")
    }
           //    onUploadSuccess()

       }
   }
    private fun saveLocationToPreferences(context: Context, location: Location) {
        val sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("latitude", location.latitude.toString())
            putString("longitude", location.longitude.toString())
            apply()
        }
    }

    /**adding all the processing of the image and saving in one equation in order to call it in an anashnchronous thread so that the capturing of images is faster*/
  /*  private suspend fun processAndSaveImage(file: File) {
        val compressedBitmap = resizeAndCompressImage(file, 640, 480, 75) // Resize to 640x480 and compress at 75% quality

        withContext(Dispatchers.IO) {
            FileOutputStream(file).use { out ->
                compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 75, out)
            }
            compressedBitmap.recycle()
            Log.d("ImageProcessing", "Image saved and compressed at ${file.absolutePath}")
        }
    }*/




  /*  private fun resizeAndCompressImage(file: File, targetWidth: Int, targetHeight: Int, quality: Int): Bitmap {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true // Load only metadata initially
        }
        BitmapFactory.decodeFile(file.absolutePath, options)

        options.inSampleSize = calculateInSampleSize(options, targetWidth, targetHeight)
        options.inJustDecodeBounds = false // Now load the actual bitmap

        val originalBitmap = BitmapFactory.decodeFile(file.absolutePath, options)
        val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)

        originalBitmap.recycle() // Free memory

        val outputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        resizedBitmap.recycle() // Free memory

        val compressedBitmap = BitmapFactory.decodeByteArray(outputStream.toByteArray(), 0, outputStream.size())
        outputStream.close()

        return compressedBitmap
    }*/

/**that for rotating the image in the right direction when capturing the image**/
/**TODO: make the image appear exactly as  captured**/
    fun getExifRotation(imagePath: String): Int {
        return try {
            val exif = ExifInterface(imagePath)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }


/**showing the image for the user right after capturing, without resizing or anything**/
    private fun showImageView(fileUri: Uri) {
        val imageView = findViewById<ImageView>(R.id.imageView)
     //   val resizedBitmap = resizeBitmapForDisplay(File(fileUri.path))  // Assuming fileUri points to the image file
     //   val compressedBitmap = compressBitmapForDisplay(resizedBitmap)
      //  imageView.setImageBitmap(compressedBitmap)

      //  val imageFile = File(fileUri.path ?: return)

     //   val resizedBitmap = resizeAndCompressImage(imageFile, 640, 480, 50) // Resize for display with lower quality
        // Display the resized bitmap in the ImageView
     //   imageView.setImageBitmap(resizedBitmap)
        val imagePath = File(fileUri.path ?: "").absolutePath
        var bitmap: Bitmap? = null
        try {
            bitmap = BitmapFactory.decodeFile(File(fileUri.path ?: "").absolutePath)
            val rotationAngle = getExifRotation(imagePath)
            val rotatedBitmap = rotateBitmap(bitmap, rotationAngle)
           // val scaledBitmap = Bitmap.createScaledBitmap(rotatedBitmap, imageView.width, imageView.height, true)
           // imageView.setImageBitmap(scaledBitmap)
            imageView.setImageBitmap(rotatedBitmap)
            uploadButton.setOnClickListener {
                //  finish()
                //  finishMainCameraActivity()
                stopCameraAndReturnToPreviousApp()
            }
            saveImageToTempFolder(applicationContext,bitmap,metadata)

        } finally {
           // bitmap?.recycle() // Recycle after use
            /**--> dont use it because it cannot be recycled yet as it will be used later for upload.**/
        }


}
    private fun showImageView2(fileUri: Uri) {
        val imageView = findViewById<ImageView>(R.id.imageView)
        val bitmap = BitmapFactory.decodeFile(File(fileUri.path ?: "").absolutePath)
        val rotationAngle = getExifRotation(fileUri.path ?: "")
        val rotatedBitmap = rotateBitmap(bitmap, rotationAngle)
        imageView.setImageBitmap(rotatedBitmap)
    }


    /**attempt to resize and compress before showing the image to user, however can take long and slows down the app**/
    /*private fun compressBitmapForDisplay(bitmap: Bitmap): Bitmap {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)  // Compress to reduce size
        val byteArray = outputStream.toByteArray()
        bitmap.recycle()
        // Return the compressed bitmap
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    private fun resizeBitmapForDisplay(imageFile: File): Bitmap {
        // Load the image file into a Bitmap with resizing
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(imageFile.absolutePath, options)

        // Calculate inSampleSize to scale the image down
        options.inSampleSize = calculateInSampleSize(options, 640, 480)  // Adjust target size as needed
        options.inJustDecodeBounds = false

        return BitmapFactory.decodeFile(imageFile.absolutePath, options)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }*/

    /**toggling the different camera view**/
    /**TODO: check here if the image show can be altered through here/problem lies here**/
    private fun toggleCameraPreview(showPreview: Boolean) {
        val previewView = findViewById<PreviewView>(R.id.previewView)
        val imageView = findViewById<ImageView>(R.id.imageView)

        if (showPreview) {
            previewView.visibility = View.VISIBLE
            imageView.visibility = View.GONE
        } else {
            previewView.visibility = View.GONE
            imageView.visibility = View.VISIBLE
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun requestNewLocationData() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 0
            fastestInterval = 0
            numUpdates = 1
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                lastKnownLocation = locationResult.lastLocation
                Log.d("Location", "New location obtained: ${lastKnownLocation?.latitude}, ${lastKnownLocation?.longitude}")
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
        }
    }


/**saving the location surrrounding data to an extra file*/
    /**TODO: save to the image itself not as an extra file*/
    private fun saveDataToFile(data: String) {
        try {
            val filename = "Data.txt"
            val fileOutputStream: FileOutputStream = openFileOutput(filename, Context.MODE_PRIVATE)
            fileOutputStream.write(data.toByteArray())
            fileOutputStream.close()
          /*  runOnUiThread{
                Toast.makeText(this, "Data saved to $filename", Toast.LENGTH_LONG).show()
            }*/
            Log.d("OSM","Data saved to $filename")
        } catch (e: IOException) {
            e.printStackTrace()
            runOnUiThread {            Toast.makeText(this, "Failed to save data", Toast.LENGTH_LONG).show() }
            Log.e("OSM","Failed to save data")

        }
    }

  /*  private fun addMetadataToImage(imageFile: File, metadata: Map<String, String>) {
        Log.d("Metadata", "New Entry : $metadata")
        try {
            val exif = ExifInterface(imageFile.absolutePath)
            metadata.forEach { (key, value) ->
                when (key) {
                    "sessionCounter" -> exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, "$key: $value")
                    "participantId" -> exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, "$key: $value") // Use TAG_MAKE for custom info
                    "latitude", "longitude" -> exif.setAttribute(ExifInterface.TAG_GPS_AREA_INFORMATION, "$key: $value")
                    "address" -> exif.setAttribute(ExifInterface.TAG_USER_COMMENT, value) // Example for address
                    "OSM" -> exif.setAttribute(ExifInterface.TAG_USER_COMMENT, value)
                    else -> Log.d("ExifMetadata", "Unsupported metadata key: $key")
                }
            }
            exif.saveAttributes()
            Log.d("MetadataAdd", "Metadata added to image: ${imageFile.absolutePath}")
        } catch (e: IOException) {
            Log.e("MetadataAdd", "Error adding metadata to image", e)
        }
    }*/
  private fun debugMetadata(imageFile: File) {
      val metadata = extractExifMetadata(imageFile)
      Log.d("MetadataDebug", "Current Metadata: $metadata")
  }

    private fun addMetadataToImage(imageFile: File, newMetadata: Map<String, String>) {
        try {
            val exif = ExifInterface(imageFile.absolutePath)

            // Retrieve and merge existing metadata
            val existingMetadataJson = exif.getAttribute(ExifInterface.TAG_USER_COMMENT)
            val existingMetadata = if (!existingMetadataJson.isNullOrEmpty()) {
                JSONObject(existingMetadataJson).toMap()
            } else {
                emptyMap()
            }
            val mergedMetadata = existingMetadata.toMutableMap().apply { putAll(newMetadata) }

            // Save merged metadata
            val jsonMetadata = JSONObject(mergedMetadata as Map<*, *>?).toString()
            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, jsonMetadata)
            exif.saveAttributes()

            Log.d("MetadataAdd", "Merged Metadata saved: $jsonMetadata")
        } catch (e: IOException) {
            Log.e("MetadataAdd", "Failed to add metadata to image", e)
        }
    }


    // Helper to convert JSONObject to Map
    private fun JSONObject.toMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        keys().forEach { key ->
            map[key] = getString(key)
        }
        return map
    }



    /**fetches nearby locations that could be meaningful in order to  know where the user is appprox or what they are doing **/
    private fun fetchOSMData(latitude: Double, longitude: Double, locationData: String, imageFile: File) {
        val smallBoxSize = 0.0001  // Represents roughly +/-10 meters depending on the location
        val minLat = latitude - smallBoxSize
        val maxLat = latitude + smallBoxSize
        val minLon = longitude - smallBoxSize
        val maxLon = longitude + smallBoxSize

        val query = """
             [out:json];
             (
                node["amenity"](bbox:$minLat,$minLon,$maxLat,$maxLon);
                way["amenity"](bbox:$minLat,$minLon,$maxLat,$maxLon);
                relation["amenity"](bbox:$minLat,$minLon,$maxLat,$maxLon);
               
             );
             out body;
             >;
             out skel qt;
          """.trimIndent()

        val encodedQuery = Uri.encode(query)
        val url = "https://overpass-api.de/api/interpreter?data=$encodedQuery"

        OkHttpClient().newCall(Request.Builder().url(url).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("HTTP", "Failed to fetch OSM data", e)
                // Save location data even if OSM data fetch fails
                metadata = mutableMapOf<String, String>().apply {
                    put("locationData", locationData.toString())
                }

                addMetadataToImage(imageFile, metadata)
                debugMetadata(imageFile)
              //  saveDataToFile(locationData)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        Log.e("HTTP", "Failed to fetch OSM data: ${response.message} URL was: $url")
                    /**TODO: no need for external saving of files, should all be in metadata**/
                       // saveDataToFile(locationData)
                        metadata = mutableMapOf<String, String>().apply {
                            put("locationData", locationData.toString())
                        }

                        addMetadataToImage(imageFile, metadata)
                        debugMetadata(imageFile)
                        return
                    }
                    val responseData = it.body?.string() ?: ""
                    val completeData = locationData + "OSM Data: $responseData"
                    Log.d("OSM", "$locationData")
                    Log.d("Metadata", "in OsmDara ; Address:$metadata")
                    findNearestAmenity(responseData, latitude, longitude,imageFile)
                }
            }
        })
    }
    private fun findNearestAmenity(data: String, myLat: Double, myLon: Double, imageFile: File) {
        val json = JSONObject(data)
        val elements = json.getJSONArray("elements")
        var nearestNode: JSONObject? = null
        var minDistance = Double.MAX_VALUE

        for (i in 0 until elements.length()) {
            val node = elements.getJSONObject(i)
            if (node.has("lat") && node.has("lon")) {
                val lat = node.getDouble("lat")
                val lon = node.getDouble("lon")
                val distance = calculateDistance(myLat, myLon, lat, lon)
               // val distance = sqrt((lat - myLat).pow(2) + (lon - myLon).pow(2))
                if (distance < minDistance) {
                    minDistance = distance
                    nearestNode = node
                }
            }
        }
        metadata = mutableMapOf<String, String>()
        nearestNode?.let {
            val amenity = if (it.has("amenity")) it.getString("amenity") else "No amenity available"
            val highway = if (it.has("highway")) it.getString("highway") else "No highway available"
            val nearestAmenityInfo = "Nearest Amenity: $amenity, Location: ${it.getDouble("lat")}, ${it.getDouble("lon")}"
        //    saveDataToFile(nearestAmenityInfo)
          //  saveDataToFile( "$nearestNode , $highway")

            uploadOSMToPicture(nearestNode.toString(), highway)
            (metadata as MutableMap<String, String>)["nearestAmenity"] = amenity
            (metadata as MutableMap<String, String>)["nearestHighway"] = highway
            (metadata as MutableMap<String, String>)["nearestNode"] = nearestNode.toString()
            (metadata as MutableMap<String, String>)["nearestNodeCoordinates"] = "Lat: ${it.getDouble("lat")}, Lon: ${it.getDouble("lon")}"

            val metadata = mapOf(
                "nearestAmenity" to amenity.toString(),
                "nearestHighway" to highway.toString(),
                "nearestlat" to  it.getDouble("lat").toString(),
                "nearestlon" to it.getDouble("lon").toString()
            )

            addMetadataToImage(imageFile, metadata)
            debugMetadata(imageFile)
            Log.d("Metadata", "in findnearestamnisty ; $metadata")
        } ?: run {
            Log.d("DEBUG", "No nearest amenity found or no nodes with valid coordinates")
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0  // Radius of the earth in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c * 1000  // Distance in meters
    }

    private fun fetchAddressFromLocation(location: Location, imageFile: File, onAddressFetched: (String?) -> Unit) {
        val latitude = location.latitude
        val longitude = location.longitude
        val url = "https://nominatim.openstreetmap.org/reverse?format=json&lat=$latitude&lon=$longitude"

        // OkHttpClient and Request creation
        val request = Request.Builder().url(url).build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("HTTP", "Failed to fetch address data", e)
                // Even if the location fetch fails, proceed to upload image
            /*    uploadButton.setOnClickListener{
                 //       finish()
                    finishMainCameraActivity()
                    //finishAffinity() // Closes all activities in the stack
                    //System.exit(0)   // Exits the application

                    // uploadImageToFirebase(imageFile, location, null,"BACK")
                    val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath) // Assuming `imageFile` exists
                    saveImageToTempFolder(applicationContext, bitmap,metadata)
                    setReturningFromCameraFlag(true)
                    Log.d("DEBUG", "Upload success")

                    onUploadSuccess()
                   // uploadImageToFirebaseRules(location,imageFile)

                }*/
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    val responseData = response.body?.string()

                        val jsonObject = JSONObject(responseData)
                        val address = jsonObject.getJSONObject("address").getString("road")
                        onAddressFetched(address)
                        // Proceed to upload the image with the address
                       /**actually dont need that anymore because it should get uploaded automatically without waiting for anynthing now**/
                    /**TODO: remove this and try it again**/

                     /*   uploadButton.setOnClickListener {
                            //     finish()
                            finishMainCameraActivity()
                            val bitmap =
                                BitmapFactory.decodeFile(imageFile.absolutePath) // Assuming `imageFile` exists
                            saveImageToTempFolder(applicationContext, bitmap)

                            //   uploadImageToFirebaseRules(location,imageFile)
                            //  uploadImageToFirebase(imageFile, location, null)

                        }*/

                }
            }
        })

    }
/**has been moved to companion object**/
    /*public fun uploadImageToFirebaseRules(location: Location?, imageFile: File){
        Log.d("UploadFromTemp", "uploadToFirebaseRules Back")

//        capturedImagePath = imageFile.absolutePath
        if (isMobileDataConnected()) {

             //   queueFileForUpload(imageFile.absolutePath, lastKnownLocation, "BACK")
                Toast.makeText(
                    this,
                    "Currently on mobile data. Image has been queued to save data.",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
   //     uploadPendingFiles()
      //  uploadImageToFirebase(imageFile, location, null)

        else if (isWifiConnected()) {


                  //  uploadPendingFiles()
                   uploadImageToFirebase(imageFile, location, null, "BACK")

            }
    }*/

fun resizeBitmap(imageFile: File): Bitmap? {
    val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
    if (bitmap == null) {
        Log.e("BitmapError", "Failed to decode bitmap from file: ${imageFile.absolutePath}")
        return null
    }

    val newWidth = 640
    val newHeight = (bitmap.height * (newWidth.toFloat() / bitmap.width)).toInt()
    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}

   /* private fun resizeBitmap(imageFile: File): Bitmap {
        // Load the image file into a Bitmap
        val options = BitmapFactory.Options()
        options.inSampleSize = 2 // Adjust this value as needed to shrink the image
        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath, options)

        // Determine the new dimensions for the resized bitmap
        val width = bitmap.width
        val height = bitmap.height
        val newWidth = 640 // Set the desired width
        val newHeight = (height * (newWidth.toFloat() / width)).toInt()

        // Resize the bitmap
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
*/
    private fun compressBitmap(bitmap: Bitmap): ByteArray {
        // Compress the bitmap into a ByteArray
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream) // Adjust quality (0-100) as needed
        return outputStream.toByteArray()
    }
    private fun correctImageOrientation(imageUri: Uri, imageFile: File) {
        val exifInterface = ExifInterface(imageFile.absolutePath)
        val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val rotationDegrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

        if (rotationDegrees != 0) {
            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
        //    cameraExecutor.execute {
                val rotatedBitmap = rotateBitmap(bitmap, rotationDegrees)
                saveRotatedBitmapToFile(rotatedBitmap, imageFile)
          //  }


        }
    }
    private fun rotateBitmap(source: Bitmap, angle: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle.toFloat())
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun saveRotatedBitmapToFile(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out) // Adjust quality as needed
        }
    }

   /* private fun compressAndPrepareImageData(imageFile: File): ByteArray {
        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream) // Compress quality can be adjusted
        return outputStream.toByteArray()
    }

    private fun queueFileForUpload(filePath: String, location: Location?,cameraSource: String) {
        val locationData = if (location != null) "${location.latitude},${location.longitude}" else "unknown,unknown"
        val fileData = JSONObject().apply {
            put("path", filePath)
            put("location", locationData) // Store as a single string
        }

        val sharedPref = getSharedPreferences("UploadQueue", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        val existingQueue = sharedPref.getStringSet("queuedFiles", mutableSetOf())!!
        existingQueue.add(fileData.toString())
        editor.putStringSet("queuedFiles", existingQueue)
        editor.apply()
        onUploadSuccess()
    }



    private fun uploadPendingFiles() {
        val sharedPref = getSharedPreferences("UploadQueue", Context.MODE_PRIVATE)
        val queuedFiles = sharedPref.getStringSet("queuedFiles", mutableSetOf())
        queuedFiles?.let { files ->
            for (fileData in files) {
                try {
                    val jsonData = JSONObject(fileData)
                    val filePath = jsonData.getString("path")
                    val locationData = jsonData.optString("location", "unknown,unknown") // Use optString for safer retrieval
                    val parts = locationData.split(",")

                    val location = Location(LocationManager.GPS_PROVIDER).apply {
                        latitude = parts[0].toDoubleOrNull() ?: 0.0 // Provide default values
                        longitude = parts[1].toDoubleOrNull() ?: 0.0
                    }

                    val file = File(filePath)
                    uploadImageToFirebase(file, location, "Optional address or null", "BACK")
                } catch (e: JSONException) {
                    Log.e("Upload", "Failed to parse queued file data", e)
                } catch (e: Exception) {
                    Log.e("Upload", "Error processing queued file", e)
                }
            }
            sharedPref.edit().remove("queuedFiles").apply()  // Clear the queue after upload attempts
        }
    }*/

    private fun uploadOSMToPicture(nearestNode: String?, highway: String?){


            osmData = mapOf(
                "nearestNode" to nearestNode,
                "highway" to highway
            )
            Log.d("OSMData", "Saved OSM data: Nearest Node = $nearestNode, Highway = $highway")


    }
    /**upload old**/
    @SuppressLint("SuspiciousIndentation")
  /*  fun uploadImageToFirebase(imageFile: File, location: Location?, address: String?, cameraSource:String) {

        cameraExecutor.execute {
            val folderName = if (cameraSource == "BACK") "backcamera" else "frontcamera"
            Log.e("Upload", "In uploadImageTofirebase.")
            val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
            val sessionCounter = sessionManager.getSessionCounter()

           val participantId = prefs.getString("PID", "")
         //   val connection
            val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            val currentDay = "Day_" + AccessService.getCurrentDay(this)
            val sessionFolder = "session_$sessionCounter"
            if (!imageFile.exists()) {
                Log.e("Upload", "Image file does not exist.")
                return@execute
            }
//new added compression
          //  val resizedBitmap = resizeAndCompressImage(imageFile, 640, 480, 75) // Resize to 640x480 with 75% quality
          //  val imageData = ByteArrayOutputStream().apply {
         //       resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 75, this)
         //   }.toByteArray()
            correctImageOrientation(Uri.fromFile(imageFile), imageFile)

            val lat = location?.latitude ?: "Unknown"
            val lon = location?.longitude ?: "Unknown"


       //     fetchOSMData(lat, location?.longitude) { nearestNode, highway ->
                val imagesRef =
                    storageRef.child("$participantId/$currentDay/$sessionFolder/back_s${sessionCounter}_p${participantId}_${imageFile.name}.jpg")

                val metadata = StorageMetadata.Builder()
                    .setCustomMetadata("latitude", lat.toString())
                    .setCustomMetadata("longitude", lon.toString())
                    .setCustomMetadata("address", address ?: "Unknown")
                    .build()
            //old compression
                val resizedBitmap = resizeBitmap(imageFile)

                // Compress the resized bitmap
                val imageData = resizedBitmap?.let { compressBitmap(it) }


                val uploadTask = imageData?.let { imagesRef.putBytes(it, metadata) }

                /*  imagesRef.metadata.addOnSuccessListener { metadata ->
            // Extract custom metadata fields
            val latitude = metadata.getCustomMetadata("latitude")
            val longitude = metadata.getCustomMetadata("longitude")
            Log.d("Metadata", "Location: Latitude $latitude, Longitude $longitude")
        }.addOnFailureListener { exception ->
            Log.e("Metadata", "Error retrieving metadata", exception)
        }*/
                //    if(isWifiConnected(this)){
                uploadTask!!.addOnSuccessListener { taskSnapshot ->
                    backcamerapicTaken = true
                    imagesRef.metadata.addOnSuccessListener { metadata ->
                        // Extract custom metadata fields
                      //  backcamerapicTaken = true
                        val latitude = metadata.getCustomMetadata("latitude")
                        val longitude = metadata.getCustomMetadata("longitude")
                        Log.d("Metadata", "Location: Latitude $latitude, Longitude $longitude")
                        SharedPreferencesUtils.clearCapturedImagePath(applicationContext)
                    }.addOnFailureListener { exception ->
                        Log.e("Metadata", "Error retrieving metadata", exception)
                    }
                    Toast.makeText(
                        baseContext,
                        "Image uploaded to Firebase with location metadata",
                        Toast.LENGTH_SHORT
                    ).show()

                    fun onBackPressed() {

                        val resultIntent = Intent()
                        resultIntent.putExtra("callingPackageName", callingPackageName)

                        // Set the result and finish the activity
                        setResult(Activity.RESULT_OK, resultIntent)

                    }

                //    saveImageMetadataToDatabase(imageFile.name, lat, lon, address)
                    setReturningFromCameraFlag(true)
                    Log.d("DEBUG", "Upload success")

                    onUploadSuccess()


                }.addOnFailureListener { exception ->
                    backcamerapicTaken = false

                    Toast.makeText(
                        baseContext,
                        "Failed to upload image: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                  //  queueFileForUpload(imageFile.absolutePath, Location("").apply {
                       // this.latitude = latitude
                       // this.longitude = longitude
                 //   }, "BACK")
               //     saveImageToTempFolder(imageFile,resizedBitmap)
                }
                //    }
                //    }
                /*  }else {
            // Not connected to WiFi, handle accordingly (e.g., queue the upload or notify the user)
            Log.d("Wifi", " Wifi not connected")
            Toast.makeText(this, "Connect to WiFi in order to proceed please.", Toast.LENGTH_LONG).show()
            if (!isWifiConnected(this)) {
                queueFileForUpload(imageFile.absolutePath,location)
                return
            }*/
                //   }

                /*
            saveImageMetadataToDatabase(imageFile.name, lat, lon, address)

            Log.d("DEBUG", "Upload success")
            Log.d("BackCamera", "Before updating should be false: ${lastReturnedValue}")
            setReturningFromCameraFlag(true)
            Log.d("BackCamera", "After updating should be true: ${lastReturnedValue}")

            //finish()
            onUploadSuccess()


        }.addOnFailureListener { exception ->
            // Handle failure
            Toast.makeText(
                baseContext,
                "Failed to upload image: ${exception.message}",
                Toast.LENGTH_SHORT
            ).show()
        }*/

        }
    }*/
    private fun setReturningFromCameraFlag(value: Boolean) {
        val sharedPref = getSharedPreferences("CameraSwitchPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("returningFromCamera", value)
            apply()
        }
        Log.d("BackCamera","$value")

        lastReturnedValue=value
    }

    /*
    private fun saveImageMetadataToDatabase(imageName: String, latitude: Any, longitude: Any, address: Any?) {

        val nearestNode = osmData["nearestNode"] ?: "Unknown"
        val highway = osmData["highway"] ?: "Unknown"
        val sanitizedImageName = imageName.replace(".", "_")
        val dbRef = FirebaseDatabase.getInstance().getReference("images")

       // val dbRef= FirebaseFirestore.getInstance()
        val imageMetadata = hashMapOf(
            "imageName" to imageName,
            "latitude" to latitude,
            "longitude" to longitude,
            "address" to (address ?: "Unknown"),
            "nearestPlace" to nearestNode,
            "nearestHighway" to highway

        )
        dbRef.child(sanitizedImageName).setValue(imageMetadata).addOnSuccessListener {
            Log.d("FirebaseDB", "Metadata saved successfully!")
        }.addOnFailureListener {
            Log.e("FirebaseDB", "Failed to save metadata", it)
        }
    }*/


    fun hasCapturePermission(): Boolean {
        val prefs = getSharedPreferences("ScreenCapturePrefs", MODE_PRIVATE)
        return prefs.getBoolean("scsGranted", false)
    }



    @SuppressLint("SuspiciousIndentation")
    private fun onUploadSuccess() {

      //  returnToApp(callingPackageName!!)
        val intent = Intent(this, HideCamera::class.java)

        intent.putExtra(SKIP_LAST_STAGE_UPDATE, true)
        startService(intent)

        Log.d("UploadSuccess", "hide camera started")
      //  sendBroadcast(Intent("com.example.cameraswitch.UPLOAD_SUCCESS"))
        Log.d("UploadSuccess", "calling receiver")

      //  uploadSuccessReceiver


     //   finishMainCameraActivity()

        /**uncomment this if you want to upload frontcamera pictures right away"
         *
         */
   //val frontCameraIntent= Intent("com.example.cameraswitch.UPLOAD_TRIGGER").apply {
     //       putExtra("cameraSource", "FRONT")
      //  }
       // sendBroadcast(frontCameraIntent)

/**edited**/
            // Camera usage and user has returned to app
      /*      Permissions.getMyServiceIntent()?.let {
                startService(it)
            }*/

       /* if(checkImageSaved(applicationContext)){
            Permissions.getMyServiceIntent()?.let {
                startService(it)
            }
        }*/
    }
    private fun finishMainCameraActivity() {
        sendBroadcast(Intent("com.example.ACTION_CLOSE_MAIN"))
        finish()
    }

    private val uploadSuccessReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            if (intent == null || context == null) {
                Log.e("BackCameraExit", "Received null intent or context")
                return
            } else if (callingPackageName.isNullOrEmpty()) {
                Log.e("BackCameraExit", "Package name is missing")
                return
            }

            if (isFinishing) {
                return  // Avoid doing work if the activity is finishing
            }

          //  returnToApp(callingPackageName!!)
            Log.d("BackCameraExit", "returning to app $callingPackageName")


            // Start the HideCamera service for front camera capture
            /*   val serviceIntent = Intent(this@BackCameraButton, HideCamera::class.java)
               serviceIntent.putExtra("camera", CameraSelector.LENS_FACING_FRONT)
               startService(serviceIntent)*/
        }

    }
    private fun returnToApp(packageName: String) {
/*

       val launchIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setPackage(packageName)
           Log.d("BackCameraExit", " Setting this app: $packageName")
        }
        val resolveInfo = packageManager.queryIntentActivities(launchIntent, 0)
        if (resolveInfo.isNotEmpty()) {
            val activityInfo = resolveInfo[1].activityInfo
            val componentName = ComponentName(activityInfo.applicationInfo.packageName, activityInfo.name)
            Intent().apply {
                component = componentName
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(this)
                Log.d("BackCameraExit", "started the returned app")
            }
        }else{
            Log.d("BackCameraExit", " resolveInfo Empty")
        }
        */

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(launchIntent)
        Log.d("BackCameraExit", "started the returned app")






    }
    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return (mediaDir ?: externalMediaDirs) as File
    }
    private fun requestPermissions() {
      /*  val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS_CODE)
        } else {
            startCamera()
        }*/
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            REQUIRED_PERMISSIONS_TIRAMISU
        } else {
            REQUIRED_PERMISSIONS_BELOW_TIRAMISU
        }
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, requiredPermissions, REQUEST_PERMISSIONS_CODE)
        }
    }
   /* override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            if (allPermissionsGranted()) {

                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }*/
    /* private fun allPermissionsGranted(): Boolean {
         return permissions.all {
             ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
         }
     }*/
/*
   override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
       Log.d("ScreenCaptureService", "in ActivityResult")
        if (requestCode == REQUEST_SCREEN_CAPTURE) {
            if (resultCode == RESULT_OK) {
              /*  val serviceIntent = Intent(this, ScreenCaptureService::class.java)
                serviceIntent.putExtra("resultCode", resultCode)
                serviceIntent.putExtra("data", data)
                startService(serviceIntent)*/
                Log.d("ScreenCaptueService", " in onActivityRes")
              /*   mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                 mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data!!)
                val intent = Intent(this, ScreenCaptureService::class.java)
                bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
                isBound = true*/
                val sharedPreferences = getSharedPreferences("ScreenCapturePrefs", MODE_PRIVATE)
                val resultCode = sharedPreferences.getInt("resultCode", Activity.RESULT_CANCELED)
                val resultDataString: String?
                resultDataString = sharedPreferences.getString("resultData", null)
                //val resultData = data?.extras
                val captureIntent = Intent(this, ScreenCaptureService::class.java)
                captureIntent.putExtra("resultCode", resultCode)
                captureIntent.putExtra("resultData", resultDataString)
                startService(captureIntent)
                Log.d("ScreenCaptureService", "onActivityRes BC: isBound: $isBound, Code: $resultCode, data: $data")
            } else {
                Log.d("ScreenCaptureService", "Permission Denied by User")
            }
        }
    }*/
    /*
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ScreenCaptureService.LocalBinder
            screenCaptureService = binder.getService()
            if (isBound) {
                screenCaptureService.setMediaProjection(mediaProjection)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }*/
   private fun logExecution(operationName: String, block: () -> Unit) {
       val startTime = System.nanoTime()
       block()
       val endTime = System.nanoTime()
       Log.d("Performance", "$operationName took ${(endTime - startTime) / 1_000_000} ms")
   }
    private fun logMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        Log.d("MemoryUsage", "Used memory: ${usedMemory}MB")
    }
    private fun checkImageSaved(context: Context): Boolean {
        val sharedPref = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPref.getBoolean(Constants.IMAGE_SAVED_KEY, false)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(uploadSuccessReceiver, IntentFilter("com.example.cameraswitch.UPLOAD_SUCCESS"),Context.RECEIVER_NOT_EXPORTED)
        if (allPermissionsGranted() && isLocationEnabled()) {
            getLastLocation()
        }
       val intentFilter = IntentFilter("com.example.cameraswitch.UPLOAD_TRIGGER")
       //registerReceiver(uploadTriggerReceiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(uploadSuccessReceiver)
    }
    override fun onDestroy() {
        super.onDestroy()
        if (::cameraExecutor.isInitialized && !cameraExecutor.isShutdown) {
            cameraExecutor.shutdown()
        }
        cameraExecutor.shutdown()
        cameraProvider?.unbindAll()
        setReturningFromCameraFlag(true)
        checkTriggerRunnable?.let { handler.removeCallbacks(it) }
        activityContext = null
    }

    companion object {
        private lateinit var sessionManager: SessionManager

        object SharedResources {
            val resourceMutex = Mutex()
        }

     /*   fun triggerUpload(context: Context) {
         /*   val pathKey = when (sourceType) {
                "camera" -> Constants.BACK_CAMERA_PATH_KEY
                "screenshots" -> Constants.SCREENSHOT_PATH_KEY
                else -> return
            }*/
            /**TODO: do the broadcast for front camera here**/
            val networkReceiver = NetworkChangeReceiver()
            val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            context.registerReceiver(networkReceiver, intentFilter)

            // sendBroadcast(frontCameraIntent)
            Log.d("UploadFromTemp", "Backcamera upload trigger received in companion")
//            sessionManager.incrementSessionCounter()


            val location = SharedPreferencesUtils.getSavedLocation(context)
            val sharedPref = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
             val imagePathSet = sharedPref.getStringSet(Constants.CAPTURED_IMAGE_PATH_KEY, mutableSetOf())!!
         //   val imagePathSet = sharedPref.getStringSet(pathKey, mutableSetOf()) ?: mutableSetOf()

            for (imagePath in imagePathSet) {
                val imageFile = File(imagePath)
                if (imageFile.exists()) {
                    Log.d("UploadFromTemp", "Triggering upload for image at $imagePath")

                    uploadImageToFirebaseRules(context,location, imageFile)
                 //   Log.d("UploadFromTemp", "Triggering uploadAllSessions")

                  //  uploadAllSessions(context)

                } else {
                    Log.e("BackCameraButton", "Image file does not exist: $imagePath")
                }
            }
            sharedPref.edit().remove(Constants.CAPTURED_IMAGE_PATH_KEY).apply()
        }*/
/**works to upload every file in last session**/
   /*     fun triggerUpload(context: Context) {
            Log.d("UploadFromTemp", "Backcamera upload trigger received in companion")
            sessionManager = SessionManager(context)
            val location = SharedPreferencesUtils.getSavedLocation(context)
            val sessionCounter = sessionManager.getSessionCounter()
            val tempDir = File(context.filesDir, "temp_images/session_$sessionCounter")

            if (!tempDir.exists() || !tempDir.isDirectory) {
                Log.e("UploadFromTemp", "No temp directory found")
                return
            }

            tempDir.listFiles()?.forEach { imageFile ->
                if (imageFile.isFile) {
                    Log.d("UploadFromTemp", "Triggering upload for image at ${imageFile.absolutePath}")
                    uploadImageToFirebaseRules(context,location, imageFile)
                } else {
                    Log.e("UploadFromTemp", "Image file does not exist: ${imageFile.absolutePath}")
                }
            }
        }*/
fun triggerUpload(context: Context) {
    Log.d("UploadFromTemp", "Backcamera upload trigger received in companion")
    val parentDir = File(context.filesDir, "temp_images")
    val location = SharedPreferencesUtils.getSavedLocation(context)
    if (!parentDir.exists() || !parentDir.isDirectory) {
        Log.e("UploadFromTemp", "No temp_images directory found")
        return
    }

    // Go through all session folders
    parentDir.listFiles()?.forEach { sessionDir ->
        if (sessionDir.isDirectory && sessionDir.name.startsWith("session_")) {
            Log.d("UploadFromTemp", "Processing session directory: \${sessionDir.name}")

            sessionDir.listFiles()?.forEach { imageFile ->
                if (imageFile.isFile) {
                    if(NetworkChangeReceiver.wifiafterdata){
                        Log.d("UploadFromTemp", "NetworkReceiver says wifi is connected after mobiledata")

                        uploadImageToFirebase(
                            context,imageFile,sessionDir,location
                        )
                    }else{
                        Log.d("UploadFromTemp", "Found image: \${imageFile.name}")
                        uploadImageToFirebaseRules(context,location, sessionDir, imageFile)
                    }

                } else {
                    Log.e("UploadFromTemp", "Not a file: \${imageFile.name}")
                }
            }
        } else {
            Log.e("UploadFromTemp", "Not a session directory: \${sessionDir.name}")
        }
    }
}
         @SuppressLint("SuspiciousIndentation")
         fun uploadImageToFirebaseRules(context: Context,location: Location?, sessionDir: File,imageFile: File){
             Log.d("UploadFromTemp", "uploadToFirebaseRules Back companion")

//        capturedImagePath = imageFile.absolutePath
             if (isMobileDataConnected(context)) {

                 //   queueFileForUpload(imageFile.absolutePath, lastKnownLocation, "BACK")
                 Log.d("UploadFromTemp", "Mobile Data Connected do nothin Back companion")

                 val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                 if (bitmap != null) {
                     savetotemptillWifi(context, bitmap)
                 } else {
                     Log.e("UploadMobileData", "Failed to decode photoFile into Bitmap.")
                 }
                 return
             }
             //     uploadPendingFiles()
             //  uploadImageToFirebase(imageFile, location, null)

             else if (isWifiConnected(context)) {


                 //  uploadPendingFiles()
                 uploadImageToFirebase(context, imageFile, sessionDir, location)

                 Log.d("UploadFromTemp", "Wifi Data Connected, uploaded to firebase Back companion")

             }
         }

        private fun savetotemptillWifi(context: Context, bitmap: Bitmap): File?{
            sessionManager.incrementSessionCounter()
            Log.d("UploadFromTemp", "saveImageToTemp till wifi Back")
          //  Log.d("UploadFromTemp", "Metadata : $metadata")
         //   Log.d("Metadata", "in UploadtoTemp ; $metadata")

            return try {

                val sessionCounter = sessionManager.getSessionCounter()
                val dateFormat = SimpleDateFormat("ddMMyyy_HHmmss", Locale.getDefault())
                val timeFormat = SimpleDateFormat("HHmmss", Locale.getDefault())
                val currentTime = System.currentTimeMillis()
                val formattedDate = dateFormat.format(Date(currentTime))
                val formattedTime = timeFormat.format(Date(currentTime))
                val participantId = SharedPreferencesUtils.getParticipantId(context)
                // val tempFile = File(tempDir, "back_s${sessionCounter}_p$participantId")
                //  val tempDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "temp_images/session_$sessionCounter/camera")
                val tempDir = File(context.filesDir, "temp_images/session_$sessionCounter")

                if (!tempDir.exists() && !tempDir.mkdirs()) {
                    Log.e("FileError", "Failed to create temp directory")
                    return null
                }
                val tempFile=File(tempDir, "back_s_${sessionCounter}_p${participantId}_${formattedDate}_${formattedTime}")
                Log.d("UploadFromTemp", "Temp directory path: ${tempDir.absolutePath}")
                FileOutputStream(tempFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                    //  val resized= resizeBitmap(tempFile)
                    //   resized?.let { Companion.compressBitmap(it) }
                }

                val metadata = mutableMapOf<String, String>().apply {
                    put("sessionCounter", sessionManager.getSessionCounter().toString())
                    put("participantId", SharedPreferencesUtils.getParticipantId(context).orEmpty())
                }
/**add metdata**/
            //    addMetadataToImage(tempFile, metadata)
            //    debugMetadata(tempFile)

                // Save the path in SharedPreferences
                //   val sharedPref = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                // sharedPref.edit().putString(Constants.CAPTURED_IMAGE_PATH_KEY, tempFile.absolutePath).apply()
                //  saveCapturedImagePath(context,tempFile.absolutePath,"camera")
                // saveCapturedImagePath(context,tempFile.absolutePath)

                tempFile
            } catch (e: IOException) {
                Log.e("FileError", "Error saving image to temp folder", e)
                null
            }
        }
        private fun extractExifMetadata(imageFile: File): Map<String, String> {
            val metadata = mutableMapOf<String, String>()

            try {
              //  val fullPath = imageFile.absolutePath
             //   val sessionPart = fullPath.substringAfter("session_").substringBefore("/")
                val exif = ExifInterface(imageFile.absolutePath)

                // Parse JSON metadata from the user comment field
                var jsonMetadata = exif.getAttribute(ExifInterface.TAG_USER_COMMENT)
                if (!jsonMetadata.isNullOrEmpty()) {
                    val jsonObject = JSONObject(jsonMetadata)
                    jsonObject.keys().forEach { key ->
                        metadata[key] = jsonObject.getString(key)
                    }
                }
                jsonMetadata = JSONObject(metadata as Map<*, *>?).toString()
                exif.setAttribute(ExifInterface.TAG_USER_COMMENT, jsonMetadata)
                exif.saveAttributes()

                val savedMetadata = exif.getAttribute(ExifInterface.TAG_USER_COMMENT)
                Log.d("MetadataExtract", "Saved Metadata in TAG_USER_COMMENT: $savedMetadata")

                Log.d("MetadataExtract", "Extracted metadata: $metadata")
            } catch (e: Exception) {
                Log.e("MetadataExtract", "Failed to extract metadata from image", e)
            }

            return metadata
        }


        fun resizeBitmap(imageFile: File): Bitmap? {
            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            if (bitmap == null) {
                Log.e("BitmapError", "Failed to decode bitmap from file: ${imageFile.absolutePath}")
                return null
            }

            val newWidth = 640
            val newHeight = (bitmap.height * (newWidth.toFloat() / bitmap.width)).toInt()
            return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        }
       /* private fun resizeBitmap(imageFile: File): Bitmap {
            // Load the image file into a Bitmap
            val options = BitmapFactory.Options()
            options.inSampleSize = 2 // Adjust this value as needed to shrink the image
            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath, options)

            // Determine the new dimensions for the resized bitmap
            val width = bitmap.width
            val height = bitmap.height
            val newWidth = 640 // Set the desired width
            val newHeight = (height * (newWidth.toFloat() / width)).toInt()

            // Resize the bitmap
            return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        }*/

        private fun compressBitmap(bitmap: Bitmap): ByteArray {
            // Compress the bitmap into a ByteArray
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream) // Adjust quality (0-100) as needed
            return outputStream.toByteArray()
        }

     /*   private fun uploadAllSessions(context: Context) {
            Log.d("UploadFromTemp", "inside uploadAllSessions")

         //   val tempDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "temp_images/session_$sessionCounter/camera")
        //    val tempDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "temp_images")
            val tempDir = File(context.filesDir, "temp_images")

            if (!tempDir.exists() || !tempDir.isDirectory) {
                Log.e("UploadFromTemp", "No temp directory found")
                return
            }
            tempDir.listFiles()?.forEach { sessionDir ->
                if (sessionDir.isDirectory) {
                    Log.d("UploadFromTemp", "Processing session directory: ${sessionDir.name}")

                    sessionDir.listFiles()?.forEach { imageFile ->
                        if (imageFile.isFile) {
                            Log.d("UploadFromTemp", "Found image: ${imageFile.name}")
                            uploadImageToFirebaseRules(context, SharedPreferencesUtils.getSavedLocation(context), imageFile)
                        } else {
                            Log.e("UploadFromTemp", "Not a file: ${imageFile.name}")
                        }
                    }
                } else {
                    Log.e("UploadFromTemp", "Not a directory: ${sessionDir.name}")
                }
            }

        }*/
        fun deleteFileAfterUpload(file: File) {
            if (file.exists() && file.delete()) {
                Log.d("FileCleanup", "Deleted file: ${file.absolutePath}")
            } else {
                Log.e("FileCleanup", "Failed to delete file: ${file.absolutePath}")
            }
        }
        private fun logExecution(operationName: String, block: () -> Unit) {
            val startTime = System.nanoTime()
            block()
            val endTime = System.nanoTime()
            Log.d("Performance", "$operationName took ${(endTime - startTime) / 1_000_000} ms")
        }
        private fun logMemoryUsage() {
            val runtime = Runtime.getRuntime()
            val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
            Log.d("MemoryUsage", "Used memory: ${usedMemory}MB")
        }
        fun uploadImageToFirebase(context: Context, imageFile: File,sessionDir: File, location: Location?) {
            val storage = FirebaseStorage.getInstance()
            val storageRef = storage.reference
         //   logExecution("Firebase Upload") {
           // logMemoryUsage()
                val participantId =
                    SharedPreferencesUtils.getParticipantId(context) ?: "unknown_participant"
                val currentDay = "Day_" + AccessService.getCurrentDay(context)
                //  val sessionManager = SessionManager(context)
                //val sessionCounter = sessionManager.getSessionCounter()
                val sessionFolder = sessionDir.name
                val fileName = imageFile.name
                val imagesRef =
                    storageRef.child("$participantId/$currentDay/$sessionFolder/$fileName")

                val metadataBuilder = StorageMetadata.Builder()
                location?.let {
                    metadataBuilder.setCustomMetadata("latitude", it.latitude.toString())
                    metadataBuilder.setCustomMetadata("longitude", it.longitude.toString())
                }
                val metadata = metadataBuilder.build()

                val resizedBitmap = resizeBitmap(imageFile)
                val imageData = resizedBitmap?.let { compressBitmap(it) }

                //   if (imageData != null) {
                if (imageData != null) {
                    //(imagesRef.putFile(imageFile.toUri(), metadata)
                    //    imagesRef.putFile(Uri.fromFile(imageFile), metadata)
                    imagesRef.putBytes(imageData, metadata)
                        .addOnSuccessListener {
                            Log.d(
                                "UploadSuccess",
                                "Image uploaded successfully: ${imageFile.absolutePath}"
                            )
                            deleteFileAfterUpload(imageFile)
                            //  SharedPreferencesUtils.clearCapturedImagePath(context)

                        }.addOnFailureListener { exception ->
                            Log.e("UploadError", "Failed to upload image: ${exception.message}")
                        }
                } else {
                    Log.d("UploadError", "no imagefile")
                }
                //  } else {
                //  Log.e("BitmapError", "Failed to compress bitmap for upload: ${imageFile.name}")
                //  }
          //  }
        }


        /*      public fun uploadImageToFirebase(context: Context, imageFile: File, location: Location?) {
                  Log.d("UploadFromTemp", "in upload to firebase Back companion")
                  val dateFormat = SimpleDateFormat("ddMMyyy_HHmmss", Locale.getDefault())
                  val timeFormat = SimpleDateFormat("HHmmss", Locale.getDefault())
                  val currentTime = System.currentTimeMillis()
                  val formattedDate = dateFormat.format(Date(currentTime))
                  val formattedTime = timeFormat.format(Date(currentTime))
      /**TODO: add exif data again**/
                //  val exifMetadata = extractExifMetadata(imageFile)

           //       Log.d("MetadataCheck", "Extracted Metadata Before Upload: $exifMetadata")

                  val metadataBuilder = StorageMetadata.Builder()
                  /*   .setCustomMetadata("uploadTimestamp", formattedTime)*/

                /*  exifMetadata.forEach { (key, value) ->
                      StorageMetadata.Builder().setCustomMetadata(key, value)
                  }*/

               //   Log.d("MetadataCheck", "exifmetadata before metadataBuilder.build : $exifMetadata")


                  var  sessionManager = SessionManager(context)

                  val storage = FirebaseStorage.getInstance()
                  val storageRef = storage.reference


                   //   imageFile.listFiles()?.forEach { sessionDir ->
                     //     if (sessionDir.isDirectory) {
                       //       sessionDir.walkTopDown().filter { it.isFile }.forEach { file ->
                                  val fullPath = imageFile.absolutePath
                                  //  val sessionPart = fullPath.substringAfter("session_").substringBefore("/")
                                  val sessionPart = fullPath.substringAfter("temp_images/")
                               //   val sessionPart = file.relativeTo(imageFile).path
                                  val participantId =
                                      SharedPreferencesUtils.getParticipantId(context)
                                          ?: "unknown_participant"
                                  val currentDay = "Day_" + AccessService.getCurrentDay(context)
                                  val sessionCounter = sessionManager.getSessionCounter()
                                  //   val connection
                                  val sessionFolder = "session_$sessionCounter"


                                  val imagesRef =
                                      storageRef.child("$participantId/$currentDay/${sessionPart}")

                                  /**TODO:check if the metada saved before is in the file */

                                  // Create metadata with location


                                  /* location?.let {
                      metadataBuilder.setCustomMetadata("latitude", it.latitude.toString())
                      metadataBuilder.setCustomMetadata("longitude", it.longitude.toString())
                  }*/
                                  val resizedBitmap = resizeBitmap(imageFile)

                                  // Compress the resized bitmap
                                  val imageData = resizedBitmap?.let { compressBitmap(it) }
                                 // val imageData= imageFile
                                  val metadata = metadataBuilder.build()
                                  Log.d("MetadataCheck", "after metadataBuilder.build : $metadata")
                                   // val imageData = imageFile.readBytes()

                                  imageData?.let {
                                      listOf(imagesRef).forEach { ref ->
                                          ref.putBytes(it, metadata)
                                       //   val fileUri = Uri.fromFile(imageFile)

                                        //  ref.putFile(fileUri,metadata)
                                        //  val stream = FileInputStream(imageFile)
                                        //  ref.putStream(stream, metadata)
                                               //imagesRef.putBytes(it, metadata)
                                              .addOnSuccessListener {
                                                  Log.d(
                                                      "BackCameraButton",
                                                      "Image uploaded successfully: ${imageFile.absolutePath}"
                                                  )
                                                  Log.d(
                                                      "UploadFromTemp",
                                                      "Image uploaded successfully: ${imageFile.absolutePath}"
                                                  )
                                                  backcamerapicTaken = true
                                                  //  Toast.makeText(context, "Image uploaded successfully", Toast.LENGTH_SHORT).show()
                                                  SharedPreferencesUtils.clearCapturedImagePath(context) // Clear path after upload
                                                  deleteFileAfterUpload(imageFile)
                                              }.addOnFailureListener { exception ->
                                                  backcamerapicTaken = false

                                                  Log.e(
                                                      "BackCameraButton",
                                                      "Failed to upload image: ${exception.message}"
                                                  )
                                                  Log.e(
                                                      "UploadFromTemp",
                                                      "Failed to upload image: ${exception.message}"
                                                  )

                                                  //  Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show()
                                                  Log.e(
                                                      "BackCameraButton",
                                                      "Failed to upload image: ${exception.message}"
                                                  )

                                              }
                                      }
                                  //}
                                  /**TODO: add listener for metada like in the original function */
                                  /*   if (uploadTask != null) {
                      uploadTask.addOnSuccessListener {
                          Log.d("BackCameraButton", "Image uploaded successfully: ${imageFile.absolutePath}")
                          Log.d("UploadFromTemp", "Image uploaded successfully: ${imageFile.absolutePath}")
                          backcamerapicTaken = true
                          //  Toast.makeText(context, "Image uploaded successfully", Toast.LENGTH_SHORT).show()
                          SharedPreferencesUtils.clearCapturedImagePath(context) // Clear path after upload
                          deleteFileAfterUpload(imageFile)
                      }.addOnFailureListener { exception ->
                          backcamerapicTaken = false

                          Log.e("BackCameraButton", "Failed to upload image: ${exception.message}")
                          Log.e("UploadFromTemp", "Failed to upload image: ${exception.message}")

                          //  Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show()
                          Log.e("BackCameraButton", "Failed to upload image: ${exception.message}")

                      }
                  }*/
                            //  }
                         // }
                  }
                  fun onBackPressed() {

                      val resultIntent = Intent()
                      resultIntent.putExtra("callingPackageName", getCallingPackageName())

                      // Set the result and finish the activity
                      setActivityResult(RESULT_OK,resultIntent)
                  //   setResult(Activity.RESULT_OK, resultIntent)

                  }
              }*/

  /*  public fun uploadImagesToFirebase(context: Context, imageFile: File, location: Location?) {
        Log.d("UploadFromTemp", "Processing file or directory: ${imageFile.absolutePath}")

        // If it's a directory, upload all files inside it
        if (imageFile.isDirectory) {
            Log.d("DirectoryFound", "Found directory: ${imageFile.absolutePath}, uploading contents...")
            imageFile.listFiles()?.forEach { file ->
                if (file.isFile) {
                    uploadImageToFirebase(context, file, location)
                } else {
                    Log.e("FileError", "Skipping invalid path (not a file): ${file.absolutePath}")
                }
            }
            return
        }

        // Validate the file before uploading
        if (!imageFile.isFile) {
            Log.e("FileError", "Invalid file: ${imageFile.absolutePath} (not a file)")
            return
        }

        // Decode and validate bitmap
        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
        if (bitmap == null) {
            Log.e("BitmapError", "Failed to decode bitmap from file: ${imageFile.absolutePath}")
            return
        }

        // Resize and compress the image
        val resizedBitmap = Bitmap.createScaledBitmap(
            bitmap,
            640,
            (bitmap.height * (640.0 / bitmap.width)).toInt(),
            true
        )
        val imageData = ByteArrayOutputStream().apply {
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 75, this)
        }.toByteArray()

        if (imageData.isEmpty()) {
            Log.e("BitmapError", "Failed to compress bitmap for upload: ${imageFile.absolutePath}")
            return
        }

        // Prepare Firebase reference
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference

        val participantId = SharedPreferencesUtils.getParticipantId(context) ?: "unknown_participant"
        val currentDay = "Day_" + AccessService.getCurrentDay(context)
        val sessionFolder = imageFile.parentFile?.name ?: "unknown_session"
      /*  var  sessionManager = SessionManager(context)

        val sessionCounter = sessionManager.getSessionCounter()

        val sessionFolder = "session_$sessionCounter"*/

        val sessionPart = imageFile.absolutePath.substringAfter("temp_images/")
        val imagesRef = storageRef.child("$participantId/$currentDay/${sessionPart}")

        //val imagesRef = storageRef.child("$participantId/$currentDay/${imageFile.name}")

        // Add metadata if location exists
        val metadata = StorageMetadata.Builder().apply {
            location?.let {
                setCustomMetadata("latitude", it.latitude.toString())
                setCustomMetadata("longitude", it.longitude.toString())
            }
        }.build()

        // Upload the file
        imagesRef.putBytes(imageData, metadata)
            .addOnSuccessListener {
                Log.d("UploadSuccess", "Image uploaded successfully: ${imageFile.absolutePath}")
                deleteFileAfterUpload(imageFile)
            }
            .addOnFailureListener { exception ->
                Log.e("UploadError", "Failed to upload image: ${exception.message}")
            }
    }*/



        fun isMobileDataConnected(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            return networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        }
        fun isWifiConnected(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            return networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }

        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CAMERA_PERMISSION = 100
        private const val REQUEST_PERMISSIONS_CODE = 200
        private val REQUIRED_PERMISSIONS_BELOW_TIRAMISU = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        private val REQUIRED_PERMISSIONS_TIRAMISU = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )

        private const val REQUEST_SCREEN_CAPTURE=104
    /*    fun getStartIntent(context: Context, resultCode: Int, data: Intent): Intent {
            val intent = Intent(context, ScreenCaptureService::class.java)
            intent.putExtra("resultCode", resultCode)
            intent.putExtra("resultData", data)
            return intent
        }*/
    var backcamerapicTaken = false
        private var callingPackageName: String? = null
        fun setCallingPackageName(packageName: String) {
            callingPackageName = packageName
        }

        fun getCallingPackageName(): String? {
            return callingPackageName
        }

        private var activityContext: BackCameraButton? = null
        val uniqueId = UUID.randomUUID().toString()

        fun setActivityResult(resultCode: Int, data: Intent?) {
            activityContext?.setResult(resultCode, data)
        }
        private const val KEY_SCREEN_CAPTURE_DATA_URI = "screen_capture_data_uri"
    }
}

