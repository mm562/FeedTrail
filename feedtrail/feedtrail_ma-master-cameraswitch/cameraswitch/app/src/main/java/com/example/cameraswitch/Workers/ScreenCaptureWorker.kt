package com.example.cameraswitch.Workers

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.Surface
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class ScreenCaptureWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val workManager = WorkManager.getInstance(appContext)
    private var mImageReader: ImageReader? = null
    private var mSurface: Surface? = null
    private var mMediaProjection: MediaProjection? = null
    private var mVirtualDisplay: android.hardware.display.VirtualDisplay? = null
    private var mDisplay: Display? = null
    private var mDensity: Int = 0

    // This method will be triggered periodically (every 5 seconds)
    override suspend fun doWork(): Result {
        Log.d("ScreenCaptureWorker", "Worker started to capture screen")
        Log.d("DebugWorker", "Worker started to capture screen")

        val sharedPreferences = applicationContext.getSharedPreferences("PREFS_NAME", Context.MODE_PRIVATE)

        // Retrieve stored values
        val resultCode = sharedPreferences.getInt("KEY_SCREEN_CAPTURE_PERMISSION", Activity.RESULT_CANCELED)
        val dataUriString = sharedPreferences.getString("KEY_SCREEN_CAPTURE_DATA_URI", null)
     /*try {
            // Start screen capture with MediaProjection
            captureScreen()

            // Capture the image and process it
            val image = captureImage()
            val resizedBitmap = resizeBitmap(image)
            val compressedData = compressBitmap(resizedBitmap)

            // Upload to Firebase or any backend
           // uploadImageToFirebase(compressedData)
            if (!isActive) {
                stopProjection()
                return Result.failure() // Return failure if the worker was cancelled
            }
            return Result.success()  // Return success after task completion
        } catch (e: Exception) {
            Log.e("ScreenCaptureWorker", "Error capturing screenshot: ${e.message}")
            return Result.failure()  // Return failure if an error occurs
        }*/


   //     val resultCode = inputData.getInt("RESULT_CODE", Activity.RESULT_CANCELED)
        val dataString = inputData.getString("DATA")
        val data = Intent().apply {
            dataUriString?.let { putExtra("DATA", it) }
        }
        Log.d("DebugWorker", "data: $data, result: $resultCode")
        // Check if the resultCode is valid
        if (resultCode == Activity.RESULT_OK && data != null) {
            // Proceed with the screen capture
            captureScreen(resultCode, data)
            return Result.success()
        } else {
            // If permission was denied or something went wrong
            return Result.failure()
        }
    }

    // Capture the image using ImageReader to capture the screen
    private fun captureImage(imageReader: ImageReader): Bitmap {
        // Use MediaProjection and ImageReader to capture the screen
        // This code assumes you already have a valid MediaProjection and ImageReader setup
        val image: Image? = imageReader.acquireLatestImage()  // Capture the latest image

        if (image == null) {
            throw Exception("Failed to capture image.")
        }

        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width

        // Create a Bitmap from the Image
        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)

        // Close the image to release resources
        image.close()

        return bitmap
    }

    // Resize the bitmap to a smaller size for efficiency (e.g., for faster uploading and lower memory consumption)
    private fun resizeBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val newWidth = 640  // Set the desired width for resizing
        val newHeight = (height * (newWidth.toFloat() / width)).toInt()

        // Create and return the resized bitmap
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    // Compress the bitmap into a byte array for uploading
    private fun compressBitmap(bitmap: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)  // Compress at 50% quality
        return outputStream.toByteArray()
    }

    // Upload the compressed image to Firebase Storage (or any other backend service)
    private suspend fun uploadImageToFirebase(compressedData: ByteArray) {
        withContext(Dispatchers.IO) {
            val storage = FirebaseStorage.getInstance()
            val storageRef = storage.reference.child("images/${System.currentTimeMillis()}.jpg")

            // Upload the byte data to Firebase Storage
            val uploadTask = storageRef.putBytes(compressedData)
            uploadTask.addOnSuccessListener {
                Log.d("ScreenCaptureWorker", "Image uploaded successfully")
            }.addOnFailureListener { e ->
                Log.e("ScreenCaptureWorker", "Upload failed", e)
            }.await()
        }
    }

    // Start screen capture with MediaProjection
    @SuppressLint("ServiceCast")
   /* private fun captureScreen(resultCode: Int, data: Intent) {
        val mediaProjectionManager = applicationContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        // Get MediaProjection from Intent (this assumes you have already triggered the permission request)
        // This should come from your permission result
        val resultCode = 1234 // Use your actual result code here
        val resultData = applicationContext.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).getString("MediaProjectionData", null) ?: return
        val data = android.content.Intent(resultData) // MediaProjection data

        mMediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)

        if (mMediaProjection != null) {
            // Initialize display and density
            mDensity = applicationContext.resources.displayMetrics.densityDpi
            mDisplay = (applicationContext.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager).defaultDisplay

            // Set up the ImageReader to capture images from the screen
            mImageReader = ImageReader.newInstance(mDisplay!!.width, mDisplay!!.height, android.graphics.PixelFormat.RGBA_8888, 2)
            mSurface = mImageReader?.surface

            // Create Virtual Display to capture the screen
            mVirtualDisplay = mMediaProjection!!.createVirtualDisplay(
                "ScreenCapture",
                mDisplay!!.width,
                mDisplay!!.height,
                mDensity,
                android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mSurface,
                null, null
            )

            Log.d("ScreenCaptureWorker", "Screen capture started.")
        }
    }*/
    private fun captureScreen(resultCode: Int, data: Intent) {
        // Retrieve MediaProjection using the resultCode and data
        val mediaProjectionManager =
            applicationContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as android.media.projection.MediaProjectionManager
        val mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)

        // Set up the ImageReader and Surface for capturing the screen
        val display = (applicationContext.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager).defaultDisplay
        val density = applicationContext.resources.displayMetrics.densityDpi

        val imageReader = ImageReader.newInstance(display.width, display.height, android.graphics.PixelFormat.RGBA_8888, 2)
        val surface = imageReader.surface

        // Create a virtual display for screen capture
        mediaProjection.createVirtualDisplay(
            "ScreenCapture",
            display.width, display.height, density,
            android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            surface, null, null
        )

        // Capture the image and upload it (as an example)
        captureImage(imageReader)
    }

    // Stop the projection (screen capture) and release resources
    public fun stopProjection() {
        // Release any resources related to screen capture or MediaProjection
        Log.d("ScreenCaptureWorker", "Projection stopped.")

        // Close the ImageReader and release the Surface and VirtualDisplay
        mImageReader?.close()
        mSurface?.release()
        mVirtualDisplay?.release()
        mMediaProjection?.stop()
    }
    object MediaProjectionManagerSingleton {
        private var mediaProjection: MediaProjection? = null
        private var mImageReader: ImageReader? = null
        private var mSurface: Surface? = null
        private var mVirtualDisplay: VirtualDisplay? = null
        fun stopProjection() {
            mImageReader?.close()
            mSurface?.release()
            mVirtualDisplay?.release()
            mediaProjection?.stop()
        }
    }


}
