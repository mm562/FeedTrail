package com.example.cameraswitch.Capturing

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BuiltInBackCamera
    : AppCompatActivity() {

 /*   private val CAMERA_REQUEST_CODE = 101
    private var currentPhotoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Immediately open the camera when this activity starts
        openCamera()
    }

    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        // Attempt to force the back camera
        takePictureIntent.putExtra("android.intent.extras.CAMERA_FACING", 0)

        // Check if there is an app to handle the intent
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            // Create a file to save the image
            val photoFile = createImageFile()
            photoFile?.also {
                val photoURI = FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.fileprovider",
                    it
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE)
            }
        } else {
            // Handle the case where no camera app is available
            finish() // Exit the activity
        }
    }

    private fun createImageFile(): File? {
        return try {
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir: File = filesDir // Use app's internal storage
            File.createTempFile(
                "IMG_${timeStamp}_", /* prefix */
                ".jpg", /* suffix */
                storageDir /* directory */
            ).apply {
                currentPhotoPath = absolutePath
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Photo has been captured and saved to the path
            currentPhotoPath?.let {
                // Handle the saved photo (already stored in filesDir)
                // For example, notify the user or pass the file path to another activity
            }
        } else {
            // If the user cancels or something goes wrong, exit the activity
            finish()
        }
    }*/
}
