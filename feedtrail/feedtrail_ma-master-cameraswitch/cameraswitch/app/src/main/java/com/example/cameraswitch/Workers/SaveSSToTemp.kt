package com.example.cameraswitch.Workers

import android.app.Service
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.cameraswitch.UtilsAndCons.ScreenshotToTempUtil
import com.example.cameraswitch.UtilsAndCons.SharedPreferencesUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SaveSSToTemp(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val imagePath = inputData.getString("image_path")
        val typePic = inputData.getString("type") ?: "screenshot"

        if (imagePath == null || imagePath.isEmpty()) {
            Log.e("SaveImageWorker", "No image path provided in inputData")
            return Result.failure()
        }

        val file = File(imagePath)
        if (!file.exists()) {
            Log.e("SaveImageWorker", "File does not exist at path: $imagePath")
            return Result.failure()
        }

        return try {
            //val bitmap = BitmapFactory.decodeFile(imagePath)
            val bitmap = withContext(Dispatchers.IO) {
                BitmapFactory.decodeFile(imagePath)
            }
            if (bitmap == null) {
                Log.e("SaveImageWorker", "Failed to decode image at path: $imagePath")
                return Result.failure()
            }
         /*   val tempFile = withContext(Dispatchers.IO) {
                saveScreenshotToSessionFolder(applicationContext, bitmap, typePic)
            }*/
            val tempFile = saveScreenshotToSessionFolder(applicationContext, bitmap, typePic)
            if (tempFile != null) {
                Log.d("SaveImageWorker", "Screenshot saved successfully: ${tempFile.absolutePath}")
                Result.success()
            } else {
                Log.e("SaveImageWorker", "Failed to save screenshot to session folder")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e("SaveImageWorker", "Unexpected error during saving process", e)
            Result.failure()
        }
    }

    private suspend fun saveScreenshotToSessionFolder(
        context: Context,
        bitmap: android.graphics.Bitmap,
        type: String
    ): File? {
        val sessionCounter = try {
            SessionManager(context).getSessionCounter()
        } catch (e: Exception) {
            Log.e("SaveImageWorker", "Failed to get session counter", e)
            0 // Fallback session counter
        }

        val baseDir = File(context.filesDir, "temp_images/session_$sessionCounter")
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            Log.e("SaveImageWorker", "Failed to create session directory: ${baseDir.absolutePath}")
            return null
        }

     //   val dateFormat = SimpleDateFormat("ddMMyyy_HHmmss", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HHmmss", Locale.getDefault())
        val currentTime = System.currentTimeMillis()
     //   val formattedDate = dateFormat.format(Date(currentTime))
        val formattedTime = timeFormat.format(Date(currentTime))

        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
        val formattedDateTime = dateFormat.format(Date())
        val pId= SharedPreferencesUtils.getParticipantId(context)

        val fileName = "screenshot_p(${pId})_{$sessionCounter}_${formattedDateTime}"


        val maxSizeInKB = 40
        val stream = ByteArrayOutputStream()

        val tempFile = File(baseDir, fileName)
        return try {
            tempFile.outputStream().use { fos ->
              //  stream.toByteArray().size / 1024 > maxSizeInKB
                withContext(Dispatchers.IO) {
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 10, fos)

                }
             //   bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 10, fos)
            }
            Log.d("SaveImageWorker", "Screenshot successfully saved to: ${tempFile.absolutePath}")
            tempFile
        } catch (e: Exception) {
            Log.e("SaveImageWorker", "Error saving screenshot to file: ${tempFile.absolutePath}", e)
            null
        }
    }
}


/*class SaveSSToTemp(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val imagePath = inputData.getString("image_path") ?: return Result.failure()
        //val metadataJson = inputData.getString("metadata") ?: "{}"
       // val metadata = JSONObject(metadataJson).toMap()
        val typepic= inputData.getString("type") ?: ""
        val imageData = inputData.getByteArray("image_data") ?: return Result.failure()
        return try {
       //     val bitmap = BitmapFactory.decodeFile(imagePath)
     //       if (bitmap != null) {
          //  val tempFile = saveImageToFile(imageData)
            val tempFile = ScreenshotToTempUtil.saveImageToTempFolder(applicationContext, imageData)
          //  if(tempFile!=null){

                if (tempFile != null) {
                    Log.d("SaveImageWorker", "Image saved to temp folder: ${tempFile.absolutePath}")
                    Result.success()
                } else {
                    Log.e("SaveImageWorker", "Failed to save image to temp folder")
                    Result.failure()
                }
         /*   } else {
                Log.e("SaveImageWorker", "Failed to decode image: $imagePath")
                Result.failure()
            }*/
        } catch (e: Exception) {
            Log.e("SaveImageWorker", "Error saving image to temp folder", e)
            Result.failure()
        }
    }

    private fun JSONObject.toMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        keys().forEach { key -> map[key] = getString(key) }
        return map
    }
}*/

