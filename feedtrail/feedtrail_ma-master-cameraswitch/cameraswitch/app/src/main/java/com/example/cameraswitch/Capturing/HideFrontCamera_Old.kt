package com.example.cameraswitch.Capturing
import android.content.Context
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class HideFrontCamera_Old : LifecycleService() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imageCapture: ImageCapture

  /*  override fun onBind(intent: Intent?): IBinder? {
        return null
    }*/

    override fun onCreate() {
        super.onCreate()
        cameraExecutor = Executors.newSingleThreadExecutor()
        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val preview = Preview.Builder().build()

            // Bind use cases to camera
            try {
                cameraProvider.unbindAll() // Unbind use cases before rebinding
                cameraProvider.bindToLifecycle(
                    this, // Provide the LifecycleOwner
                    cameraSelector,
                    imageCapture,
                    preview
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    fun takePicture() {
        val photoFile = createFile(applicationContext, "yyyyMMddHHmmssSSS", ".jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    // Handle image saved
                }

                override fun onError(exception: ImageCaptureException) {
                    // Handle error
                }
            })
    }
    private fun createFile(context: Context, format: String, extension: String): File {
        val timeStamp: String = SimpleDateFormat(format, Locale.US).format(System.currentTimeMillis())
        val storageDir: File = context.getExternalFilesDir(null) ?: context.filesDir
        return File(storageDir, "JPEG_${timeStamp}_$extension")
    }
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val FILE_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_EXTENSION = ".jpg"
    }
}