package com.example.cameraswitch.Workers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.util.Log
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.cameraswitch.Service.AccessService
import com.example.cameraswitch.UtilsAndCons.SharedPreferencesUtils
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

class UploadToFirebaseWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val parentDir = File(applicationContext.filesDir, "temp_images")
        val location = SharedPreferencesUtils.getSavedLocation(applicationContext)

        if (!parentDir.exists() || !parentDir.isDirectory) {
            Log.e("UploadWorker", "No temp_images directory found")
            return@withContext Result.failure()
        }

        parentDir.listFiles()?.forEach { sessionDir ->
            if (sessionDir.isDirectory && sessionDir.name.startsWith("session_")) {
                Log.d("UploadWorker", "Processing session directory: ${sessionDir.name}")

                sessionDir.listFiles()?.forEach { imageFile ->
                    if (imageFile.isFile) {
                        uploadImageToFirebase(applicationContext, imageFile, sessionDir, location)
                    } else {
                        Log.d("UploadWorker", "Found non-file: ${imageFile.name}")

                    }
                }
            } else {
                Log.e("UploadWorker", "Not a session directory: ${sessionDir.name}")
            }
        }

        Result.success()
    }
    private fun resizeBitmap(imageFile: File, maxWidth: Int, maxHeight: Int): Bitmap? {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true

        // Decode the image size to get the original width and height
        BitmapFactory.decodeFile(imageFile.absolutePath, options)
        val originalWidth = options.outWidth
        val originalHeight = options.outHeight

        // Calculate the scaling factor to fit within max width and height
        val scaleFactor = Math.min(originalWidth / maxWidth, originalHeight / maxHeight)

        options.inJustDecodeBounds = false
        options.inSampleSize = if (scaleFactor > 1) scaleFactor else 1

        // Decode the image file with scaling
        return BitmapFactory.decodeFile(imageFile.absolutePath, options)
    }
    private fun compressBitmap(bitmap: Bitmap, quality: Int): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }
    private fun uploadImageToFirebase(
        context: Context,
        imageFile: File,
        sessionDir: File,
        location: Location?
    ) {
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference
        val participantId =
            SharedPreferencesUtils.getParticipantId(context) ?: "unknown_participant"
        val currentDay = "Day_" + AccessService.getCurrentDay(context)
        val sessionFolder = sessionDir.name
        val fileName = imageFile.name
        val imagesRef = storageRef.child("$participantId/$currentDay/$sessionFolder/$fileName")

        val metadataBuilder = StorageMetadata.Builder()
        location?.let {
            metadataBuilder.setCustomMetadata("latitude", it.latitude.toString())
            metadataBuilder.setCustomMetadata("longitude", it.longitude.toString())
        }
        val metadata = metadataBuilder.build()

       // val resizedBitmap = resizeBitmap(imageFile)
        //val imageData = resizedBitmap?.let { compressBitmap(it) }

       // if (imageData != null) {
        //    imagesRef.putBytes(imageData, metadata)
     //   val resizedBitmap = resizeBitmap(imageFile, 800, 600)
        if(imageFile!=null) {

            //    val compressedData = compressBitmap(resizedBitmap!!, 50) // compress to 80% quality

                // Upload the compressed image to Firebase
         //   val byteArrayInputStream = ByteArrayInputStream(compressedData)

                  imagesRef.putFile(imageFile.toUri(), metadata)
              //  imagesRef.putFile(byteArrayInputStream, metadata)

                    .addOnSuccessListener {
                        Log.d(
                            "UploadWorker",
                            "Image uploaded successfully: ${imageFile.absolutePath}"
                        )
                        deleteFileAfterUpload(imageFile)
                        //   deleteFilesInDirectory(imageFile)
                    }.addOnFailureListener { exception ->
                        Log.e("UUploadWorker", "Failed to upload image: ${exception.message}")
                    }
            } else {
                Log.e(
                    "UploadError",
                    "Failed to prepare image for upload or imagefile is null: ${imageFile.name}"
                )
            }

    }


    private fun deleteFileAfterUpload(file: File) {
        if (file.delete()) {
            Log.d("UploadWorker", "Deleted file after upload: ${file.absolutePath}")
        } else {
            Log.e("UploadWorker", "Failed to delete file: ${file.absolutePath}")
        }
    }

    fun deleteFilesInDirectory(directory: File) {
        // Check if the directory exists
        if (directory.exists() && directory.isDirectory) {
            // List all files in the directory
            val files = directory.listFiles()

            // Loop through each file and delete it
            files?.forEach { file ->
                if (file.exists()) {
                    // Delete the file
                    val deleted = file.delete()
                    if (deleted) {
                        Log.d("UploadWorker", "File deleted: ${file.name}")
                    } else {
                        Log.d("UploadWorker", "Failed to delete: ${file.name}")
                    }
                }
            }
        }
    }
}