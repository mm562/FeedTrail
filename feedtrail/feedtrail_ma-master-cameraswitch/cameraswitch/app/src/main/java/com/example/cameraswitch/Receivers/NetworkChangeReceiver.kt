package com.example.cameraswitch.Receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.cameraswitch.Capturing.BackCameraButton
import com.example.cameraswitch.Capturing.CombineCameras
import com.example.cameraswitch.Capturing.HideCamera
import com.example.cameraswitch.Capturing.ScreenCaptureService
import com.example.cameraswitch.Workers.UploadToFirebaseWorker
import java.io.File

class NetworkChangeReceiver : BroadcastReceiver() {

    companion object {
      public  var wasmobiledataon = false
        public  var wifiafterdata = false
        public  var wifi = false


    }


    private fun uploadPendingImages(context: Context) {
        val tempDir = File(context.filesDir, "temp_images")
        if (tempDir.exists()) {
            val files = tempDir.listFiles()
            files?.forEach { file ->
                // Upload each file to Firebase
            //    BackCameraButton.uploadImageToFirebase(context, file, null)
            //    HideCamera.uploadImageToFirebase(context, file, null)
             //   ScreenCaptureService.uploadScreenshot(context,file)

            }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("NetworkChangeReceiver", "Network Receiver")

        if (context == null || intent == null) return

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        val isWifiConnected = networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isMobileDataConnected = networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        if(isWifiConnected){
            wifi=true

            if(wasmobiledataon){
                val workRequest = OneTimeWorkRequestBuilder<UploadToFirebaseWorker>().build()
                WorkManager.getInstance(context).enqueue(workRequest)
            }


            wasmobiledataon=false
          //  BackCameraButton.triggerUpload(context)
           // HideCamera.triggerUpload(context)
            //ScreenCaptureService.triggerUpload(context)
        }
        if(isMobileDataConnected){
            wasmobiledataon=true
            wifi=false

        }

    }
}
