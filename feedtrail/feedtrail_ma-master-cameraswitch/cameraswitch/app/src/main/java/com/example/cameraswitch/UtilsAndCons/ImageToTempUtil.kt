package com.example.cameraswitch.UtilsAndCons

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.example.cameraswitch.Workers.SessionManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageToTempUtil {
    fun saveImageToTempFolder(
        context: Context,
        bitmap: Bitmap,
      //  metadata: Map<String, String>,
        typepic:String
    ): File? {
        return try {
            var sessionManager = SessionManager(context)

            val sessionCounter = sessionManager.getSessionCounter()
         //   val dateFormat = SimpleDateFormat("ddMMyyy_HHmmss", Locale.getDefault())
          //  val formattedDate = dateFormat.format(Date())

            val timeFormat = SimpleDateFormat("HHmmss", Locale.getDefault())
            val currentTime = System.currentTimeMillis()
            val formattedTime = timeFormat.format(Date(currentTime))

            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            val formattedDateTime = dateFormat.format(Date())

            val tempDir = File(context.filesDir, "temp_images/session_$sessionCounter")
            if (!tempDir.exists() && !tempDir.mkdirs()) {
                Log.e("SaveImage", "Failed to create temp directory")
                return null
            }

           val pId= SharedPreferencesUtils.getParticipantId(context)
         //   val tempFile = File(tempDir, "$typepic$formattedDate.jpg")
            val tempFile = File (tempDir,"${typepic}_{$pId}_{$sessionCounter}_$formattedDateTime")
          /*  do {
                stream.reset() // Clear the stream for re-compression
                bitmap.compress(Bitmap.CompressFormat.JPEG, 30, stream)

            } while (stream.toByteArray().size / 1024 > maxSizeInKB && quality > 0)*/

            FileOutputStream(tempFile).use { fos ->
              //  stream.toByteArray().size / 1024 > maxSizeInKB
               // stream.reset() // Clear the stream for re-compression
                //stream.toByteArray().size / 1024 > maxSizeInKB && quality > 0
                bitmap.compress(Bitmap.CompressFormat.JPEG, 30, fos)

             //   bitmap.compress(Bitmap.CompressFormat.JPEG, 30, fos)
             //   fos.write(stream.toByteArray())
            }
          //  embedMetadata(tempFile, metadata)

            Log.d("SaveImage", "Image saved to temp folder: ${tempFile.absolutePath}")
            tempFile
        } catch (e: IOException) {
            Log.e("SaveImage", "Error saving image to temp folder", e)
            null
        }
    }


    private fun embedMetadata(file: File, metadata: Map<String, String>) {
        try {
            val exif = ExifInterface(file.absolutePath)


            // Embed custom metadata as user comments or other fields
            metadata.forEach { (key, value) ->
                when (key.lowercase(Locale.getDefault())) {
                    "location" -> {
                        // For location data
                        val locationParts = value.split(", ")
                        if (locationParts.size == 2) {
                            val lat = locationParts[0].substringAfter("Lat: ").toDoubleOrNull()
                            val lon = locationParts[1].substringAfter("Lon: ").toDoubleOrNull()
                            if (lat != null && lon != null) {
                                exif.setLatLong(lat, lon)
                            }
                        }
                    }
                    else -> {
                        // Add other metadata as user comment
                        exif.setAttribute(ExifInterface.TAG_USER_COMMENT, "$key: $value")
                    }
                }
            }

            exif.saveAttributes()
            val userComment = exif.getAttribute(ExifInterface.TAG_USER_COMMENT)
            val latLong = exif.latLong

            Log.d("Metadata", "Metadata embedded into image: $exif, $userComment,$latLong")
        } catch (e: IOException) {
            Log.e("Metadata", "Error embedding metadata", e)
        }
    }
}
