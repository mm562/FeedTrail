package com.example.cameraswitch.Capturing


import android.annotation.SuppressLint
import android.app.Activity
import android.app.Activity.RESULT_CANCELED
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import android.util.Log
import android.view.Display
import android.view.OrientationEventListener
import android.view.WindowManager
import androidx.core.util.Pair
import androidx.core.util.component1
import androidx.core.util.component2
import androidx.fragment.app.Fragment.SavedState
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.cameraswitch.PagesandActivites.Permissions
import com.example.cameraswitch.Service.AccessService
import com.example.cameraswitch.UtilsAndCons.Constants
import com.example.cameraswitch.UtilsAndCons.NotificationUtils
import com.example.cameraswitch.UtilsAndCons.SharedPreferencesUtils
import com.example.cameraswitch.Workers.SaveSSToTemp
import com.example.cameraswitch.Workers.SavingCamToTemp
import com.example.cameraswitch.Workers.SessionManager
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
//import com.github.anrwatchdog:anrwatchdog
//import com.github.anrwatchdog
class ScreenCaptureService : Service() {
   // private var serviceScope = CoroutineScope(Dispatchers.IO)
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
   // private lateinit var anrWatchdog: ANRWatchdog

    private var screenshotJob: Job? = null // Job to manage screenshot coroutine
    private val screenshotInterval: Long = 5000 // Interval for screenshot capture
    private var mMediaProjection: MediaProjection? = null
    private var mStoreDir: String? = null
    private var mImageReader: ImageReader? = null
    private var mHandler: Handler? = null
    private var mDisplay: Display? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    private var mDensity = 0
    private var mWidth = 0
    private var mHeight = 0
    private var mRotation = 0
    private var mOrientationChangeCallback: OrientationChangeCallback? = null
    private lateinit var mediaProjection: MediaProjection
    private var screenshotCounter = 0
    private val screenshotLimit = 50
    private var intent : Intent? = null
    private var isCapturing = true
    var screenshotCount=0
    var screenshotdone:Boolean=false
    private val mCaptureRunnable = Runnable {
        Log.d(TAG, " calling captureScreen")
        CoroutineScope(Dispatchers.IO).launch {

            // startScreenshotLoop()
            //   }
            /**edited**/
            captureScreen()
        }

        /*    if (screenshotCounter >= screenshotLimit) {

                Log.e(TAG, "Screenshot limit reached. No more screenshots will be captured.")

                mHandler?.post {
                    sessionCounter++  // Move to the next session
                    screenshotCounter = 0  // Reset screenshot counter
                }
                stopProjection()

            }*/

        //   mHandler?.postDelayed(this, 5000) // Schedule the next capture after 5 seconds
    }
    private lateinit var sessionManager: SessionManager

