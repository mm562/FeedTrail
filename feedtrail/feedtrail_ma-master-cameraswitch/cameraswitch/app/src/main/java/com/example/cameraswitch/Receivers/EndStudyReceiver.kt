package com.example.cameraswitch.Receivers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.cameraswitch.Capturing.BackCameraButton
import com.example.cameraswitch.Capturing.CombineCameras
import com.example.cameraswitch.Capturing.HideCamera
import com.example.cameraswitch.Capturing.ScreenCaptureService
import com.example.cameraswitch.MainActivity
import com.example.cameraswitch.PagesandActivites.StudyCompleted
import com.example.cameraswitch.R
import com.example.cameraswitch.Service.AccessService
import com.example.cameraswitch.UtilsAndCons.SharedPreferencesUtils
import com.example.cameraswitch.Workers.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EndStudyReceiver : BroadcastReceiver() {
    private val db = FirebaseFirestore.getInstance()
    private val notificationId = 1337  // Unique ID for the notification
    private val channelId = "study_completion_channel"
    private var percenatgeComplete = "unknown"
    private lateinit var sessionManager: SessionManager
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("EndStudy", "In the end study receiver")
     /*   val participantId = SharedPreferencesUtils.getParticipantId(context!!)
        val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

        if (context != null) {
            var lastday = AccessService.getCurrentDay(context)
         //   if (lastday > 7 || isSevenDaysPassed(context)) {
          //  if (isSevenDaysPassed(context) && !prefs.getBoolean("logicExecuted", false)) {
            if (!prefs.getBoolean("logicExecuted", false)) {

               // sendEndStudyBroadcast(context)

                /**Notification**/
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val name = "Study Completion"
                    val descriptionText = "Notification for study completion after 7 days"
                    val importance = NotificationManager.IMPORTANCE_HIGH
                    val channel = NotificationChannel(channelId, name, importance).apply {
                        description = descriptionText
                    }

                    val notificationManager: NotificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.createNotificationChannel(channel)
                }
                val intent = Intent(context, StudyCompleted::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntent: PendingIntent = PendingIntent.getActivity(
                    context, 0, intent,
                    PendingIntent.FLAG_IMMUTABLE
                )

                // Build the notification
                val builder = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_notification)  // Replace with your own icon
                    .setContentTitle("Study Completed")
                    .setContentText("Congratulations! You have completed the 7-day study.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)

                // Show the notification
                with(NotificationManagerCompat.from(context)) {
                    if (ActivityCompat.checkSelfPermission(
                            context,
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
                    notify(
                        notificationId,
                        builder.build()
                    )  // Using the same notification ID replaces any previous notification
                }
            }
        }

        /**Save to firebase**/

        val completedStudy = "CompletedStudy"
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val userData = hashMapOf(
       //     "achieved80percent" to percenatgeComplete,
            "finished the study at" to timestamp,
            "pID" to participantId
        )

        db.collection("participants").document(participantId!!)
            .collection(completedStudy).document("finishedStudySuccessfully")
            .set(userData)
            .addOnSuccessListener {
                Log.d("Firestore", "Completed study timestamp saved correctly")
                prefs.edit().putBoolean("logicExecuted", true).apply()
                Log.d("EndStudy", "log should be added in firebase now, logic executed : ${prefs.getBoolean("logicExecuted",false)}")


            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error writing document", e)
            }
    }



    private fun sendEndStudyBroadcast(context: Context?) {
        val intent = Intent("com.example.app.Endstudy") // Custom action
        context?.sendBroadcast(intent) // Sending broadcast
    }




    fun isSevenDaysPassed(context: Context): Boolean {
        Log.d("EndStudy", "isSevendayspassed")

        val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val firstLaunchTimestamp = prefs.getLong("firstLaunchTimestamp", 0)

        if (firstLaunchTimestamp == 0L) {
            Log.d("EndStudy", "First launch time is not set")

            return false // First launch time is not set
        }

      //  val sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000 // 7 days in milliseconds
        val sevenDaysInMillis= 3 * 60 * 1000L
        val targetTime = firstLaunchTimestamp + sevenDaysInMillis
        val currentTime = System.currentTimeMillis()

        return currentTime >= targetTime // If current time is past the target time
    }

*/}
}
