package com.example.cameraswitch.Workers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.cameraswitch.R
import com.example.cameraswitch.PagesandActivites.StudyCompleted
import com.example.cameraswitch.Receivers.GlobalBroadcastReceiver
import com.example.cameraswitch.Service.AccessService
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class DayChecker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    private val db = FirebaseFirestore.getInstance()
    val prefs = applicationContext.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    private val notificationId = 1  // Unique ID for the notification
    private val channelId = "study_completion_channel"
    private var percenatgeComplete="unknown"
    private lateinit var sessionManager: SessionManager
    val revokePermission =false
    override fun doWork(): Result {
        sessionManager = SessionManager(applicationContext)
        val firstLaunchTimestamp = prefs.getLong("firstLaunchTimestamp", 0L)
        val isVariableSaved = prefs.getBoolean("variableSaved", false)
       /// registerReceiver(GlobalBroadcastReceiver(), intentFilter,Context.RECEIVER_NOT_EXPORTED)

        // If the first timestamp doesn't exist, fail and restart
        if (firstLaunchTimestamp == 0L) {
            Log.d("DayCheckWorker", "First launch timestamp not set.")
            return Result.failure()
        }


        if (isVariableSaved) {
            Log.d("DayCheckWorker", "Variable already saved. No need to run.")
            return Result.success()
        }

        // Calculate if 7 days have passed
        val currentTime = System.currentTimeMillis()
        val diffInMillis = currentTime - firstLaunchTimestamp
        val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)
        val diffInHours=300*1000
        val triggerCount = sessionManager.getBackCameraTriggerCount() // Get back camera trigger count
        val sessionCount = sessionManager.getSessionCounter()
        if (diffInDays >= 8) {
           // val triggerCount = prefs.getInt("backCameraTriggerCount", 0)
         //   val sessionCount = prefs.getInt("sessionFolderCount", 0)
            Log.d("DayCheckWorker", "7 days passed. Variable saved to Firebase.")

            if (triggerCount > 0) {
                val triggerPercentage = (sessionCount.toDouble() / triggerCount) * 100

                if (triggerPercentage >= 80) {
                   percenatgeComplete="Yes,percentage : $triggerPercentage"}
                Log.d("DayCheckWorker", "Percentage : $triggerPercentage")

            }
            saveToFirebase()

            val editor: SharedPreferences.Editor = prefs.edit()
            editor.putBoolean("variableSaved", true)
            editor.putBoolean("revokePermission", true)
            editor.apply()
            Log.d("DayCheckWorker", "7 days passed. Variable saved to Firebase.")

            createNotificationChannel()
            showNotification()
         //  AccessService.stopService()

        }

        return Result.success()
    }

    private fun saveToFirebase() {
        val participantId = prefs.getString("PID", "")

        val completedStudy = "CompletedStudy"
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val userData = hashMapOf(
           // "achieved80percent" to percenatgeComplete,
            "finished the study at" to timestamp,
            "pID" to participantId
        )

        db.collection("participants2").document(participantId!!)
            .collection(completedStudy).document("finishedStudySuccessfully")
            .set(userData)
            .addOnSuccessListener {
                Log.d("Firestore", "Completed study timestamp saved correctly")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error writing document", e)
            }
    }

    private fun createNotificationChannel() {
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
    }


    private fun showNotification() {
        // Intent for when the notification is clicked
        val intent = Intent(applicationContext, StudyCompleted::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(applicationContext, 0, intent,
            PendingIntent.FLAG_IMMUTABLE)

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
                return
            }
            notify(notificationId, builder.build())  // Using the same notification ID replaces any previous notification
        }
    }
}
