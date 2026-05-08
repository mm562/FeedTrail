package com.example.cameraswitch.Receivers
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.cameraswitch.MainActivity
import com.example.cameraswitch.PagesandActivites.Permissions
import com.example.cameraswitch.Workers.EndStudyWorker
import java.util.concurrent.TimeUnit

class BootRceiver: BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action.equals(Intent.ACTION_BOOT_COMPLETED)) {

            /*   val launchIntent = Intent(context, MainActivity::class.java)
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)*/
            val launchIntent = Intent(context, Permissions::class.java).apply {
                putExtra("updateButton", true)
            }
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            context?.let {
                val prefs = it.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                val firstLaunchTimestamp = prefs.getLong("firstLaunchTimestamp", 0)

                if (firstLaunchTimestamp > 0) {
                    val delay = calculateDelayForEndStudy(it)
                    if (delay > 0) {
                        val workRequest = OneTimeWorkRequestBuilder<EndStudyWorker>()
                            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                            .build()

                        WorkManager.getInstance(context).enqueueUniqueWork(
                            "EndStudyWorker",  // Unique name to prevent duplicates
                            ExistingWorkPolicy.REPLACE,  // Ensures only one worker is scheduled
                            workRequest
                        )
                        /*
                        val workRequest = OneTimeWorkRequestBuilder<EndStudyWorker>()
                            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                            .build()
                        WorkManager.getInstance(it).enqueue(workRequest)*/

                        Log.d("RebootReceiver", "WorkManager rescheduled with delay: $delay ms")
                    }
                }

                //   Toast.makeText(context, "App started after reboot", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun calculateDelayForEndStudy(context: Context): Long {
        Log.d("EndStudy", "Calculating delay for WorkManager...")

        val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val firstLaunchTimestamp = prefs.getLong("firstLaunchTimestamp", 0)

        if (firstLaunchTimestamp == 0L) {
            Log.d("EndStudy", "First launch time is not set, returning 0 delay")
            return 0L // No delay if first launch time isn't set
        }
        val sevenDaysInMillis= 7 * 24 * 60 * 60 * 1000
      //  val sevenDaysInMillis = 1 * 60 * 1000L // Change to 7 * 24 * 60 * 60 * 1000L for 7 days
        val targetTime = firstLaunchTimestamp + sevenDaysInMillis
        val currentTime = System.currentTimeMillis()

        val delay = targetTime - currentTime // Calculate time remaining

        Log.d("EndStudy", "Target time: $targetTime, Current time: $currentTime, Delay: $delay ms")

        return if (delay > 0) delay else 0L // Ensure delay is never negative
    }

}