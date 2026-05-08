package com.example.cameraswitch.Workers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.cameraswitch.PagesandActivites.StudyCompleted
import com.example.cameraswitch.R
import com.example.cameraswitch.Service.AccessService
import com.example.cameraswitch.UtilsAndCons.SharedPreferencesUtils
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EndStudyWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    private val db = FirebaseFirestore.getInstance()
    private val notificationId = 1337  // Unique ID for the notification
    private val channelId = "study_completion_channel"
    private var percenatgeComplete = "unknown"
    private lateinit var sessionManager: SessionManager
    override fun doWork(): Result {
        Log.d("EndStudy", "In the end study worker")
        val participantId = SharedPreferencesUtils.getParticipantId(applicationContext!!)
        val prefs = applicationContext.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

        if (applicationContext != null) {
            var lastday = AccessService.getCurrentDay(applicationContext)
            //   if (lastday > 7 || isSevenDaysPassed(context)) {
            //  if (isSevenDaysPassed(context) && !prefs.getBoolean("logicExecuted", false)) {
            if (!prefs.getBoolean("logicExecuted", false)) {
                prefs.edit().putBoolean("endStudyReceived", true).apply()
                 sendEndStudyBroadcast(applicationContext)

                /**Notification**/
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val name = "Study Completion"
                    val descriptionText = "Notification for study completion after 7 days"
                    val importance = NotificationManager.IMPORTANCE_HIGH
                    val channel = NotificationChannel(channelId, name, importance).apply {
                        description = descriptionText
                    }

                    val notificationManager: NotificationManager =
                        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.createNotificationChannel(channel)
                }
                val intent = Intent(applicationContext, StudyCompleted::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntent: PendingIntent = PendingIntent.getActivity(
                    applicationContext, 0, intent,
                    PendingIntent.FLAG_IMMUTABLE
                )

                // Build the notification
                val builder = NotificationCompat.Builder(applicationContext, channelId)
                    .setSmallIcon(R.drawable.ic_notification)  // Replace with your own icon
                    .setContentTitle("Study Completed")
                    .setContentText("Congratulations! You have completed the 7-day study.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)

                // Show the notification
                with(NotificationManagerCompat.from(applicationContext)) {
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
                        return@with
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

        db.collection("participants2").document(participantId!!)
            .collection(completedStudy).document("finishedStudySuccessfully")
            .set(userData)
            .addOnSuccessListener {
                Log.d("Firestore", "Completed study timestamp saved correctly")
                prefs.edit().putBoolean("logicExecuted", true).apply()
                Log.d(
                    "EndStudy",
                    "log should be added in firebase now, logic executed : ${
                        prefs.getBoolean(
                            "logicExecuted",
                            false
                        )
                    }"
                )


            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error writing document", e)
            }
        return Result.success()
    }
    private fun sendEndStudyBroadcast(context: Context?) {
        val intent = Intent("com.example.app.Endstudy") // Custom action
        context?.sendBroadcast(intent) // Sending broadcast
    }

}

