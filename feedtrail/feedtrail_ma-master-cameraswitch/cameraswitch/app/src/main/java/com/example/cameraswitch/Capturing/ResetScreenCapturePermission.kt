package com.example.cameraswitch.Capturing

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.cameraswitch.PagesandActivites.Permissions
import com.example.cameraswitch.R

class ResetScreenCapturePermission : AppCompatActivity() {

    private val PERMISSION_SCREENSHOT_CODE = 104

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Optionally set a simple layout or nothing at all

        val mediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(captureIntent, PERMISSION_SCREENSHOT_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PERMISSION_SCREENSHOT_CODE && resultCode == RESULT_OK && data != null) {
            // You now have a valid MediaProjection token.
            // You can pass it on to your ScreenCaptureService or store it in a singleton/global variable.
            Permissions.setMyServiceIntent(data)
        } else {
            // Handle the case where permission was denied.
        }
        // Close this Activity after handling the result.
        finish()
        minimizeApp()
    }
  /*override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
      super.onActivityResult(requestCode, resultCode, data)
      if (requestCode == PERMISSION_SCREENSHOT_CODE && resultCode == RESULT_OK && data != null) {
          // Save or use the MediaProjection token immediately
          Permissions.setMyServiceIntent(data)
          // Now start the screen capture service
          val serviceIntent = ScreenCaptureService.getStartIntent(this, resultCode, data)
         startForegroundService(serviceIntent)
      } else {
          // Handle the case where permission was denied.
          val permissionIntent = Intent(applicationContext, ResetScreenCapturePermission::class.java)
          val pendingIntent = PendingIntent.getActivity(
              applicationContext,
              0,
              permissionIntent,
              PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
          )
          val notification = NotificationCompat.Builder(applicationContext, "com.example.screencap")
              .setSmallIcon(R.drawable.ic_notification)
              .setContentTitle("Screen Capture Permission Required")
              .setContentText("Tap here to grant permission for screen capture")
              .setPriority(NotificationCompat.PRIORITY_HIGH)
              .setAutoCancel(true)
              .setContentIntent(pendingIntent)
              .build()

          if (ActivityCompat.checkSelfPermission(
                  this,
                  Manifest.permission.POST_NOTIFICATIONS
              ) != PackageManager.PERMISSION_GRANTED
          ) {
              // TODO: Consider calling
              //    ActivityCompat#requestPermissions
              // here to request the missing permissions, and then overriding
              //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
              //                                          int[] grantResults)
              // to handle the case where the user grants the permission. See the documentation
              // for ActivityCompat#requestPermissions for more details.
              return
          }
          NotificationManagerCompat.from(applicationContext).notify(1337, notification)
      }
      finish() // Close the permission Activity
      minimizeApp()
  }*/

    private fun minimizeApp() {
        moveTaskToBack(true)
    }

}
