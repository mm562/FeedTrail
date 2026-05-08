package com.example.cameraswitch.Workers


import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.cameraswitch.PagesandActivites.Permissions
import com.example.cameraswitch.R
import com.example.cameraswitch.UtilsAndCons.PermissionsUtils

class PermissionCheckWorker(appContext: Context, workerParams: WorkerParameters)
    : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        // Use PermissionsUtils to check if all permissions are granted
        if (!PermissionsUtils.areAllPermissionsGranted(applicationContext)) {
            showMissingPermissionNotification()

        }else{
            NotificationManagerCompat.from(applicationContext).cancel(1337)




        }
        return Result.success()
    }

    private fun showMissingPermissionNotification() {
        val intent = Intent(applicationContext, Permissions::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(applicationContext, "permissions_channel")
            .setSmallIcon(R.drawable.logo)
            .setContentTitle("Permission Required")
            .setContentText("Please grant the missing permissions.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setContentIntent(pendingIntent)

        if (PermissionsUtils.areNotificationsEnabled(applicationContext)) {
            if (ActivityCompat.checkSelfPermission(
                        applicationContext,
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
            NotificationManagerCompat.from(applicationContext).notify(1337, builder.build())
        }
    }

    /*private fun showStudyRunning(){
        val builder = NotificationCompat.Builder(this, "start_channel") // Use consistent channel ID
            .setSmallIcon(R.drawable.logo) // Ensure this drawable exists
            .setContentTitle("Study Running")
            .setContentText("Thank you for participating in our study!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            return
        }
        NotificationManagerCompat.from(this).notify(1, builder.build())
    }*/
}
