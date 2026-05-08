package com.example.cameraswitch.UtilsAndCons

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.cameraswitch.Workers.SessionManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ScreenshotToTempUtil {
    fun saveImageToTempFolder(
        context: Context,
       imageData:ByteArray
    ): File? {
        return try {
            var sessionManager = SessionManager(context)

            val sessionCounter = sessionManager.getSessionCounter()
            val dateFormat = SimpleDateFormat("ddMMyyy_HHmmss", Locale.getDefault())
            val formattedDate = dateFormat.format(Date())

            val timeFormat = SimpleDateFormat("HHmmss", Locale.getDefault())
            val currentTime = System.currentTimeMillis()
            val formattedTime = timeFormat.format(Date(currentTime))

            val tempDir = File(context.filesDir, "temp_images/session_$sessionCounter")
            if (!tempDir.exists() && !tempDir.mkdirs()) {
                Log.e("SaveImage", "Failed to create temp directory")
                return null
            }


            //   val tempFile = File(tempDir, "$typepic$formattedDate.jpg")
            val tempFile = File (tempDir,"screenshot_${sessionCounter}_${formattedDate}_${formattedTime}")

            FileOutputStream(tempFile).use { fos ->
               // bitmap.compress(Bitmap.CompressFormat.JPEG, 30, fos)
                fos.write(imageData)
            }

            Log.d("SaveImage", "Image saved to temp folder: ${tempFile.absolutePath}")
            tempFile
        } catch (e: IOException) {
            Log.e("SaveImage", "Error saving image to temp folder", e)
            null
        }
    }
}