    private inner class ImageAvailableListener(private val service: ScreenCaptureService) : ImageReader.OnImageAvailableListener {


        override fun onImageAvailable(reader: ImageReader) {
            screenshotdone=false
            //   captureScreen()
            Log.d("DEBUG", "in screenshots")
          //  mHandler?.postDelayed(mCaptureRunnable, 5000)
            screenshotCounter++
       // CoroutineScope(Dispatchers.IO).launch {
      //       mHandler?.postDelayed(mCaptureRunnable, 5000)
        //    if (screenshotCounter % 5 == 0) {
                startScreenshotLoop()
              //  screenshotCounter=0
            //if(screenshotCounter==1){
              //  screenshotdone=true
                SharedPreferencesUtils.saveScreenVar(applicationContext,screenshotdone)

            //}
          //  }
            //  startScreenshotLoop()
              //   captureScreen()
            //    delay(5000)
         //  }

            /* val storage = FirebaseStorage.getInstance()

              val storageRef = storage.reference
              val appContext = applicationContext
             FirebaseApp.initializeApp(appContext)*/

            //  service.mHandler?.removeCallbacks(service.mCaptureRunnable!!)
            //   service.mHandler?.postDelayed(service.mCaptureRunnable!!, 5000)


            /*
                            var fos: FileOutputStream? = null
                            var bitmap: Bitmap? = null
                            try {
                                mImageReader!!.acquireLatestImage().use { image ->
                                    if (image != null) {
                                        val planes = image.planes
                                        val buffer = planes[0].buffer
                                        val pixelStride = planes[0].pixelStride
                                        val rowStride = planes[0].rowStride
                                        val rowPadding = rowStride - pixelStride * mWidth

                                        // create bitmap
                                        bitmap = Bitmap.createBitmap(
                                            mWidth + rowPadding / pixelStride,
                                            mHeight,
                                            Bitmap.Config.ARGB_8888
                                        )
                                        bitmap!!.copyPixelsFromBuffer(buffer)

                                        // write bitmap to a file
                                        fos =
                                            FileOutputStream(mStoreDir + "/myscreen_" + IMAGES_PRODUCED + ".png")
                                        bitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, fos!!)
                                        IMAGES_PRODUCED++
                                        Log.e(
                                            TAG,
                                            "captured image: " + IMAGES_PRODUCED
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                if (fos != null) {
                                    try {
                                        fos!!.close()
                                    } catch (ioe: IOException) {
                                        ioe.printStackTrace()
                                    }
                                }
                                if (bitmap != null) {
                                    bitmap!!.recycle()
                                }
                            }*/
//starting here is correct
            /*
               val image = reader.acquireLatestImage() ?: return

                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * image.width

                // Create Bitmap
                val bitmap = Bitmap.createBitmap(
                    image.width + rowPadding / pixelStride,
                    image.height,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)

                // Create a reference to the image in Firebase Storage
                val imageRef = storageRef.child("images/${System.currentTimeMillis()}.jpg")

                // Convert bitmap to byte array
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                val data = baos.toByteArray()

                // Upload image to Firebase Storage
                val uploadTask = imageRef.putBytes(data)
                uploadTask.addOnSuccessListener { taskSnapshot ->
                    // Image uploaded successfully
                    Log.d(TAG, "Image uploaded: ${taskSnapshot.metadata?.path}")
                }.addOnFailureListener { exception ->
                    // Handle unsuccessful uploads
                    Log.e(TAG, "Failed to upload image: $exception")
                }.addOnCompleteListener {
                    // Clean up resources
                    image.close()
                }
            //service.
           // service.

            service.mHandler?.postDelayed({

                reader.setOnImageAvailableListener(this, service.mHandler)
            }, 5000) */
        }

    }
   // private fun saveScreenshotToTempFolder(context: Context, bitmap: Bitmap): File? {
   private suspend fun saveScreenshotToTempFolder(context: Context, bitmap: Bitmap) {

        withContext(Dispatchers.IO) {
            val sessionCounter = sessionManager.getSessionCounter()
            val dateFormat = SimpleDateFormat("ddMMyyy_HHmmss", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HHmmss", Locale.getDefault())
            val currentTime = System.currentTimeMillis()
            val formattedDate = dateFormat.format(Date(currentTime))
            val formattedTime = timeFormat.format(Date(currentTime))
            val participantId = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString(
                "PID",
                "unknown_participant"
            )

            val tempDir = File(context.filesDir, "temp_images/session_$sessionCounter")

            if (!tempDir.exists() && !tempDir.mkdirs()) {
                Log.e(TAG, "Failed to create temp directory for screenshots")
                return@withContext
            }

            val tempFile = File(
                tempDir,
                "SS_s${sessionCounter}_p${participantId}_${formattedDate}_${formattedTime}"
            )

            try {

                FileOutputStream(tempFile).use { fos ->
                    //   bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)

                    /*   val resized= Companion.resizeBitmap(bitmap)
                    resized?.let { Companion.compressBitmap(it) }*/

                    val resized = Companion.resizeBitmap(bitmap)
                    resized.compress(Bitmap.CompressFormat.JPEG, 30, fos)

                }

                /**resizing before compressing in the fileoutput**/
                /*   val resizedBitmap = Companion.resizeBitmap(bitmap)
            FileOutputStream(tempFile).use { fos ->
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 30, fos)
            }*/


                /*val bufferSize = bitmap.byteCount
            val byteBuffer = ByteBuffer.allocate(bufferSize)

            // Copy raw bitmap pixels into the buffer
            bitmap.copyPixelsToBuffer(byteBuffer)
            val byteArray = byteBuffer.array()

            FileOutputStream(tempFile).use { fos ->
                fos.write(byteArray)
                fos.flush()
            }*/

                Log.d(TAG, "Screenshot saved to temp folder: ${tempFile.absolutePath}")
                //    saveCapturedImagePath(context,tempFile.absolutePath)

             //   return tempFile
            } catch (e: Exception) {
                Log.e(TAG, "Error saving screenshot", e)
                return@withContext
            }
        }
       // sessionCounter++
    }

    private fun saveCapturedImagePath(context: Context, imagePath: String) {
        val sharedPref = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val imagePathSet = sharedPref.getStringSet(Constants.CAPTURED_IMAGE_PATH_KEY, mutableSetOf())!!
        imagePathSet.add(imagePath)
        sharedPref.edit().putStringSet(Constants.CAPTURED_IMAGE_PATH_KEY, imagePathSet).apply()
    }
    /*
    private fun saveCapturedImagePath(context: Context, imagePath: String, sourceType: String) {
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

    private inner class OrientationChangeCallback internal constructor(context: Context?) :
        OrientationEventListener(context) {
        override fun onOrientationChanged(orientation: Int) {
            val rotation = mDisplay!!.rotation
            if (rotation != mRotation) {
                mRotation = rotation
                try {
                    // clean up
                    if (mVirtualDisplay != null) mVirtualDisplay!!.release()
                    if (mImageReader != null) mImageReader!!.setOnImageAvailableListener(null, null)

                    // re-create virtual display depending on device width / height
                    createVirtualDisplay()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private inner class MediaProjectionStopCallback : MediaProjection.Callback() {
        override fun onStop() {
            Log.e(TAG, "stopping projection.")
            // mHandler!!.post {
            //       if (mVirtualDisplay != null)
        //    stopProjection()
        //    stoptheProjection="true"
          //   stopProjection()
       //     if(mHandler!=null) {
                mHandler?.removeCallbacksAndMessages(null)
         //   }
           // if(mVirtualDisplay!=null) {
                mVirtualDisplay!!.release()
               //  mVirtualDisplay == null
            //}
            // if (mImageReader != null) {
                 mImageReader!!.setOnImageAvailableListener(null, null)
          //  mImageReader==null
             //}
             //if (mOrientationChangeCallback != null) {
                 mOrientationChangeCallback!!.disable()
          //  mOrientationChangeCallback==null
             //}
            //if(mMediaProjection!=null) {
                mMediaProjection!!.stop()
                mMediaProjection!!.unregisterCallback(this@MediaProjectionStopCallback)
              //   mMediaProjection == null
            //}
         //   stoptheProjection="true"
           // stopProjection()
            //  }
           /* val exitTimer = Timer()
            exitTimer.schedule(object : TimerTask() {
                override fun run() {
                    Log.d("UploadFromTemp", "User exited for X minutes, preparing to upload.")
                    //  uploadFirebase= true
                    triggerUpload(applicationContext)

                }
            },  240 * 1000)*/
        }
    }



    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "ScsCapService onCreate")
      /*  anrWatchdog = ANRWatchdog()
            .setANRListener { thread, throwable ->
                // Handle ANR detection
                Log.e("ANRWatchdog", "ANR detected on thread: ${thread.name}", throwable)

                // Stop the service gracefully before the system kills the app
                stopSelf() // This stops the service to prevent the app from getting killed by the system
            }
            .start()*/

        sessionManager = SessionManager(applicationContext)
      //  sessionManager.incrementSessionCounter()

     //   val intent2 = Intent(applicationContext, CombineCameras::class.java)
       // stopService(intent2)
        /*   val networkReceiver = NetworkChangeReceiver()
           val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)

           registerReceiver(networkReceiver, intentFilter,Context.RECEIVER_NOT_EXPORTED)*/
        /*  val externalFilesDir = getExternalFilesDir(null)
          if (externalFilesDir != null) {
              mStoreDir = externalFilesDir.absolutePath + "/screenshots/"
              val storeDirectory = mStoreDir?.let { File(it) }
              if (!storeDirectory?.exists()!!) {
                  val success = storeDirectory.mkdirs()
                  if (!success) {
                      Log.e(TAG, "failed to create file storage directory.")
                      stopSelf()
                  }
              }
          } else {
              Log.e(TAG, "failed to create file storage directory, getExternalFilesDir is null.")
              stopSelf()
          }
  */
        if(stoppedProjection){
            Log.d("stopProj", "in oncreate")
/**edited**/
          //  stopProjection()
        }

        /* if (intent?.let { isStopCommand(it) } == true) {
             Log.e(TAG, "isStopCommand arrival")
             Log.d("stopProj", "in isStopCommand")

             // stoppedProjection=true
             stopProjection()
             stopSelf()
             //  sessionCounter++

         }*/
        // start capture handling thread
        object : Thread() {
            override fun run() {
                Looper.prepare()
                mHandler = Handler()
                /**edited**/
               //  mHandler = Handler(Looper.myLooper()!!)
                Log.d(TAG, "Handler and Looper are ready")
                mHandler?.post {
                    Log.d(TAG, "Handler is working")
                    //logic
                }
                //    sessionCounter++
                //  sessionCounter=1
                Looper.loop()
            }
        }.start()
       //     startScreenshotLoop()

    }
    fun stopProjection() {

        //  stoppedProjection=true
        //stoptheProjection="true"
        /*if (stoptheProjection=="true") {
            Log.d(TAG, "Projection already stopped")
            return
        }*/
     //   stoptheProjection="true"

        try {
   //    if(stoptheProjection =="true"){

            Log.d(TAG, "in stopProj itself")
            isCapturing = false

            screenshotJob?.cancel()
            screenshotJob = null
            serviceScope.cancel()

           // Remove all handler callbacks
           if(mHandler!=null){
               mHandler?.removeCallbacks(mCaptureRunnable)
               mHandler?.removeCallbacksAndMessages(null)

           }

           // Release MediaProjection and associated resources
         //  mVirtualDisplay?.release()
           //mVirtualDisplay = null




           if (mMediaProjection != null) {
               mMediaProjection?.stop()
               mMediaProjection=null
           }
           if (mVirtualDisplay != null) {
               mVirtualDisplay?.release()
               mVirtualDisplay=null
           }
           if(mImageReader!=null){
               mImageReader?.setOnImageAvailableListener(null, null)
               mImageReader?.close()
               mImageReader = null
           }
            mOrientationChangeCallback?.disable()
            mOrientationChangeCallback=null

Log.d(TAG,  "mHandler :${mHandler.toString()},Mediaproj : ${mMediaProjection.toString()}, Virtualdis: ${mVirtualDisplay.toString()}")
          /*  if (mHandler != null) {
                mHandler!!.post {
                    if (mMediaProjection != null) {
                        mMediaProjection!!.stop()
                        mMediaProjection=null
                    }
                }
            }*/
     //  }

       }catch (e: Exception){

                Log.e(TAG, "Error stopping projection: ${e.message}", e)
        }
    }

    private fun resizeBitmap(bitmap: Bitmap): Bitmap {
        // Determine the new dimensions for the resized bitmap
        val width = bitmap.width
        val height = bitmap.height
        val newWidth = 640 // Set the desired width
        val newHeight = (height * (newWidth.toFloat() / width)).toInt()

        // Resize the bitmap
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun compressBitmap(bitmap: Bitmap): ByteArray {
        // Compress the bitmap into a ByteArray
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream) // Adjust quality (0-100) as needed
        return outputStream.toByteArray()
    }

   @SuppressLint("SuspiciousIndentation")
   private suspend fun captureScreen() {
       if (!serviceScope.isActive) return
      //  val captureRunnable = object : Runnable {
        //    @SuppressLint("SuspiciousIndentation")
        //    override fun run() {
       if (screenshotCounter >= 200) {
           stopProjection()  // Stop projection when the limit is reached
           stopSelf()
           stopForeground(true)
         //  val notification: Pair<Int, Notification> = NotificationUtils.getNotification(this@ScreenCaptureService)
          // stopForeground(notification.first,notification.second)
           return
       }
       withContext(Dispatchers.IO) {
                mImageReader?.acquireLatestImage()?.let { image ->
                    try {
                        val planes = image.planes
                        val buffer = planes[0].buffer
                        val pixelStride = planes[0].pixelStride
                        val rowStride = planes[0].rowStride
                        val rowPadding  = rowStride - pixelStride * image.width
                        // Create Bitmap
                        val bitmap = Bitmap.createBitmap(
                            image.width + rowPadding / pixelStride,
                            image.height,
                            Bitmap.Config.ARGB_8888
                        )
                        bitmap.copyPixelsFromBuffer(buffer)
                        val resizedBitmap = resizeBitmap(bitmap)
                        val compressedData = compressBitmap(resizedBitmap)
                    //    screenshotCount++
                        // Convert bitmap to byte array
                       // val baos = ByteArrayOutputStream()
                     //   bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos)
                     //   val data = baos.toByteArray()
                                 //  serviceScope.launch {
                        val imageData = compressBitmap(bitmap)
                        /**Edited**/
                     //   if (screenshotCounter % 5 == 0) {
                            triggerSaveImageWorker(bitmap, "SS")
                       // }
                       /* if (screenshotCount >= screenshotLimit) {
                            Log.d(TAG, "Reached screenshot limit. Restarting capture loop.")
                            captureScreen()
                        }*/
                              //  triggerSaveImageWorker(imageData)
                      //  saveScreenshotToTempFolder(applicationContext, resizedBitmap)

                        //}
                      //  return@let bitmap

                    }finally {
                        image.close()
                    }


                }

                //   screenshotCounter++
             //   isCapturing = false

          //  }
        }

        // Start the initial screenshot capture

      //  mHandler?.post(captureRunnable)

          //  startScreenshotLoop()


    }
  /*  private fun triggerSaveImageWorker(photoFile: File, cameraType: String) {
        // val sessionManager = SessionManager(applicationContext)
        //  sessionManager.incrementSessionCounter()

        val inputData = Data.Builder()
            .putString("image_path", photoFile.absolutePath)
            //    .putString("camera_type", cameraType)
            .build()

        val saveImageWorkRequest = OneTimeWorkRequestBuilder<SavingCamToTemp>()
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(saveImageWorkRequest)
    }*/
  /*private fun triggerSaveImageWorker(imageData: ByteArray) {
      val inputData = Data.Builder()
          .putByteArray("image_data", imageData) // Pass the compressed image data
          .build()

      val saveImageWorkRequest = OneTimeWorkRequestBuilder<SaveSSToTemp>()
          .setInputData(inputData)
          .build()

      WorkManager.getInstance(applicationContext).enqueue(saveImageWorkRequest)
  }*/
     fun triggerSaveImageWorker(bitmap: Bitmap, type: String) {
        try {
            // Save the bitmap to a temporary file
            CoroutineScope(Dispatchers.IO).launch {
                val fileName = "screenshot_${System.currentTimeMillis()}"
              //  val tempFile = File(applicationContext.cacheDir, fileName)
                val tempFile = File(applicationContext.filesDir, fileName)

                 FileOutputStream(tempFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 30, fos)
                    /* val buffer = ByteBuffer.allocate(bitmap.byteCount)
                     bitmap.copyPixelsToBuffer(buffer)
                     fos.write(buffer.array())*/
                 //    fos.write(tempFile)
                }

                // Prepare the file path as input data
                val inputData = Data.Builder()
                    .putString("image_path", tempFile.absolutePath)
                    //.putString("type", type)
                    .build()

                // Enqueue the worker
                val saveImageWorkRequest = OneTimeWorkRequestBuilder<SaveSSToTemp>()
                    .setInputData(inputData)
                    .build()

                WorkManager.getInstance(applicationContext).enqueue(saveImageWorkRequest)
            }
        //    Log.d("SaveSStoTemp", "triggerSaveImageWorker: Image saved at ${tempFile.absolutePath}")

        } catch (e: Exception) {
            Log.e("SaveSStoTemp", "Error in triggerSaveImageWorker", e)
        }
    }


    /**Capture Screen with Mutex lock**/
