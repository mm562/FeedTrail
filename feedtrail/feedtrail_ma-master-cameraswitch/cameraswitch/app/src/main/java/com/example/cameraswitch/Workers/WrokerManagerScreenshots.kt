package com.example.cameraswitch.Workers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

class WorkerManagerScreenshots(private val context: Context) {
    private var captureWorkRequest: PeriodicWorkRequest? = null

    fun prepareWorker(resultCode: Int, dataString: Intent?) {


            val inputData = Data.Builder()
                .putInt("RESULT_CODE", resultCode)
                .putString("DATA", dataString.toString())
                .build()

            captureWorkRequest = PeriodicWorkRequestBuilder<ScreenCaptureWorker>(5, TimeUnit.SECONDS)
                .setInputData(inputData)
                .addTag("ScreenCaptureWorker")
                .build()

            Log.d("DebugWorker", "Worker prepared with input data but not started.")
        }

        // Start the worker
        fun startWorker() {

             captureWorkRequest?.let {
                 WorkManager.getInstance(context).enqueue(it)
                 Log.d("WorkerManagerScreenshots", "ScreenCaptureWorker started.")
             } ?: Log.e("WorkerManagerScreenshots", "Worker not prepared. Call prepareWorker first.")

               if (captureWorkRequest == null) {
                   Log.e("WorkerManagerScreenshots", "startWorker called but worker is not prepared. Call prepareWorker first.")
                   return
               }

               WorkManager.getInstance(context).enqueue(captureWorkRequest!!)
               Log.d("WorkerManagerScreenshots", "ScreenCaptureWorker started.")

            /*
            val sharedPreferences = context.getSharedPreferences("PREFS_NAME", Context.MODE_PRIVATE)

            val resultCode = sharedPreferences.getInt("KEY_SCREEN_CAPTURE_PERMISSION", Activity.RESULT_CANCELED)
            val dataUriString = sharedPreferences.getString("KEY_SCREEN_CAPTURE_DATA_URI", null)

            val inputData = Data.Builder()
                .putInt("RESULT_CODE", resultCode)
                .putString("DATA", dataUriString)
                .build()
            Log.d("DebugWorker", "WorkerManager data : resultcode: $resultCode, data: $dataUriString")

            val captureWorkRequest = PeriodicWorkRequestBuilder<ScreenCaptureWorker>(5, TimeUnit.SECONDS)
                .setInputData(inputData)
                .addTag("ScreenCaptureWorker")
                .build()

            WorkManager.getInstance(context).enqueue(captureWorkRequest!!)
            Log.d("DebugWorker", "Worker starter in Manager")*/


        }

    // Function to stop the worker (and its projection)
     fun stopWorker() {
        // Cancel the worker by its tag
        WorkManager.getInstance(context).cancelAllWorkByTag("ScreenCaptureWorker")
        Log.d("WorkerManager", "ScreenCaptureWorker stopped.")

    }
}
