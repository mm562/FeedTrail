package com.example.cameraswitch.Capturing

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.VISIBLE
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.cameraswitch.PagesandActivites.Permissions
import com.example.cameraswitch.R
import com.example.cameraswitch.Workers.SavingCamToTemp
import com.example.cameraswitch.Workers.SessionManager
import com.example.cameraswitch.Workers.WorkerManagerScreenshots
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.CameraView

import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.controls.Mode
import com.otaliastudios.cameraview.size.SizeSelectors
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class CameraViewLibrary : AppCompatActivity() {

    private lateinit var cameraView: CameraView
    private lateinit var captureButton: ImageButton
    private lateinit var upload: ImageButton

    private lateinit var imageView: ImageView
    private lateinit var workerManager: WorkerManagerScreenshots
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.cameraviewlib)
        cameraView = findViewById(R.id.cameraView)
        cameraView.mode = Mode.PICTURE // Ensure it's in picture mode
        cameraView.setPictureSize(SizeSelectors.smallest())

        captureButton = findViewById(R.id.captureButton)
        upload= findViewById(R.id.uploadbutton)
        imageView = findViewById(R.id.imageView)

        cameraView.setLifecycleOwner(this) // Automatically starts/stops the camera

        captureButton.setOnClickListener {
            cameraView.takePictureSnapshot()
        }

        // Listen for picture capture
        cameraView.addCameraListener(object : CameraListener() {
            override fun onPictureTaken(result: com.otaliastudios.cameraview.PictureResult) {
                result.toFile(createPhotoFile()) { file ->
                    if (file != null) {
                        Log.d("CVlIB", "Photo saved: ${file.absolutePath}")
                        showCapturedImage(file)
                        upload.visibility=VISIBLE
                        triggerSaveImageWorker(file,"back")
                       // intent.putExtra(SKIP_LAST_STAGE_UPDATE, true)
                       startFrontCamera()

                    } else {
                        Log.e("CVlIB", "Failed to save photo.")
                    }
                }
            }
        })

        upload.setOnClickListener{
            stopCamera()
            stopCameraAndReturnToPreviousApp()
        }

    }
    private fun startFrontCamera(){
        val intent = Intent(this, HideCamera::class.java)

        startService(intent)
    }
    private fun triggerSaveImageWorker(photoFile: File,typepic:String) {
        val sessionManager= SessionManager(applicationContext)
        sessionManager.incrementSessionCounter()
      //  val metadataJson = JSONObject(metadata).toString()

        val inputData = Data.Builder()
            .putString("image_path", photoFile.absolutePath)
        //    .putString("metadata", metadataJson)
            .putString("type", typepic)
            .build()

        val saveImageWorkRequest = OneTimeWorkRequestBuilder<SavingCamToTemp>()
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(saveImageWorkRequest)
    }
    private fun getOutputFile(): File {
        val dir = getExternalFilesDir(null) ?: filesDir
        return File(dir, "captured_photo.jpg")
    }
    private fun createPhotoFile(): File {
        val photoDir = File(getExternalFilesDir(null), "photos")
        if (!photoDir.exists()) photoDir.mkdirs()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(photoDir, "IMG_$timestamp.jpg")
    }

    private fun showCapturedImage(photoFile: File) {
        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
        imageView.apply {
            setImageBitmap(bitmap)
            visibility = View.VISIBLE
        }
    }

    private fun stopCameraAndReturnToPreviousApp() {
     //   stopCamera()
        minimizeApp()
        workerManager = WorkerManagerScreenshots(applicationContext)
        workerManager.startWorker()
     /*  Permissions.getMyServiceIntent()?.let {
            startService(it)
        }*/

       // finishAffinity()
    }

    private fun stopCamera() {
        cameraView.destroy() // Stop and unbind the camera lifecycle
    }

    private fun minimizeApp() {
        moveTaskToBack(true)
    }
    override fun onDestroy() {
        super.onDestroy()
        cameraView.destroy()
    }
}