/*
    @SuppressLint("SuspiciousIndentation")
    private fun captureScreen() {
        if (isCapturing) return
        isCapturing = true

        serviceScope.launch(Dispatchers.IO) { // Ensure coroutine context
            BackCameraButton.Companion.SharedResources.resourceMutex.withLock {
                Log.d("MutexDebug", "ScreenCaptureService acquired lock")
                mImageReader?.acquireLatestImage()?.let { image ->
                    try {
                        val androidId =
                            Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                        val dateFormat = SimpleDateFormat("ddMMyyy_HHmmss", Locale.getDefault())
                        val timeFormat = SimpleDateFormat("HHmmss", Locale.getDefault())
                        val currentTime = System.currentTimeMillis()
                        val formattedDate = dateFormat.format(Date(currentTime))
                        val formattedTime = timeFormat.format(Date(currentTime))
                        val storage = FirebaseStorage.getInstance()
                        val storageRef = storage.reference
                        val appContext = applicationContext
                        FirebaseApp.initializeApp(appContext)
                        val planes = image.planes
                        val buffer = planes[0].buffer
                        val pixelStride = planes[0].pixelStride
                        val rowStride = planes[0].rowStride
                        val rowPadding  = rowStride - pixelStride * image.width
                        //    val sessionCounter = sessionManager.getSessionCounter()
                        //  val sessionFolder = "session_${sessionCounter}"
                        val currentDay =
                            "Day_" + AccessService.getCurrentDay(this@ScreenCaptureService)

                        // Create Bitmap
                        val bitmap = Bitmap.createBitmap(
                            image.width + rowPadding / pixelStride,
                            image.height,
                            Bitmap.Config.ARGB_8888
                        )
                        bitmap.copyPixelsFromBuffer(buffer)
                        val resizedBitmap = resizeBitmap(bitmap)
                        val compressedData = compressBitmap(resizedBitmap)


                        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                        val participantId = prefs.getString("PID", "")
                        // Create a reference to the image in Firebase Storage
                        //          val imageRef = storageRef.child("$participantId/$currentDay/$sessionFolder/SS_s${sessionCounter}_p${participantId}_${formattedDate}_${formattedTime}")

                        // Convert bitmap to byte array
                        // val baos = ByteArrayOutputStream()
                        //   bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos)
                        //   val data = baos.toByteArray()

                            saveScreenshotToTempFolder(applicationContext, resizedBitmap)

                    } finally {
                        image.close()
                    }
                }
                Log.d("MutexDebug", "ScreenCaptureService released lock")
            }
            isCapturing = false
        }
    }**/
