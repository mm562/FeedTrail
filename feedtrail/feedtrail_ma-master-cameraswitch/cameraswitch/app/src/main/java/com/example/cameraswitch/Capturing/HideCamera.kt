
package com.example.cameraswitch.Capturing

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.cameraswitch.Capturing.BackCameraButton.Companion.uploadImageToFirebaseRules
import com.example.cameraswitch.PagesandActivites.Permissions
import com.example.cameraswitch.Service.AccessService
import com.example.cameraswitch.R
import com.example.cameraswitch.Receivers.NetworkChangeReceiver
import com.example.cameraswitch.Workers.SessionManager
import com.example.cameraswitch.UtilsAndCons.Constants
import com.example.cameraswitch.UtilsAndCons.SharedPreferencesUtils
import com.example.cameraswitch.Workers.SavingCamToTemp
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class HideCamera : LifecycleService() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference
    private lateinit var imageCapture: ImageCapture
    private var currentCamera = CameraSelector.DEFAULT_FRONT_CAMERA
    private lateinit var photoFile: File
    private var cameraProvider: ProcessCameraProvider? = null
    private var pictureTaken = false
    private lateinit var sessionManager: SessionManager
    private var sessionCounter: Int = 0
    //   private lateinit var context:Context
    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        sessionManager = SessionManager(applicationContext)
        sessionCounter = sessionManager.getSessionCounter()
        cameraExecutor = Executors.newSingleThreadExecutor()
        Log.d("HideCamera", "starting HideCamera")
        Log.d("DebugCameraTrigger", "in HideCamera")


        /*    val networkReceiver = NetworkChangeReceiver()
            val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)

            registerReceiver(networkReceiver, intentFilter,Context.RECEIVER_NOT_EXPORTED)*/

        // Initialize Firebase Storage
        storage = FirebaseStorage.getInstance()
        storageRef = storage.reference

        // Initialize image capture
        imageCapture = ImageCapture.Builder().build()

        // Start image capture from both front and back cameras

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get() // Initialize cameraProvider here

            // Start image capture from both front and back cameras
            CoroutineScope(Dispatchers.Main).launch {
                //  captureImageFromCamera(currentCamera)
                bindImageCaptureUseCase(applicationContext,currentCamera)
            }
        }, ContextCompat.getMainExecutor(this))
        //  startScreenshots()
    }




    fun startScreenshots(){
        // Log.d("HideCamera", "Starting Screenshots")


    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("HideCamera","onStartComnnd, current PictureTaken: $pictureTaken")

        if (intent?.action == RESET_PICTURE_ACTION) {
            Log.d("HideCamera","reset current PictureTaken: $pictureTaken")
            pictureTaken = false
            Log.d("HideCamera","after reset, current PictureTaken: $pictureTaken")

        }

        return super.onStartCommand(intent, flags, startId)
    }
    private fun startForegroundService() {
        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel("camera_service", "Camera Service")
            } else {
                ""
            }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("FeedTrail")
            .setContentText("Collecting Data. Thank you for participating in our study.")
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    resources,
                    R.drawable.ic_notification
                )
            )
            .build()

        startForeground(1, notification)
    }

    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
        return channelId
    }

    private fun bindImageCaptureUseCase(context: Context,cameraSelector: CameraSelector) {
        val rotation = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = rotation.defaultDisplay
        val targetRotation = when (display.rotation) {
            Surface.ROTATION_0 -> Surface.ROTATION_0
            Surface.ROTATION_90 -> Surface.ROTATION_90
            Surface.ROTATION_180 -> Surface.ROTATION_180
            Surface.ROTATION_270 -> Surface.ROTATION_270
            else -> Surface.ROTATION_0
        }

        imageCapture = ImageCapture.Builder().setTargetRotation(targetRotation).build()

        try {
            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(this, cameraSelector, imageCapture)
        } catch (e: Exception) {
            Log.e("HideCamera", "Error binding image capture use case", e)
        }

        // Start capturing images

        //  use captureImageFromCamera for capturing both front and back hidden
        //    captureImageFromCamera(cameraSelector)
        captureImageFromFrontCamera(context)
    }
    private fun captureImageFromFrontCamera(context: Context) {
        if (!pictureTaken) {
            val photoFile = File(
                externalMediaDirs.firstOrNull(),
                SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
            )
            // Wait for the camera provider to be available
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this@HideCamera)
            val cameraProvider = cameraProviderFuture.get()

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    // Bind the image capture use case to the camera
                    cameraProvider.bindToLifecycle(this@HideCamera, CameraSelector.DEFAULT_FRONT_CAMERA, imageCapture)

                    // Configure output options for capturing the image
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                    // Capture the image on a background thread
                    imageCapture.takePicture(
                        outputOptions,
                        cameraExecutor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onError(exc: ImageCaptureException) {
                                Log.e("ImageCaptureService", "Error capturing image", exc)
                            }

                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                // Image captured and saved, now switch to the other camera
                                CoroutineScope(Dispatchers.Main).launch {
                                    switchCamera()

                                    //  uploadImageToFirebaseRules(photoFile, cameraSource = "FRONT")
                                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                                    if (bitmap != null) {
                                        //saveImageToTempFolder(context, bitmap)
                                        triggerSaveImageWorker(photoFile,"front")
                                    } else {
                                        Log.e("SaveImage", "Failed to decode photoFile into Bitmap.")
                                    }
                                    pictureTaken = true
                                }
                            }

                        })
                    Permissions.getMyServiceIntent()?.let {
                        startService(it)
                    }
                } catch (exc: Exception) {
                    Log.e("HideCamera", "Error binding image capture use case in captureim", exc)
                }
            }; ContextCompat.getMainExecutor(this)
        } else {
            stopSelf()
        }
    }
    private fun triggerSaveImageWorker(photoFile: File,typepic:String) {
        val sessionManager= SessionManager(applicationContext)
        sessionManager.incrementSessionCounter()
        //  val metadataJson = JSONObject(metadata).toString()

        val inputData = Data.Builder()
            .putString("image_path", photoFile.absolutePath)
            //    .putString("metadata", metadataJson)
            .putString("type", typepic)
            .build()

        val saveImageWorkRequest = OneTimeWorkRequestBuilder<SavingCamToTemp>()
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(saveImageWorkRequest)
    }
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        cameraProvider?.unbindAll()

    }



    /*fun onBind(intent: Intent?): IBinder? {
        if (intent != null) {
            super.onBind(intent)
        }
        return null
    }*/
    /* private fun isWifiConnected(): Boolean {
         val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
         val activeNetwork = connectivityManager.activeNetwork
         val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
         return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
     }
     private fun isMobileDataConnected(): Boolean {
         val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
         val activeNetwork = connectivityManager.activeNetworkInfo
         return activeNetwork != null && activeNetwork.isConnected && activeNetwork.type == ConnectivityManager.TYPE_MOBILE
     }*/
    private fun queueFileForUpload(filePath: String, cameraSource: String) {
        //  val locationData = if (location != null) "${location.latitude},${location.longitude}" else "unknown,unknown"
        val fileData = JSONObject().apply {
            put("path", filePath)
            put("cameraSource", cameraSource)
            // put("location", locationData) // Store as a single string
        }

        val sharedPref = getSharedPreferences("UploadQueue", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        val existingQueue = sharedPref.getStringSet("queuedFiles", mutableSetOf())!!
        existingQueue.add(fileData.toString())
        editor.putStringSet("queuedFiles", existingQueue)
        editor.apply()

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
                    val cameraSource = jsonData.getString("cameraSource")
                    val location = Location(LocationManager.GPS_PROVIDER).apply {
                        latitude = parts[0].toDoubleOrNull() ?: 0.0 // Provide default values
                        longitude = parts[1].toDoubleOrNull() ?: 0.0
                    }

                    val file = File(filePath)
                    uploadImageToFirebase(file,cameraSource)
                } catch (e: JSONException) {
                    Log.e("Upload", "Failed to parse queued file data", e)
                } catch (e: Exception) {
                    Log.e("Upload", "Error processing queued file", e)
                }
            }
            sharedPref.edit().remove("queuedFiles").apply()  // Clear the queue after upload attempts
        }
    }
    private fun captureImageFromCamera(context: Context,cameraSelector: CameraSelector) {
        if (!pictureTaken) {
            val photoFile = File(
                externalMediaDirs.firstOrNull(),
                SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
            )

            // Wait for the camera provider to be available
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this@HideCamera)
            val cameraProvider = cameraProviderFuture.get()

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    // Bind the image capture use case to the camera
                    cameraProvider.bindToLifecycle(this@HideCamera, cameraSelector, imageCapture)

                    // Configure output options for capturing the image
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                    // Capture the image on a background thread

                    imageCapture.takePicture(
                        outputOptions,
                        cameraExecutor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onError(exc: ImageCaptureException) {
                                Log.e("HideCamera", "Error capturing image", exc)
                            }

                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                // Image captured and saved, now switch to the other camera

                                CoroutineScope(Dispatchers.Main).launch {

                                    switchCamera()
                                    //    uploadImageToFirebase(photoFile)
                                    //    uploadImageToFirebaseRules(photoFile, cameraSource = "FRONT")
                                    //     saveImageToTempFolder(context,photoFile)
                                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                                    if (bitmap != null) {
                                        saveImageToTempFolder(context, bitmap)
                                    } else {
                                        Log.e("SaveImage", "Failed to decode photoFile into Bitmap.")
                                    }
                                    if (currentCamera == CameraSelector.DEFAULT_FRONT_CAMERA){
                                        pictureTaken=true
                                    }
                                    /* when (currentCamera) {
                                        CameraSelector.DEFAULT_FRONT_CAMERA -> frontCameraTaken = true
                                        CameraSelector.DEFAULT_BACK_CAMERA -> backCameraTaken = true
                                    }

                                    if (!frontCameraTaken && !backCameraTaken) {
                                        switchCamera()
                                    } else {
                                        stopForeground(true)
                                        stopSelf()
                                    }

                                    // Upload the image to Firebase Storage
                                    uploadImageToFirebase(photoFile)*/
                                }
                            }
                        })
                } catch (exc: Exception) {
                    Log.e("HideCamera", "Error binding image capture use case", exc)
                }
            }; ContextCompat.getMainExecutor(this)
        }else{
            stopSelf()
        }
    }

    private fun saveImageToTempFolder(context: Context,bitmap: Bitmap): File? {
        //  sessionManager.incrementSessionCounter()
        Log.d("UploadFromTemp", "saveImageToTemp Front")
        return try {

            val dateFormat = SimpleDateFormat("ddMMyyy_HHmmss", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HHmmss", Locale.getDefault())
            val currentTime = System.currentTimeMillis()
            val formattedDate = dateFormat.format(Date(currentTime))
            val formattedTime = timeFormat.format(Date(currentTime))
            val sessionCounter = sessionManager.getSessionCounter()

            val participantId = SharedPreferencesUtils.getParticipantId(context)
            // val tempDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "temp_images/session_$sessionCounter/camera")
            val tempDir = File(context.filesDir, "temp_images/session_$sessionCounter")

            if (!tempDir.exists() && !tempDir.mkdirs()) {
                Log.e("FileError", "Failed to create temp directory")
                return null
            }
            val tempFile = File(tempDir, "front_s_${sessionCounter}_p${participantId}_${formattedDate}_${formattedTime}")
            FileOutputStream(tempFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                //    compressBitmap(bitmap)
            }

            // Save the path in SharedPreferences
            //  val sharedPref = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            //  sharedPref.edit().putString(Constants.CAPTURED_IMAGE_PATH_KEY, tempFile.absolutePath).apply()
            //    saveCapturedImagePath(context,tempFile.absolutePath,"camera")
            //  saveCapturedImagePath(context,tempFile.absolutePath)

            tempFile
        } catch (e: IOException) {
            Log.e("FileError", "Error saving image to temp folder", e)
            null
        }
    }

    private fun saveCapturedImagePath(context: Context, imagePath: String) {
        val sharedPref = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val imagePathSet = sharedPref.getStringSet(Constants.CAPTURED_IMAGE_PATH_KEY, mutableSetOf())!!
        imagePathSet.add(imagePath)
        sharedPref.edit().putStringSet(Constants.CAPTURED_IMAGE_PATH_KEY, imagePathSet).apply()
    }

    /* private fun saveCapturedImagePath(context: Context, imagePath: String, sourceType: String) {
         val sharedPref = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
         val pathKey = when (sourceType) {
             "camera" -> Constants.BACK_CAMERA_PATH_KEY
             "screenshots" -> Constants.SCREENSHOT_PATH_KEY
             else -> return
         }
         val imagePathSet = sharedPref.getStringSet(pathKey, mutableSetOf()) ?: mutableSetOf()
         imagePathSet.add(imagePath)
         sharedPref.edit().putStringSet(pathKey, imagePathSet).apply()
     }*/

    private fun saveCapturedImagePath(context: Context, imagePath: String, sourceType: String) {
        val file = File(imagePath)
        if (!file.isFile) {
            Log.e("PathError", "Attempted to save a directory path: $imagePath")
            return
        }
        val sharedPref = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val pathKey = when (sourceType) {
            "camera" -> Constants.BACK_CAMERA_PATH_KEY
            "screenshots" -> Constants.SCREENSHOT_PATH_KEY
            else -> return
        }
        val imagePathSet = sharedPref.getStringSet(pathKey, mutableSetOf()) ?: mutableSetOf()
        imagePathSet.add(imagePath)
        sharedPref.edit().putStringSet(pathKey, imagePathSet).apply()
    }

    /*  private fun uploadImageToFirebaseRules(imageFile: File,cameraSource: String){

        if (isMobileDataConnected()) {
            queueFileForUpload(imageFile.absolutePath, "FRONT")
            Toast.makeText(
                this,
                "Currently on mobile data. Image has been queued to save data.",
                Toast.LENGTH_LONG
            ).show()

        }
        //     uploadPendingFiles()
        //  uploadImageToFirebase(imageFile, location, null)

        else if (isWifiConnected()) {
            //  }else {
            //   uploadPendingFiles()
            //   if (isWifiConnected(this)) {
            //    uploadPendingFiles()
            uploadPendingFiles()
            uploadImageToFirebase(imageFile,cameraSource)
        }
    }*/
    private fun switchCamera() {
        currentCamera = if (currentCamera == CameraSelector.DEFAULT_FRONT_CAMERA) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }
        cameraProvider!!.unbindAll()
        // Ensure cameraProvider is not null before calling unbindAll()
        if (cameraProvider != null) {
            // cameraProvider!!.unbindAll()
        } else {
            Log.e("HideCamera", "cameraProvider is null when attempting to unbindAll() in switchCamera")
        }

        // Capture image from the newly selected camera
        CoroutineScope(Dispatchers.IO).launch {
            captureImageFromCamera(applicationContext,currentCamera)
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this@HideCamera)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            // Capture image from the newly selected camera
            CoroutineScope(Dispatchers.IO).launch {
                captureImageFromCamera(applicationContext,currentCamera)
            }
        }, ContextCompat.getMainExecutor(this@HideCamera))
    }

    private fun uploadImageToFirebase(imageFile: File,cameraSource: String) {
        val folderName = if (cameraSource == "FRONT") "frontcamera" else "backcamera"
        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        val currentDay = "Day_" + AccessService.getCurrentDay(this)
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        val participantId = prefs.getString("PID", "")
        val filename = imageFile.name
        val sessionFolder = "session_${sessionCounter}"
        val imagesRef = storageRef.child("$participantId/$currentDay/$sessionFolder/front_s${sessionCounter}_p$participantId${imageFile.name}.jpg")

        val resizedBitmap = resizeBitmap(imageFile)
        val imageData = compressBitmap(resizedBitmap)


        val uploadTask = imagesRef.putBytes(imageData)

        //val uploadTask = imagesRef.putFile(Uri.fromFile(imageFile))

        uploadTask.addOnSuccessListener {
            // Image uploaded successfully
            frontCamerapictureTaken =true
            Log.d("HideCamera", "Image uploaded to Firebase: $filename")
            Log.d("HideCamera", "Images from HideCamera uploaded")

        }.addOnFailureListener { exception ->
            // Handle failure
            frontCamerapictureTaken =false

            Log.e("HideCamera", "Failed to upload image", exception)
        }
    }
    private fun compressBitmap(bitmap: Bitmap): ByteArray {
        // Compress the bitmap into a ByteArray
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream) // Adjust quality (0-100) as needed
        return outputStream.toByteArray()
    }


    private fun resizeBitmap(imageFile: File): Bitmap {
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
    /*private fun resetPictureTaken() {
        pictureTaken = false
        startCameraServiceIfNeeded()
    }

    private fun startCameraServiceIfNeeded() {
        if (!pictureTaken) {
            val intent = Intent(this, HideCamera::class.java)
            ContextCompat.startForegroundService(this, intent)
        }
    }*/

    companion object {
        private lateinit var sessionManager: SessionManager
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
        private fun compressBitmap(bitmap: Bitmap): ByteArray {
            // Compress the bitmap into a ByteArray
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream) // Adjust quality (0-100) as needed
            return outputStream.toByteArray()
        }
        /**old frontcamera working**/
        /*
                fun triggerUpload(context: Context) {
                    //     sessionManager.incrementSessionCounter()

                    Log.d("UploadFromTemp", "Front Cam upload trigger received in companion")
                    /*   val pathKey = when (sourceType) {
                           "camera" -> Constants.BACK_CAMERA_PATH_KEY
                           "screenshots" -> Constants.SCREENSHOT_PATH_KEY
                           else -> return
                       }*/
                    /**TODO: do the broadcast for front camera here**/
                    val networkReceiver = NetworkChangeReceiver()
                    val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
                    context.registerReceiver(networkReceiver, intentFilter)

                    val location = SharedPreferencesUtils.getSavedLocation(context)
                    val sharedPref = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                    val imagePathSet = sharedPref.getStringSet(Constants.CAPTURED_IMAGE_PATH_KEY, mutableSetOf())!!
                    //  val imagePathSet = sharedPref.getStringSet(pathKey, mutableSetOf()) ?: mutableSetOf()

                    for (imagePath in imagePathSet) {

                        // if (imagePath != null) {
                        val imageFile = File(imagePath)
                        if (imageFile.exists()) {
                            Log.d("HideCam", "Triggering upload for image at $imagePath")
                            uploadImageToFirebaseRules(context, location, imageFile)


                        } else {
                            Log.e("HideCam", "Image file does not exist: $imagePath")
                        }
                        // }
                    }
                    sharedPref.edit().remove(Constants.CAPTURED_IMAGE_PATH_KEY).apply()

                }*/
        fun triggerUpload(context: Context) {
            Log.d("UploadFromTemp", "FrontCamera upload trigger received in companion")
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
                            Log.d("UploadFromTemp", "Found image: \${imageFile.name}")
                            uploadImageToFirebaseRules(context,location, sessionDir, imageFile)
                        } else {
                            Log.e("UploadFromTemp", "Not a file: \${imageFile.name}")
                        }
                    }
                } else {
                    Log.e("UploadFromTemp", "Not a session directory: \${sessionDir.name}")
                }
            }
        }
        fun uploadImageToFirebaseRules(context: Context,location: Location?,  sessionDir: File,imageFile: File){
            Log.d("UploadFromTemp", "uploadToFirebaseRules Back companion")

//        capturedImagePath = imageFile.absolutePath
            if (isMobileDataConnected(context)) {

                //   queueFileForUpload(imageFile.absolutePath, lastKnownLocation, "BACK")
                Log.d("UploadFromTemp", "Mobile Data Connected do nothin Back companion")
                val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                if (bitmap != null) {
                    savetotemptillWifi(context, bitmap)
                } else {
                    Log.e("SaveImage", "Failed to decode photoFile into Bitmap.")
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
        fun uploadImageToFirebase(context: Context, imageFile: File,sessionDir: File, location: Location?) {
            val storage = FirebaseStorage.getInstance()
            val storageRef = storage.reference

            val participantId = SharedPreferencesUtils.getParticipantId(context) ?: "unknown_participant"
            val currentDay = "Day_" + AccessService.getCurrentDay(context)
            //  val sessionManager = SessionManager(context)
            //val sessionCounter = sessionManager.getSessionCounter()
            val sessionFolder = sessionDir.name
            val fileName = imageFile.name
            val imagesRef = storageRef.child("$participantId/$currentDay/$sessionFolder/$fileName")

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
                imagesRef.putBytes(imageData,metadata)
                    .addOnSuccessListener {
                        Log.d("UploadSuccess", "Image uploaded successfully: ${imageFile.name}")
                        deleteFileAfterUpload(imageFile)
                        //   SharedPreferencesUtils.clearCapturedImagePath(context)
                    }.addOnFailureListener { exception ->
                        Log.e("UploadError", "Failed to upload image: ${exception.message}")
                    }
            }else{
                Log.d("UploadError", "no imagefile")
            }
            //  } else {
            //  Log.e("BitmapError", "Failed to compress bitmap for upload: ${imageFile.name}")
            //  }
        }
        private fun deleteFileAfterUpload(file: File) {
            if (file.exists() && file.delete()) {
                Log.d("FileCleanup", "Deleted file: ${file.absolutePath}")
            } else {
                Log.e("FileCleanup", "Failed to delete file: ${file.absolutePath}")
            }
        }

        private fun savetotemptillWifi(context: Context, bitmap: Bitmap): File?{
            sessionManager.incrementSessionCounter()
            Log.d("UploadFromTemp", "saveImageToTemp FRONTCAMERA till wifi Back")
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
                val tempFile=File(tempDir, "front_s_${sessionCounter}_p${participantId}_${formattedDate}_${formattedTime}")
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
        /**old upload working**/
        /*
          fun uploadImageToFirebase(context: Context, imageFile: File,sessionDir: File, location: Location?) {
              Log.d("UploadFromTemp", "in upload to firebase front companion")
              var  sessionManager = SessionManager(context)
              val dateFormat = SimpleDateFormat("ddMMyyy_HHmmss", Locale.getDefault())
              val timeFormat = SimpleDateFormat("HHmmss", Locale.getDefault())
              val currentTime = System.currentTimeMillis()
              val formattedDate = dateFormat.format(Date(currentTime))
              val formattedTime = timeFormat.format(Date(currentTime))
              val storage = FirebaseStorage.getInstance()
              val storageRef = storage.reference

              val participantId = SharedPreferencesUtils.getParticipantId(context) ?: "unknown_participant"
              val currentDay = "Day_" + AccessService.getCurrentDay(context)
              val sessionCounter = sessionManager.getSessionCounter()
              //   val connection
              val sessionFolder = "session_$sessionCounter"
              val fullPath = imageFile.absolutePath
             // val sessionPart = fullPath.substringAfter("session_").substringBefore("/")
              val sessionPart = fullPath.substringAfter("temp_images/")

              val imagesRef =
                  storageRef.child("$participantId/$currentDay/${sessionPart}")
              // Create metadata with location
              val metadataBuilder = StorageMetadata.Builder()
                  .setCustomMetadata("uploadTimestamp", System.currentTimeMillis().toString())

              location?.let {
                  metadataBuilder.setCustomMetadata("latitude", it.latitude.toString())
                  metadataBuilder.setCustomMetadata("longitude", it.longitude.toString())
              }

              val metadata = metadataBuilder.build()
              val imageData = imageFile.readBytes()

              // Upload task
              val uploadTask = imagesRef.putBytes(imageData, metadata)
              uploadTask.addOnSuccessListener {
                  frontCamerapictureTaken =true

                  Log.d("HideCam", "Image uploaded successfully: ${imageFile.absolutePath}")
                  Log.d("UploadFromTemp", "Image uploaded successfully: ${imageFile.absolutePath}")

                  //  Toast.makeText(context, "Image uploaded successfully", Toast.LENGTH_SHORT).show()
                  SharedPreferencesUtils.clearCapturedImagePath(context) // Clear path after upload
                //  deleteFileAfterUpload(imageFile)
              }.addOnFailureListener { exception ->
                  frontCamerapictureTaken =false

                  Log.e("HideCam", "Failed to upload image: ${exception.message}")
                  Log.e("UploadFromTemp", "Failed to upload image: ${exception.message}")

                  //  Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show()
                  Log.e("BackCameraButton", "Failed to upload image: ${exception.message}")

              }
          }*/


        /* fun uploadImageToFirebase(context: Context, imageFile: File, location: Location?) {
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

             val imagesRef = storageRef.child("$participantId/$currentDay/$sessionFolder/${imageFile.name}")

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
        val uniqueId = UUID.randomUUID().toString()

        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val RESET_PICTURE_ACTION = "com.example.cameraswitch.RESET_PICTURE_ACTION"
        var frontCamerapictureTaken=false

    }
}
