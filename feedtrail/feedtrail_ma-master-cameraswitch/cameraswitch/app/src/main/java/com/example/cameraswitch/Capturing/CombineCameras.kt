package com.example.cameraswitch.Capturing


import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.finishAffinity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.lifecycle.LifecycleService
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.cameraswitch.PagesandActivites.Permissions
import com.example.cameraswitch.R
import com.example.cameraswitch.Receivers.NetworkChangeReceiver
import com.example.cameraswitch.UtilsAndCons.NotificationUtils
import com.example.cameraswitch.UtilsAndCons.SharedPreferencesUtils
import com.example.cameraswitch.Workers.SessionManager
import com.example.cameraswitch.Workers.SavingCamToTemp
import com.example.cameraswitch.Workers.UploadToFirebaseWorker
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit





class CombineCameras : LifecycleService() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imageCapture: ImageCapture
    private var currentCamera = CameraSelector.DEFAULT_FRONT_CAMERA
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var handler: Handler
    private lateinit var sessionManager: SessionManager
    private var isServiceActive = true
    private var switchCount = 0
    private val maxSwitches = 2
    private var isCameraInitializing = true  // Flag to track initialization state
    private var isCameraBound = false  // Flag to check if camera is bound
  //  private lateinit var anrWatchdog: ANRWatchdog

    override fun onCreate() {
        super.onCreate()

     /*   anrWatchdog = ANRWatchdog()
            .setANRListener { thread, throwable ->
                // Handle ANR detection
                Log.e("ANRWatchdog", "ANR detected on thread: ${thread.name}", throwable)

                // Stop the service gracefully before the system kills the app
                stopSelf() // This stops the service to prevent the app from getting killed by the system
            }
            .start()*/

        handler = Handler(Looper.getMainLooper())
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Start foreground service
        val notification: Pair<Int, Notification> = NotificationUtils.getNotification(this)
        startForeground(notification.first, notification.second)

        initializeCameraProvider()

        sessionManager = SessionManager(applicationContext)
        sessionManager.incrementSessionCounter()
    }

    private fun initializeCameraProvider() {
        isCameraInitializing = true
        isCameraBound=false
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
            cameraProvider = cameraProviderFuture.get()
            // Move image capture to a background thread
            CoroutineScope(Dispatchers.IO).launch {
             //   imageCapture = ImageCapture.Builder().build()
                 imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY) // Optimize for low latency
                    .build()
              //  captureImageFromCurrentCamera(applicationContext)
              //  captureImageWithTimeout2(applicationContext)
                captureImageFromCurrentCameraD(applicationContext)
            }
            } catch (e: Exception) {
                Log.e("CombineCameras", "Error initializing camera provider", e)
            } finally {
                isCameraInitializing = false  // Reset flag when initialization completes
            }
        }, ContextCompat.getMainExecutor(this))
    }

    //suggestion
    private suspend fun captureImageFromCurrentCameraD(context: Context) {
        val maxRetries = 3 // Maximum number of retries
        var currentRetry = 0
      /*  if (!isCameraBound) {  // NEW: Check if camera is bound
            Log.e("CombineCameras", "Camera is not bound yet.")

            return
        }*/

        while (currentRetry < maxRetries) {
            try {
                if (!isServiceActive  || cameraProvider == null || imageCapture == null || !::imageCapture.isInitialized) {
                    Log.e("CombineCameras", "CameraProvider or ImageCapture is not initialized. Aborting capture.")
                    return
                }
                if (!::cameraExecutor.isInitialized || cameraExecutor.isShutdown) {
                    Log.e("CombineCameras", "cameraExecutor is shut down! Restarting...")
                    restartExecutor()
                }
                val cameraLabel = if (currentCamera == CameraSelector.DEFAULT_FRONT_CAMERA) "front" else "back"
                val photoFile = File(
                    applicationContext.filesDir,
                    SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + "_$cameraLabel"
                )

                withContext(Dispatchers.Main) {
                    try {
                        cameraProvider?.unbindAll() // Unbind the current camera
                        cameraProvider?.bindToLifecycle(this@CombineCameras, currentCamera, imageCapture) // Bind the new camera
                        isCameraBound=true
                    } catch (e: Exception) {
                        Log.e("CombineCameras", "Camera binding error", e)
                        throw e // Rethrow the exception to trigger a retry
                    }
                }

                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                imageCapture.takePicture(
                    outputOptions,
                    cameraExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                       /* override fun onError(exc: ImageCaptureException) {
                            Log.e("CombineCameras", "Error capturing image", exc)
                         //   throw exc // Rethrow the exception to trigger a retry
                            stopSelf()
                        }*/

                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            Log.d("CombineCameras", "Image captured: ${photoFile.absolutePath}")
                            triggerSaveImageWorker(photoFile)

                            // Switch camera after the image is saved
                            if (currentCamera == CameraSelector.DEFAULT_FRONT_CAMERA) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    switchCameraAndCaptureD()
                                }
                            } else {
                                stopSelf()
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {

                        }
                    }
                )
                return // Exit the loop if successful
            } catch (e: Exception) {
                currentRetry++
                Log.e("CombineCameras", "Image capture failed (attempt $currentRetry): ${e.message}")

                if (currentRetry < maxRetries) {
                    delay(1000) // Wait 1 second before retrying
                } else {
                    Log.e("CombineCameras", "Max retries reached for image capture. Stopping service.")
                    stopSelf() // Stop the service if max retries are reached
                    return
                }
            }
        }
    }
    private fun restartExecutor() {
        if (!::cameraExecutor.isInitialized || cameraExecutor.isShutdown) {
            Log.d("CombineCameras", "Restarting cameraExecutor...")
            cameraExecutor = Executors.newSingleThreadExecutor()
        }
    }

    private suspend fun switchCameraAndCaptureD() {
        val maxRetries = 3 // Maximum number of retries
        var currentRetry = 0

        while (currentRetry < maxRetries) {
            try {
                withContext(Dispatchers.Main) {
                    cameraProvider?.unbindAll() // Unbind the current camera
                }
                withContext(Dispatchers.IO) {
                    currentCamera = if (currentCamera == CameraSelector.DEFAULT_FRONT_CAMERA) {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    } else {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    }
                    captureImageFromCurrentCameraD(applicationContext)
                }
                return // Exit the loop if successful
            } catch (e: Exception) {
                currentRetry++
                Log.e("CombineCameras", "Camera switching failed (attempt $currentRetry): ${e.message}")

                if (currentRetry < maxRetries) {
                    delay(1000) // Wait 1 second before retrying
                } else {
                    Log.e("CombineCameras", "Max retries reached for camera switching. Stopping service.")
                    stopSelf() // Stop the service if max retries are reached
                    return
                }
            }
        }
    }



    private suspend fun captureImageWithTimeout(context: Context) {
        try {
            // Apply timeout to the camera capture logic
            withTimeout(5000) { // Timeout after 5 seconds
                captureImageFromCurrentCamera(context)  // Calls the original capture logic
            }
        } catch (e: TimeoutCancellationException) {
            Log.e("CombineCameras", "Camera capture timed out: ${e.message}")
            stopSelf() // Stop the service to prevent ANR
        }
    }
    private suspend fun captureImageWithTimeout2(context: Context) {
        val maxRetries = 3  // Set the maximum number of retries
        var currentRetry = 0

        // Retry logic
        while (currentRetry < maxRetries) {
            try {
                // Apply timeout to the camera capture logic
                withTimeout(5000) { // Timeout after 5 seconds
                    captureImageFromCurrentCamera(context)  // Calls the original capture logic
                }
                return  // Exit the loop if capture was successful
            } catch (e: TimeoutCancellationException) {
                currentRetry++
                Log.e("CombineCameras", "Camera capture timed out (attempt $currentRetry): ${e.message}")

                // If retries remain, log the retry and continue
                if (currentRetry < maxRetries) {
                    Log.d("CombineCameras", "Retrying image capture (attempt $currentRetry)...")
                } else {
                    Log.e("CombineCameras", "Max retries reached. Stopping service.")
                    stopSelf()  // Stop the service after max retries
                    return  // Exit after the last retry attempt
                }
            }
        }
    }

    private suspend fun captureImageFromCurrentCamera(context: Context) {

        if (cameraProvider == null || imageCapture == null || !::imageCapture.isInitialized) {
            Log.e("CombineCameras", "CameraProvider is null. Aborting capture.")
            return
        }

        val cameraLabel =
            if (currentCamera == CameraSelector.DEFAULT_FRONT_CAMERA) "front" else "back"
        val photoFile = File(
            applicationContext.filesDir,
            SimpleDateFormat(
                FILENAME_FORMAT,
                Locale.US
            ).format(System.currentTimeMillis()) + "_$cameraLabel"
        )

        try {
            withContext(Dispatchers.Main) {
                try {
                    cameraProvider?.unbindAll() // This must be on the main thread
                    cameraProvider?.bindToLifecycle(this@CombineCameras, currentCamera, imageCapture) // This must be on the main thread
                } catch (e: Exception) {
                    Log.e("CombineCameras", "Camera binding error", e)
                    return@withContext
                }
            }



            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            // Capture image on a background thread
            imageCapture.takePicture(
                outputOptions,
                cameraExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Log.e("CombineCameras", "Error capturing image", exc)
                    }

                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        Log.d("CombineCameras", "Image captured: ${photoFile.absolutePath}")
                        triggerSaveImageWorker(photoFile)

                        // Switch camera after the image is saved
                        if (currentCamera == CameraSelector.DEFAULT_FRONT_CAMERA) {
                            CoroutineScope(Dispatchers.IO).launch {
                                switchCameraAndCapture()
                            }
                        } else {
                            stopSelf()
                        }
                    }
                }
            )
        } catch (exc: Exception) {
            Log.e("CombineCameras", "Error capturing image", exc)
        }
    }

    private suspend fun switchCameraAndCapture() {
        withContext(Dispatchers.IO) {
            currentCamera = if (currentCamera == CameraSelector.DEFAULT_FRONT_CAMERA) {
                CameraSelector.DEFAULT_BACK_CAMERA
            } else {
                CameraSelector.DEFAULT_FRONT_CAMERA
            }
            captureImageFromCurrentCamera(applicationContext)
        }
    }

    private fun triggerSaveImageWorker(photoFile: File) {
        val inputData = Data.Builder()
            .putString("image_path", photoFile.absolutePath)
            .putString("camera_type", getCameraName())
            .build()

        val saveImageWorkRequest = OneTimeWorkRequestBuilder<SavingCamToTemp>()
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(saveImageWorkRequest)
    }

    private fun getCameraName(): String {
        return if (currentCamera == CameraSelector.DEFAULT_FRONT_CAMERA) "front" else "back"
    }

    private fun startForegroundService() {
        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel("camera_service", "Camera Service")
        } else {
            ""
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("FeedTrail")
            .setContentText("Collecting Data. Thank you for participating")
            .setSmallIcon(R.drawable.logo)
            .setAutoCancel(true)
            .build()

        startForeground(1337, notification)
    }

    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val channel =
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
        return channelId
    }

    override fun onDestroy() {
        //anrWatchdog.stop()
        if(cameraProvider!=null){
            cameraProvider?.unbindAll()
        }
        if(!isCameraInitializing || isCameraBound) {
            if (::cameraExecutor.isInitialized && !cameraExecutor.isShutdown) {
                cameraExecutor.shutdown()
                if (!cameraExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    cameraExecutor.shutdownNow() // Force shutdown if tasks are hanging
                }
            }


            isCameraBound = false

            stopForeground(true)
            handler.removeCallbacksAndMessages(null)
            isServiceActive = false
            super.onDestroy()

        }
       /* while (isCameraInitializing ||!isCameraBound) {
            //   stopForeground(true)

            return
        }*/
        while (isCameraInitializing) {
            //   stopForeground(true)

            return
        }
    }

    fun onPause() {
  /*      while (isCameraInitializing) {
            stopForeground(true)
            return
        }
        if(!isCameraInitializing) {
            if (::cameraExecutor.isInitialized && !cameraExecutor.isShutdown) {
                cameraExecutor.shutdown()
                if (!cameraExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    cameraExecutor.shutdownNow() // Force shutdown if tasks are hanging
                }
            }
      //      cameraProvider?.unbindAll()
            stopForeground(true)
        }*/
    }
    fun onResume() {
        // Reinitialize the camera executor if it was shut down in onPause()
        if (!::cameraExecutor.isInitialized || cameraExecutor.isShutdown) {
            cameraExecutor = Executors.newSingleThreadExecutor()
        }

        // Rebind the camera if it was unbound in onPause()
        initializeCameraProvider()
    }
    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"


    }
}
