package com.example.cameraswitch.TestandTemp
import android.annotation.SuppressLint
import android.widget.Button
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.cameraswitch.R
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
class CameraAlterTestTemp: AppCompatActivity() {
    companion object {
        const val REQUEST_IMAGE_CAPTURE = 100
    }

    private var mediaPath: File? = null
    private lateinit var storageReference: StorageReference
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        storageReference = FirebaseStorage.getInstance().reference
        val buttonCapture: Button = findViewById(R.id.button_capture)
        buttonCapture.setOnClickListener {
            dispatchTakePictureIntent()
        }
    }


    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            mediaPath = createNewImageFile(this)
            val photoURI = FileProvider.getUriForFile(this, "$packageName.provider", mediaPath!!)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle error: camera app not found or other issues
        }
    }

    // Create a new image file
    @Throws(IOException::class)
    private fun createNewImageFile(context: Context): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            absolutePath
        }
    }

    // Handle result after capturing photo
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) {
            return
        }
        when (requestCode) {
            REQUEST_IMAGE_CAPTURE -> {
                val resultBitmap = BitmapFactory.decodeFile(mediaPath?.absolutePath)
                uploadToFirebase(resultBitmap)
            }
        }
    }

    // Upload bitmap to Firebase
    private fun uploadToFirebase(bitmap: Bitmap) {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storagePath = "images/IMG_$timeStamp.jpg"
        val imageRef = storageReference.child(storagePath)

        // Compress bitmap to JPEG
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val imageData = baos.toByteArray()

        // Upload image to Firebase Storage
        imageRef.putBytes(imageData)
            .addOnSuccessListener {
                Log.d("FirebaseUpload", "Image uploaded successfully: ${it.metadata?.path}")
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    Log.d("FirebaseUpload", "Download URL: $uri")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseUpload", "Error uploading image: ${exception.message}")
            }
    }
}