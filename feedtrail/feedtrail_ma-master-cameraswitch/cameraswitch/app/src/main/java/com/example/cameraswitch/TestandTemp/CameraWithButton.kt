package com.example.cameraswitch.TestandTemp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cameraswitch.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraWithButton : AppCompatActivity() {
//succesfully take pictures with button front and back//
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference
    private lateinit var auth: FirebaseAuth
    private var user = FirebaseAuth.getInstance().currentUser
    private lateinit var outputDirectory: File
    private val REQUEST_CAMERA_PERMISSION = 100
    private var currentCamera = CameraSelector.DEFAULT_FRONT_CAMERA
private var label="TESTING"
    private lateinit var imageView: ImageView
    private var isCaptured = false
    private var isFrontCameraPictureTaken = false
    private var isBackCameraPictureUploaded = false
    private var isFrontCameraSelected = true
    private var capturedImage: ByteArray? = null
    private var imagegone = false
    private var previousPackage: String? = null
    private lateinit var statusTextView: TextView
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        previousPackage = intent.getStringExtra("previous_package")
        Log.d("upload", "Previous package: $previousPackage")
        imageView = findViewById(R.id.imageView)
     //   statusTextView = findViewById(R.id.MainText)
        checkCameraPermission()
        // Initialize cameraExecutor
        imageCapture = ImageCapture.Builder().build()
        outputDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Initialize Firebase Storage
        storage = FirebaseStorage.getInstance()
        storageRef = storage.reference

        // Initialize Firebase Authentication
       // auth = Firebase.auth
        outputDirectory = getOutputDirectory()
        // Request camera permissions

        // Set up camera view
      //  startCamera(if (currentCamera == CameraSelector.DEFAULT_BACK_CAMERA) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK)



       /* if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }*/
        val captureButton= findViewById<Button>(R.id.take)
        val uploadButton= findViewById<Button>(R.id.upload)
        captureButton.setOnClickListener {
            takePhoto()


        }

// Button click listener for uploading
        uploadButton.setOnClickListener {
            if (isCaptured) {
                capturedImage?.let { image ->
                    // Upload the image to Firebase Storage
                    uploadCapturedImage(image)
                }

                Handler(Looper.getMainLooper()).postDelayed({
                    switchCamera()
                }, 3000)


                isCaptured = false
            } else {
                Toast.makeText(applicationContext, "Take a picture first", Toast.LENGTH_SHORT).show()
            }
        }

        // Set up capture button click listener





    }
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

   /* private fun startCamera(currentCamera: Int) {
        Log.d("upload", "starting camera")

        val previewView: PreviewView = findViewById(R.id.previewView)
        val preview = Preview.Builder().build()

        preview.setSurfaceProvider(previewView.surfaceProvider)

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val cameraSelector = CameraSelector.Builder().requireLensFacing(currentCamera).build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                // Handle error
            }
        }, ContextCompat.getMainExecutor(this))
        Log.d("upload", "end of startcamera")

    }*/
   private fun startCamera() {
       val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

       cameraProviderFuture.addListener({
           val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

           val cameraSelector = if (isFrontCameraSelected) {
               CameraSelector.DEFAULT_FRONT_CAMERA
           } else {
               CameraSelector.DEFAULT_BACK_CAMERA
           }



           val preview = Preview.Builder().build()
           val previewView: PreviewView = findViewById(R.id.previewView)
           preview.setSurfaceProvider(previewView.surfaceProvider)

           try {
               cameraProvider.unbindAll()
               cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
           } catch (exc: Exception) {
               // Handle error
               Log.e("CameraActivity", "Error starting camera", exc)
           }
       }, ContextCompat.getMainExecutor(this))
   }



  fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
      val cameraType = if (currentCamera == CameraSelector.DEFAULT_BACK_CAMERA) "back" else "front"
      val photoFile = File(outputFileResults.savedUri?.path!!)
      isCaptured = true

      // Show the captured image to the user
      val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
      imageView.setImageBitmap(bitmap)
      imageView.visibility = View.VISIBLE

      // Optionally, trigger the upload process here
      // Depending on your requirements, you might want to prompt the user to upload or upload automatically
      // For demonstration purposes, I'll show how to prompt the user
      // You can adjust this based on your actual requirements

      AlertDialog.Builder(this)
          .setTitle("Upload Image")
          .setMessage("Do you want to upload the captured image?")
          .setPositiveButton("Upload") { dialog, _ ->
              //uploadImageToFirebase(photoFile, cameraType)
              dialog.dismiss()
          }
          .setNegativeButton("Cancel") { dialog, _ ->
              dialog.dismiss()
          }
          .show()
  }


    private fun takePhoto() {
        Log.d("upload", "image in takePhoto")

        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture?.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    // Handle error
                    Toast.makeText(applicationContext, "Error capturing image", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Log.d("upload", "image saved")
                  /*  val cameraType = if (currentCamera == CameraSelector.DEFAULT_BACK_CAMERA) "back" else "front"
                    isCaptured = true
                    // Show the captured image to the user
                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                    imageView.setImageBitmap(bitmap)
                    imageView.visibility = View.VISIBLE

                    // Upload the image to Firebase Storage
                    uploadImageToFirebase(photoFile, cameraType)*/
                    val photoFile = File(outputFileResults.savedUri?.path ?: "")

                    // Read the image data into a byte array
                    capturedImage = photoFile.readBytes()

                    // Show the captured image to the user
                  //  val bitmap = BitmapFactory.decodeByteArray(capturedImage, 0, capturedImage?.size ?: 0)
                  //  imageView.setImageBitmap(bitmap)
                 //   imageView.visibility = View.VISIBLE
                     displayCapturedImage(photoFile)
                    // Set the flag indicating that the image is captured
                    isCaptured = true

                }
            })
    }

    private fun displayCapturedImage(photoFile: File) {
        val exif = ExifInterface(photoFile.absolutePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val rotationAngle = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
        val rotatedBitmap = Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            Matrix().apply { postRotate(rotationAngle.toFloat()) },
            true
        )

        imageView.setImageBitmap(rotatedBitmap)
        imageView.visibility = View.VISIBLE
    }


    private fun uploadCapturedImage(image: ByteArray) {
        val cameraType = if (isFrontCameraSelected) "front" else "back"
        val filename = "${SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())}.jpg"
        val directory = "camerapictures/$filename"
        val imagesRef = storageRef.child(directory)

        val uploadTask = imagesRef.putBytes(image)

        uploadTask.addOnSuccessListener {
            imageView.visibility = View.GONE
            Log.d(TAG, "Image uploaded to Firebase: $directory")
            Toast.makeText(this, "Image uploaded to Firebase", Toast.LENGTH_SHORT).show()

            imagegone = true
           if (!isBackCameraPictureUploaded && !isFrontCameraSelected) {
                isBackCameraPictureUploaded = true
            }
            if (isFrontCameraSelected && isBackCameraPictureUploaded) {
              //  finish()
                // Navigate back to the previous app
                val launchIntent = previousPackage?.let { packageName ->
                    packageManager.getLaunchIntentForPackage(packageName)
                }
                Log.d("upload", "Launch intent: $launchIntent")
                launchIntent?.let {
                    Log.d("upload", "Starting activity: $launchIntent")
                    startActivity(it)
                }
            }

        }.addOnFailureListener { exception ->

            Log.e(TAG, "Failed to upload image", exception)
            Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
        }
    }



    /*private fun uploadImageToFirebase(imageFile: File, cameraType: String) {
        Log.d("upload", "image inside uploadImageToFirebase1")
        user?.let {
            Log.d("upload", "image inside uploadImageToFirebase2")

            val directory = "camerapictures/${imageFile.nameWithoutExtension}_$cameraType"
            val imagesRef = storageRef.child(directory)

            val uploadTask = imagesRef.putFile(Uri.fromFile(imageFile))

            uploadTask.addOnSuccessListener {
                // Image uploaded successfully
                Log.e(label, "Image uploaded to Firebase")

                makeText(applicationContext, "Image uploaded to Firebase", Toast.LENGTH_SHORT).show()
                // Switch to back camera after upload
                switchCamera()
            }.addOnFailureListener {
                // Handle failure
                Log.e(label, "Failed to upload image")

                makeText(applicationContext, "Failed to upload image", Toast.LENGTH_SHORT).show()
            }
        }
    }*/
    /*private fun switchCamera() {
        Log.d("upload", "switch camera")

        currentCamera = if (currentCamera == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }


        // Restart camera with the new camera selector
        startCamera(if (currentCamera == CameraSelector.DEFAULT_BACK_CAMERA) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK)
    }*/
    private fun switchCamera() {
        isFrontCameraSelected = !isFrontCameraSelected
        startCamera()

        if (isFrontCameraSelected && isBackCameraPictureUploaded) {
            // Both front and back camera pictures are uploaded, close the activity
            finish()
          //  statusTextView.text = "Pictures uploaded , you can return to app"
           // val launchIntent = previousPackage?.let { packageName ->
              //  packageManager.getLaunchIntentForPackage(packageName)
        //   }
          //  launchIntent?.let {
           //     startActivity(it)
          //  }
        }
    }


    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return mediaDir ?: filesDir // Return filesDir if mediaDir is null
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request the permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        } else {
            // Permission has already been granted
            // Proceed with camera operations
            startCamera()
        }
    }

   /* private fun switchCameraAndTakePhoto() {
        // Toggle between front and back cameras
        isFrontCameraSelected = !isFrontCameraSelected

        // Determine the new camera lens facing direction
        val newLensFacing = if (isFrontCameraSelected) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }

        // Restart camera with the new lens facing direction
        startCamera(newLensFacing)
        takePhoto()
    }*/


    /*private fun takeFrontCameraPhoto() {
        currentCamera = CameraSelector.DEFAULT_FRONT_CAMERA
        startCamera(CameraSelector.LENS_FACING_FRONT)
        takePhoto()
    }

    private fun takeBackCameraPhoto() {
        currentCamera = CameraSelector.DEFAULT_BACK_CAMERA
        startCamera(CameraSelector.LENS_FACING_BACK)
        takePhoto()
    }


    private fun checkAndTakePhotos() {
        if (!isFrontCameraPictureTaken) {
            takeFrontCameraPhoto()
        } else if (!isBackCameraPictureTaken) {
            takeBackCameraPhoto()
        }
    }*/
    // Handle permission request result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with camera operations
            } else {
                // Permission denied, handle accordingly
                Toast.makeText(
                    this,
                    "Camera permission denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}