private fun resetImageSavedFlag(context: Context) {
    val sharedPref = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
    sharedPref.edit().putBoolean(Constants.IMAGE_SAVED_KEY, false).apply()
}

    fun restoreMediaProjection(context: Context, resultCode: Int, data: Intent): MediaProjection? {
        Log.d("ScreenCap", "restoring media projections")
        val mpManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mMediaProjection=mpManager.getMediaProjection(resultCode,data)
        return mpManager.getMediaProjection(resultCode, data)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification: Pair<Int, Notification> = NotificationUtils.getNotification(this@ScreenCaptureService)
        startForeground(
            notification.first,
            notification.second,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        )
        /*if (intent == null) {
            Log.e(TAG, "Received null Intent in onStartCommand")
            stopSelf()
            return START_NOT_STICKY
        }*/
     //  val (savedResultCode, savedData) = getScreenCaptureData(applicationContext)
      //  var (savedResultCode, savedData) = getScreenCaptureData(applicationContext)

       // var dataFromFile=getIntentFromFile(applicationContext)
        if (mMediaProjection != null) {
            Log.d(TAG, "Service restarted but projection is already running.")
            return START_STICKY
        }
        if (intent == null) {
            Log.e(TAG, "Received null Intent in onStartCommand")
            stopSelf()
            return START_NOT_STICKY
        }
                // return super.onStartCommand(intent, flags, startId)

        serviceScope.launch {
            if (isStopCommand(intent)) {
                Log.d(TAG, "Stop command received, stopping service")
                stopProjection()
                stopSelf()
              //  return START_NOT_STICKY
            } else if (isStartCommand(intent)) {
                //   isCapturing = true
                resetImageSavedFlag(applicationContext)
              //  val notification: Pair<Int, Notification> = NotificationUtils.getNotification(this@ScreenCaptureService)

             /*   startForeground(
                    notification.first,
                    notification.second,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )*/



                val resultCode = intent.getIntExtra(RESULT_CODE, RESULT_CANCELED)
                val data: Intent? = intent.getParcelableExtra(DATA)
             /*   if(data!=null) {
                    saveScreenCaptureData(applicationContext,resultCode,data)
                   // saveIntentToFile(applicationContext,data)
                }*/

                if (data != null) {
              //  if(savedData!=null){
                 //   saveScreenCaptureData(applicationContext, resultCode, data)
              //      startProjection(savedResultCode, savedData)


                       startProjection(resultCode,data)
                    //    getIntentFromFile(applicationContext)
                 //   startProjection(savedResultCode,dataFromFile)
                }
              /*  else if (savedResultCode == Activity.RESULT_OK && savedData != null) {
                    Log.d("ScreenCap", "Restoring MediaProjection from saved data")
                    startProjection(savedResultCode, savedData)
                } else {
                    Log.e("ScreenCap", "No valid MediaProjection data available, stopping service")
                    stopSelf()
                }*/else {
                    Log.d("Screencap", " data is now null after probably app was destroyed")
                   // Log.d("Screencap", " data and code being restored, data: ${dataFromFile} resultcode:$savedResultCode")

                /*   if (savedResultCode == Activity.RESULT_OK && dataFromFile != null) {
                        Log.d("ScreenCap", "Restoring MediaProjection from saved data")
                        startProjection(savedResultCode, dataFromFile)
                    } else {
                        Log.e(
                            "ScreenCap",
                            "No valid MediaProjection data available, stopping service"
                        )
                        stopSelf()
                    }*/
                    Log.e(
                        "ScreenCap",
                        "No valid MediaProjection data available, stopping service"
                    )
                    stopSelf()
                }
        }/* else {
            Log.e("ScreenCap", "No saved MediaProjection data, stopping service")
            stopSelf()
        }*/
            if (stoppedProjection) {
                Log.d("stopProj", "inonstartCommand")
                /**edited**/
                //  stopProjection()
            } else {
                stopSelf()
            }

        }
        return START_STICKY
    }
   /* fun getScreenCaptureData(context: Context): Pair<Int, Intent?> {
        val sharedPref = context.getSharedPreferences("ScreenCapturePrefs", Context.MODE_PRIVATE)
        val resultCode = sharedPref.getInt("resultCode", Activity.RESULT_CANCELED)
        val dataUri = sharedPref.getString("data", null)

        if (dataUri == null) {
            Log.e("ScreenCap", "No saved intent found")
            return Pair(resultCode, null)
        }

        return try {
            val restoredIntent = Intent.parseUri(dataUri, Intent.URI_INTENT_SCHEME)

            if (restoredIntent != null) {
                Log.d("ScreenCap", "Restored valid screen capture Intent.")
                Pair(resultCode, restoredIntent)
            } else {
                Log.e("ScreenCap", "Failed to restore Intent from URI.")
                Pair(resultCode, null)
            }

        } catch (e: Exception) {
            Log.e("ScreenCap", "Error restoring screen capture Intent: ${e.message}", e)
            Pair(resultCode, null)
        }
    }*/

    /*fun getScreenCaptureData(context: Context): Pair<Int, Intent?> {
        val sharedPref = context.getSharedPreferences("Prefs", Context.MODE_PRIVATE)
        val resultCode = sharedPref.getInt("resultCode", Activity.RESULT_CANCELED)
        val dataUri = sharedPref.getString("data", null)
        val data = if (dataUri != null) Intent.parseUri(dataUri, Intent.URI_INTENT_SCHEME) else null
        return Pair(resultCode, data)
    }*/
  /*  fun getScreenCaptureData(context: Context): Pair<Int, Intent?> {
        val sharedPref = context.getSharedPreferences("ScreenCapturePrefs", Context.MODE_PRIVATE)
        val resultCode = sharedPref.getInt("resultCode", Activity.RESULT_CANCELED)
        val dataUri = sharedPref.getString("data", null)

        if (dataUri == null) {
            Log.e("ScreenCap", "No saved intent found")
            return Pair(resultCode, null)
        }

        return try {
            val restoredIntent = Intent.parseUri(dataUri, Intent.URI_INTENT_SCHEME)

            if (restoredIntent != null) {
                Log.d("ScreenCap", "Restored valid screen capture Intent.")
                Pair(resultCode, restoredIntent)
            } else {
                Log.e("ScreenCap", "Failed to restore Intent from URI.")
                Pair(resultCode, null)
            }

        } catch (e: Exception) {
            Log.e("ScreenCap", "Error restoring screen capture Intent: ${e.message}", e)
            Pair(resultCode, null)
        }
    }*/



    /* fun getScreenCaptureData(context: Context): Pair<Int, Intent?> {
       val sharedPref = context.getSharedPreferences("ScreenCapturePrefs", Context.MODE_PRIVATE)
       val resultCode = sharedPref.getInt("resultCode", Activity.RESULT_CANCELED)
       val encodedIntent = sharedPref.getString("data", null)

       if (encodedIntent != null) {
           try {
               val intentBytes = android.util.Base64.decode(encodedIntent, android.util.Base64.DEFAULT)
               val intentString = String(intentBytes, Charsets.UTF_8)
               val data = Intent.parseUri(intentString, Intent.URI_INTENT_SCHEME)

               Log.d("ScreenCap", "Restored valid screen capture Intent.")
               return Pair(resultCode, data)
           } catch (e: Exception) {
               Log.e("ScreenCap", "Error parsing stored screen capture data: ${e.message}")
           }
       }

       return Pair(Activity.RESULT_CANCELED, null)
   }*/
   /* fun saveScreenCaptureData(context: Context, resultCode: Int, data: Intent) {
        val sharedPref = context.getSharedPreferences("ScreenCapturePrefs", Context.MODE_PRIVATE)
        sharedPref.edit().apply {
            putInt("resultCode", resultCode)

            // Save the Intent as a base64 string to prevent corruption
            val intentBytes = data.toUri(Intent.URI_INTENT_SCHEME).toByteArray(Charsets.UTF_8)
            val intentEncoded = android.util.Base64.encodeToString(intentBytes, android.util.Base64.DEFAULT)
            putString("data", intentEncoded)

          //  putString("data", data.toString())

            apply()
        }
        Log.d("ScreenCap", "Saved screen capture data: resultCode: $resultCode, encoded intent.")
    }*/
  /* fun saveScreenCaptureData(context: Context, resultCode: Int, data: Intent) {
       val sharedPref = context.getSharedPreferences("ScreenCapturePrefs", Context.MODE_PRIVATE)
       sharedPref.edit().apply {
           putInt("resultCode", resultCode)

           // Convert the Intent to a Base64-encoded string
           val intentBytes = data.toUri(Intent.URI_INTENT_SCHEME).toByteArray(Charsets.UTF_8)
           val intentEncoded = android.util.Base64.encodeToString(intentBytes, android.util.Base64.DEFAULT)
           putString("data", intentEncoded)

           apply()
       }
       Log.d("ScreenCap", "Saved screen capture data: resultCode: $resultCode, encoded intent.")
   }*/


    /* fun getScreenCaptureData(context: Context): Pair<Int, Intent?> {
         val sharedPref = context.getSharedPreferences("ScreenCapturePrefs", Context.MODE_PRIVATE)
         val resultCode = sharedPref.getInt("resultCode", Activity.RESULT_CANCELED)

         // Retrieve the stored Base64-encoded Intent string
         val encodedIntent = sharedPref.getString("data", null)
         val data: Intent? = if (encodedIntent != null) {
             try {
                 val intentBytes = android.util.Base64.decode(encodedIntent, android.util.Base64.DEFAULT)
                 val intentString = String(intentBytes, Charsets.UTF_8)
                 Intent.parseUri(intentString, Intent.URI_INTENT_SCHEME)
             } catch (e: Exception) {
                 Log.e("ScreenCap", "Failed to decode stored Intent: ${e.message}")
                 null
             }
         } else {
             null
         }

         Log.d("ScreenCap", "Restored screen capture data: resultCode: $resultCode, data: $data")
         return Pair(resultCode, data)
     }*/

  /*  fun saveScreenCaptureData(context: Context, resultCode: Int, data: Intent) {
        val sharedPref = context.getSharedPreferences("ScreenCapturePrefs", Context.MODE_PRIVATE)
        sharedPref.edit().apply {
            putInt("resultCode", resultCode)
           // putString("data", data.toUri(Intent.URI_INTENT_SCHEME)) // Convert Intent to URI
            putString("data",data.toString())
            apply()
        }
        Log.d("ScreenCap", "Saved screen capture data: resultCode: $resultCode, data: $data")
    }*/
  fun getIntentFromFile(context: Context): Intent? {
      try {
          val file = File(context.filesDir, "screen_capture_intent.ser")
          if (file.exists()) {
              ObjectInputStream(FileInputStream(file)).use { it ->
                  val intent = it.readObject() as? Intent  // Deserialize the Intent
                  if (intent != null) {
                      Log.d("ScreenCap", "Intent restored successfully.")
                  }
                  return intent
              }
          } else {
              Log.e("ScreenCap", "Saved Intent file does not exist.")
          }
      } catch (e: Exception) {
          Log.e("ScreenCap", "Failed to retrieve Intent: ${e.message}")
      }
      return null
  }

    fun saveIntentToFile(context: Context, intent: Intent) {
      try {
          val file = File(context.filesDir, "screen_capture_intent.ser")
          ObjectOutputStream(FileOutputStream(file)).use { it ->
              it.writeObject(intent)  // Serialize the Intent
          }
          Log.d("ScreenCap", "Intent saved successfully.")
      } catch (e: Exception) {
          Log.e("ScreenCap", "Failed to save Intent: ${e.message}")
      }
  }

    fun saveScreenCaptureData(context: Context, resultCode: Int, data: Intent) {
      val sharedPref = context.getSharedPreferences("ScreenCapturePrefs", Context.MODE_PRIVATE)
      val editor = sharedPref.edit()

      // Save the result code and action
      editor.putInt("resultCode", resultCode)
    //  editor.putString("data", data.action) // Save the action of the Intent
      editor.putString("data", data.toString()) // Save the action of the Intent

      // Save the extras as a bundle (if any)
      val bundle = data.extras
      if (bundle != null) {
          val bundleString = bundle.toString() // You could convert the bundle to a String or save individual items
          editor.putString("extras", bundleString)
      }

      // Apply changes to SharedPreferences
      editor.apply()

      Log.d("ScreenCap", "Saved screen capture data: resultCode: $resultCode")
  }
    fun getScreenCaptureData(context: Context): Pair<Int, Intent?> {
        val sharedPref = context.getSharedPreferences("ScreenCapturePrefs", Context.MODE_PRIVATE)
        // Retrieve the saved resultCode and action
        val resultCode = sharedPref.getInt("resultCode", Activity.RESULT_CANCELED)
        val data = sharedPref.getString("data", null)

        if (data == null) {
            Log.e("ScreenCap", "No saved intent action found")
            return Pair(resultCode, null)
        }

        // Create a new intent with the saved action
        val intent = Intent(data)
        //restoreMediaProjection(context,resultCode,data)
        if (data != null) {
            intent.data = Uri.parse(data)
        }
        // Retrieve and add the extras, if any
        val extrasString = sharedPref.getString("extras", null)
        if (extrasString != null) {
            // Assuming the extras are stored as a string, you would need to parse and reconstruct the Bundle here
            val bundle = Bundle()
            bundle.putString("data", extrasString) // Add the necessary data to the bundle
            intent.putExtras(bundle)
        }
       /* val restoredIntent = Intent().apply {
            if (data != null) {
                try {
                    // Parse the saved string into an Intent
                    this.data = Uri.parse(dataString) // Assuming the data was stored as a URI or similar string
                } catch (e: Exception) {
                    Log.e("ScreenCap", "Error restoring intent data from string: ${e.message}")
                }
            }
        }*/
        if (extrasString != null) {
            val bundle = Bundle()  // You can parse the extrasString further if needed
            bundle.putString("data", extrasString)  // This is a basic way to store extras; refine this as needed
            intent.putExtras(bundle)
        }

        // Now that we have the resultCode and the restored intent, we can call restoreMediaProjection
      /* if (intent != null) {
           restoreMediaProjection(context, resultCode, intent)
        } else {
            null
        }*/
        Log.d("ScreenCap", "Restored/Got valid screen capture Intent., resultcode: $resultCode, data: $data")
       // return Pair(resultCode, intent)
        return Pair(resultCode, intent)

    }
 /*fun getScreenCaptureData(context: Context): Pair<Int, Intent?> {
     val sharedPref = context.getSharedPreferences("ScreenCapturePrefs", Context.MODE_PRIVATE)

     // Retrieve the resultCode and action
     val resultCode = sharedPref.getInt("resultCode", Activity.RESULT_CANCELED)
     val action = sharedPref.getString("data", null)

     if (action == null) {
         Log.e("ScreenCap", "No saved action found for Intent")
         return Pair(resultCode, null)
     }

     // Rebuild the intent with the saved action
     val intent = Intent(action)

     // Retrieve and restore the extras (if any)
     val extrasString = sharedPref.getString("extras", null)
     if (extrasString != null) {
         // Convert the string back to a bundle
         val bundle = stringToBundle(extrasString)  // A helper method to parse the bundle string back
         intent.putExtras(bundle)
     }

     Log.d("ScreenCap", "Restored valid screen capture Intent., resultCode: $resultCode, data: $action")
     return Pair(resultCode, intent)
 }

    // Helper method to convert the bundle string back to a Bundle
    fun stringToBundle(extrasString: String): Bundle {
        val bundle = Bundle()
        val keyValuePairs = extrasString.split(",")  // You might need to adjust this depending on your serialization method
        for (pair in keyValuePairs) {
            val keyValue = pair.split("=")
            if (keyValue.size == 2) {
                bundle.putString(keyValue[0], keyValue[1])  // Assuming String data type for simplicity
            }
        }
        return bundle
    }*/



    private fun startProjection(resultCode: Int, data: Intent) {
        Log.d("ScreenCap", "in Projection resultCode: $resultCode and data: $data")
     //   saveScreenCaptureData(applicationContext,resultCode, data)

       val mpManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mMediaProjection = mpManager.getMediaProjection(resultCode, data!!)
         mpManager.getMediaProjection(resultCode, data!!)

        Log.d("ScreenCap", "in startprojection passing resultCode: $resultCode, data:$data")
        if (mMediaProjection == null) {
            Log.d(TAG, "Mediaprojections null")
           // stopProjection()
            stopSelf()

        }else if (mMediaProjection != null) {

            // display metrics
            mDensity = Resources.getSystem().displayMetrics.densityDpi
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            mDisplay = windowManager.defaultDisplay

            Log.d(TAG, "creating virtual dispplay")

            // create virtual display depending on device width / height
            createVirtualDisplay()

         //   startScreenshotLoop()
            // register orientation change callback
            mOrientationChangeCallback = OrientationChangeCallback(this)
            if (mOrientationChangeCallback!!.canDetectOrientation()) {
                mOrientationChangeCallback!!.enable()
            }

            // register media projection stop callback
          //  mMediaProjection!!.registerCallback(MediaProjectionStopCallback(), mHandler)
       //     mImageReader!!.setOnImageAvailableListener(ImageAvailableListener(this), mHandler)

            //   startScreenshotLoop()


        }


    }
    private fun requestNewScreenCapturePermission() {
        Log.d(TAG, "Requesting new screen capture permission...")

        val intent = Intent(this, Permissions::class.java)
        intent.putExtra("request_screen_capture", true)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Launch activity from service
        startActivity(intent)
    }


    private fun startScreenshotLoop() {
        Log.d("Screencaptureservice", "i am in startscreenshotloop")
        if(screenshotJob?.isActive==true) return


        screenshotJob = serviceScope.launch {
          //  while (isActive) {
            //   screenshotCount = 0
            while(isCapturing){
                captureScreen() // Call your existing screenshot capture logic
            //    delay(screenshotInterval) // Wait for the next capture
            // Capture the screen asynchronously
               delay(5000)

            /*    if (screenshotCount >= screenshotLimit) {
                    Log.d(TAG, "Reached screenshot limit. Restarting capture loop.")
                    restartCaptureLoop() // Restart the loop
                    break
                }
                captureScreen()
                screenshotCount++*/
            }
        }
    }
    private fun restartCaptureLoop() {
        stopProjection() // Stops media projection and releases resources
       // screenshotJob?.cancel()
        //screenshotJob = null
        isCapturing = true // Reset capturing flag
        val intent = Intent(applicationContext, ScreenCaptureService::class.java)
        intent.putExtra(ACTION, START) // Restart media projection
        startScreenshotLoop() // Restart screenshot loop
    }

    /*   private fun startScreenshotLoop() {
           screenshotJob = serviceScope.launch {
               while (isActive) {
                   val startTime = System.currentTimeMillis()

                   try {
                       captureScreen() // Call your existing screenshot capture logic
                   } catch (e: Exception) {
                       Log.e(TAG, "Error capturing screen: ${e.message}", e)
                   }

                   val elapsedTime = System.currentTimeMillis() - startTime
                   val remainingTime = screenshotInterval - elapsedTime
                   if (remainingTime > 0) {
                       delay(remainingTime)
                   } else {
                       Log.w(TAG, "Screenshot took longer than the interval!")
                   }
               }
           }

       }*/

    @SuppressLint("WrongConstant")
    private fun createVirtualDisplay() {
        mWidth = Resources.getSystem().displayMetrics.widthPixels
        mHeight = Resources.getSystem().displayMetrics.heightPixels
//TODO:change amount of max images
        /** 2 allows one image to be processed while the next one is captured, avoiding buffer overflow.**/
        mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, screenshotLimit)
        mVirtualDisplay = mMediaProjection!!.createVirtualDisplay(
            SCREENCAP_NAME, mWidth, mHeight,
            mDensity,
            virtualDisplayFlags, mImageReader!!.surface, null, mHandler
        )
        //  mImageReader!!.setOnImageAvailableListener(ImageAvailableListener(), mHandler)

        mImageReader!!.setOnImageAvailableListener(ImageAvailableListener(this), mHandler)

        if (mVirtualDisplay != null || mImageReader!=null) {
            Log.d(TAG, "Virtual display created")
        } else {
            Log.e(TAG, "Failed to create virtual display")
        }
    //   mHandler?.postDelayed(mCaptureRunnable, 5000)
      //  startScreenshotLoop()
    }

    companion object {
        private const val TAG = "ScreenCaptureService"
        private const val RESULT_CODE = "RESULT_CODE"
        private const val DATA = "DATA"
        private const val ACTION = "ACTION"
        private const val START = "START"
        private const val STOP = "STOP"
        private const val SCREENCAP_NAME = "screencap"
        private const val PREFS_NAME = "ScreenCapturePrefs"
        private const val PERMISSION_SCREENSHOT_CODE = 104
        //   private var IMAGES_PRODUCED = 0
        /*   fun getStartIntent(context: Context?, resultCode: Int, data: String?): Intent {
               val intent = Intent(context, ScreenCaptureService::class.java)
               intent.putExtra(ACTION, START)
               intent.putExtra(RESULT_CODE, resultCode)
               intent.putExtra(DATA, data)
               return intent
           }*/
        var screenshotsTaken=false

        private var stoppedProjection =false
        private var stoptheProjection ="false"

        //  var sessionCounter = 1

        /*
        fun triggerUpload(context: Context, sourceType: String) {
            val pathKey = when (sourceType) {
                "camera" -> Constants.BACK_CAMERA_PATH_KEY
                "screenshots" -> Constants.SCREENSHOT_PATH_KEY
                else -> return
            }
            val networkReceiver = NetworkChangeReceiver()
            val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            context.registerReceiver(networkReceiver, intentFilter)

            // sendBroadcast(frontCameraIntent)
            Log.d("UploadFromTemp", "Backcamera upload trigger received in companion")
//            sessionManager.incrementSessionCounter()


            val location = SharedPreferencesUtils.getSavedLocation(context)
            val sharedPref = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            // val imagePathSet = sharedPref.getStringSet(Constants.CAPTURED_IMAGE_PATH_KEY, mutableSetOf())!!
            val imagePathSet = sharedPref.getStringSet(pathKey, mutableSetOf()) ?: mutableSetOf()

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
        fun triggerUpload(context: Context) {
            Log.d("UploadFromTemp", "Screenshots upload trigger received")
            val parentDir = File(context.filesDir, "temp_images")
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
                            uploadImageToFirebaseRules(
                                context,
                                sessionDir,
                                imageFile
                            )
                        } else {
                            Log.e("UploadFromTemp", "Not a file: \${imageFile.name}")
                        }
                    }
                } else {
                    Log.e("UploadFromTemp", "Not a session directory: \${sessionDir.name}")
                }
            }
        }
        fun uploadImageToFirebaseRules(context: Context, sessionsDir:File,imageFile: File){
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
                CoroutineScope(Dispatchers.IO).launch {
                  //  async {
                        uploadScreenshot(context, sessionsDir, imageFile)
                    //  }
                }

                Log.d("UploadFromTemp", "Wifi Data Connected, uploaded to firebase Back companion")

            }
        }

        public suspend fun uploadScreenshot(context: Context, sessionsDir: File, imageFile: File) {
            withContext(Dispatchers.IO) {
                val storage = FirebaseStorage.getInstance()
                val storageRef = storage.reference
                var sessionManager = SessionManager(context)

                //    val sessionCounter = sessionManager.getSessionCounter()
                val participantId = SharedPreferencesUtils.getParticipantId(context)
                    ?: "unknown_participant"
                val currentDay = "Day_" + AccessService.getCurrentDay(context)
                // val sessionFolder = "session_$sessionCounter"
                val sessionFolder = sessionsDir.name
                val screenshotRef = storageRef.child(
                    "$participantId/$currentDay/$sessionFolder/${imageFile.name}"
                )

                val imageData = BitmapFactory.decodeFile(imageFile.absolutePath).let { bitmap ->
                //  val resized=  resizeBitmap(bitmap)
                    compressBitmap100(bitmap)

                }

                val uploadTask = screenshotRef.putBytes(imageData)
              /*  val uploadTask = async {
                    screenshotRef.putBytes(imageData).await()
                }*/

                uploadTask.addOnSuccessListener {
                    screenshotsTaken = true
                    Log.d(TAG, "Screenshot uploaded: ${imageFile.absolutePath}")
                    //  sessionManager.incrementSessionCounter()

                    deleteFileAfterUpload(imageFile)

                }.addOnFailureListener { exception ->
                    screenshotsTaken = false
                    Log.e(TAG, "Failed to upload screenshot: ${exception.message}")
                }
            }
        }

        /*
       public fun uploadScreenshot(context: Context, imageFile: File) {
            Log.d("UploadFromTemp", "Processing file or directory: ${imageFile.absolutePath}")

            // If it's a directory, upload all files inside it
            if (imageFile.isDirectory) {
                Log.d(
                    "DirectoryFound",
                    "Found directory: ${imageFile.absolutePath}, uploading contents..."
                )
                imageFile.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        uploadScreenshot(context, file)
                    } else {
                        Log.e(
                            "FileError",
                            "Skipping invalid path (not a file): ${file.absolutePath}"
                        )
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
            val storage = FirebaseStorage.getInstance()
            val storageRef = storage.reference
            var  sessionManager = SessionManager(context)

            val sessionCounter = sessionManager.getSessionCounter()
            val participantId = SharedPreferencesUtils.getParticipantId(context)
                ?: "unknown_participant"
            val currentDay = "Day_" + AccessService.getCurrentDay(context)
            val sessionFolder = "session_$sessionCounter"
            val screenshotRef = storageRef.child(
                "$participantId/$currentDay/$sessionFolder/screenshots/${imageFile.name}"
            )
            val imageData = BitmapFactory.decodeFile(imageFile.absolutePath).let { bitmap ->
                compressBitmap(bitmap)
            }


            val uploadTask = screenshotRef.putBytes(imageData)
            uploadTask.addOnSuccessListener {
                screenshotsTaken=true
                Log.d(TAG, "Screenshot uploaded: ${imageFile.absolutePath}")
                deleteFileAfterUpload(imageFile)
            }.addOnFailureListener { exception ->
                screenshotsTaken=false
                Log.e(TAG, "Failed to upload screenshot: ${exception.message}")
            }
            if (imageData.isEmpty()) {
                Log.e(
                    "BitmapError",
                    "Failed to compress bitmap for upload: ${imageFile.absolutePath}"
                )
                return
            }


        }
*/
        @SuppressLint("SuspiciousIndentation")
        private fun savetotemptillWifi(context: Context, bitmap: Bitmap): File?{
          var sessionManager = SessionManager(context)
           // sessionManager.incrementSessionCounter()
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
                val tempFile=File(tempDir, "SS_s_${sessionCounter}_p${participantId}_${formattedDate}_${formattedTime}")
                Log.d("UploadFromTemp", "Temp directory path: ${tempDir.absolutePath}")
                FileOutputStream(tempFile).use { fos ->
                   //bitmap.compress(Bitmap.CompressFormat.JPEG, 50, fos)
                      val resized= resizeBitmap(bitmap)
                       resized?.let { Companion.compressBitmap(it) }
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
        private fun resizeBitmap(bitmap: Bitmap): Bitmap {
            // Determine the new dimensions for the resized bitmap
            val width = bitmap.width
            val height = bitmap.height
            val newWidth = 640 // Set the desired width
            val newHeight = (height * (newWidth.toFloat() / width)).toInt()

            // Resize the bitmap
            return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        }

        private fun compressBitmap(bitmap: Bitmap): ByteArray {
            // Compress the bitmap into a ByteArray
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 30, outputStream) // Adjust quality (0-100) as needed
            return outputStream.toByteArray()
        }
        private fun compressBitmap100(bitmap: Bitmap): ByteArray {
            // Compress the bitmap into a ByteArray
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream) // Adjust quality (0-100) as needed
            return outputStream.toByteArray()
        }
        private fun deleteFileAfterUpload(file: File) {
            if (file.exists() && file.delete()) {
                Log.d("FileCleanup", "Deleted file: ${file.absolutePath}")
            } else {
                Log.e("FileCleanup", "Failed to delete file: ${file.absolutePath}")
            }
        }
        private fun isMobileDataConnected(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            return networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        }

        private fun isWifiConnected(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            return networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }

        fun getStopIntent_SessionCounter(context: Context): Intent {
            Log.d(TAG, "getStopIntent session updat eand stopping screenshots")
            //    sessionCounter++
            stoppedProjection =true
            //    Log.d(TAG, "Update session : $sessionCounter")
            val intent = Intent(context, ScreenCaptureService::class.java)
            intent.putExtra(ACTION, STOP)
            return intent
        }
        fun getStopIntent(context: Context): Intent {
            Log.d(TAG, "getStopIntent called")
            stoppedProjection =true

            val intent = Intent(context, ScreenCaptureService::class.java)
            intent.putExtra(ACTION, STOP)
           // intent.putExtra(stoptheProjection, "true")
            return intent
        }

        private fun isStartCommand(intent: Intent): Boolean {
            return (intent.hasExtra(RESULT_CODE) && intent.hasExtra(DATA)
                    && intent.hasExtra(ACTION) && intent.getStringExtra(ACTION) == START)

        }

        fun isStopCommand(intent: Intent): Boolean {
            //   stoppedProjection=true
            Log.d(TAG, "isStopCommand called")

            return intent.hasExtra(ACTION) && intent.getStringExtra(ACTION) == STOP
        }

        fun getStartIntent(context: Context, resultCode: Int, data: Intent): Intent {
            Log.d("ScreenCap", "inside of getStartIntent in Screencapserv")

            val intent = Intent(context, ScreenCaptureService::class.java)
            intent.putExtra(ACTION, START)
            intent.putExtra(RESULT_CODE, resultCode)
            intent.putExtra(DATA, data)

            return intent
        }





        private const val KEY_SCREEN_CAPTURE_DATA_URI = "screen_capture_data_uri"
        private val virtualDisplayFlags: Int
            private get() = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
    }
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
     //   screenshotJob?.cancel()
        // Remove the scheduled screenshot capture callbacks
        //anrWatchdog.stop()
        isCapturing = false
        screenshotJob?.cancel()
        screenshotJob=null
        serviceScope.cancel()


        mHandler?.removeCallbacks(mCaptureRunnable)

        mHandler = null
        mHandler?.removeCallbacksAndMessages(null)

        stopProjection()

        if(mImageReader !=null){
            mImageReader?.close()
        }
        if(mVirtualDisplay !=null){
            mVirtualDisplay?.release()
        }
        if(mMediaProjection!=null){
            mMediaProjection?.stop()

        }
        super.onDestroy()

      /*  mImageReader?.close() // Close ImageReader
        mVirtualDisplay?.release() // Release VirtualDisplay
        mMediaProjection?.stop() // Stop MediaProjection*/

    }
}