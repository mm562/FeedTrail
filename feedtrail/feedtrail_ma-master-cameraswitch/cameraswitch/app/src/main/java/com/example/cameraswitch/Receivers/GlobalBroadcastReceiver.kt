package com.example.cameraswitch.Receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.cameraswitch.Capturing.BackCameraButton
import com.example.cameraswitch.Capturing.CombineCameras
import com.example.cameraswitch.Capturing.HideCamera
import com.example.cameraswitch.Capturing.ScreenCaptureService
import com.example.cameraswitch.MainActivity

class GlobalBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent?.action == "com.example.cameraswitch.UPLOAD_TRIGGER") {
            Log.d("UploadFromTemp", "BroadcastReceiver upload trigger received")
        /*    val cameraSource = intent.getStringExtra("cameraSource")
            if (cameraSource == "BACK") {
                Log.d("Upload", "global broadcast back")
                BackCameraButton.triggerUpload(context)
            } else if (cameraSource == "FRONT") {
                Log.d("Upload", "global broadcast front")

                HideCamera.triggerUpload(context)
            }else if(cameraSource=="SCREENSHOT"){
                ScreenCaptureService.triggerUpload(context)

            } else {
                Log.e("UploadFromTemp", "Unknown camera source in broadcast: $cameraSource")
            }*/
          //  CombineCameras.triggerUpload(context)
            MainActivity.triggerUpload(context)
        } else if (context != null && intent?.action == "com.example.cameraswitch.UPLOAD_TRIGGER_CANCEL") {

        } else{
            Log.e("UploadFromTemp", "Context or intent was null, broadcast not handled")
        }
    }
}
