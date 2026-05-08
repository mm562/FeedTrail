package com.example.cameraswitch.PagesandActivites



import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.cameraswitch.MainViewModel
import com.example.cameraswitch.R
import com.example.cameraswitch.Workers.EndStudyWorker
import java.util.concurrent.TimeUnit

class StartStudyPage  : AppCompatActivity() {
    private lateinit var viewModel: MainViewModel
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.startedsuccessfully)

        onBackPressedDispatcher.addCallback(this) {
            // Do nothing or handle the back button press in some way
            // This prevents the default back behavior for api >33
        }

        fun onBackPressed() {
            // Leave this empty to disable the back button for api <  33
        }

       // viewModel = ViewModelProvider(this, ViewModelProvider.NewInstanceFactory()).get(MainViewModel::class.java)

        // Save the current stage as xxxxx
     //   viewModel.saveCurrentStage("Stage_2_startedsuccess")
        saveStageToPreferences("Stage_2_startedsuccess")

        val sevendayspassed= calculateDelayForEndStudy(applicationContext)
        val workRequest = OneTimeWorkRequestBuilder<EndStudyWorker>()
            // .setInitialDelay(7, TimeUnit.DAYS)
            .setInitialDelay(sevendayspassed, TimeUnit.MILLISECONDS)
            .build()
       // WorkManager.getInstance(applicationContext).enqueue(workRequest)
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "EndStudyWorker",  // Unique name to prevent duplicates
            ExistingWorkPolicy.REPLACE,  // Ensures only one worker is scheduled
            workRequest
        )
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
       // val sevenDaysInMillis = 1 * 60 * 1000L // Change to 7 * 24 * 60 * 60 * 1000L for 7 days
        val targetTime = firstLaunchTimestamp + sevenDaysInMillis
        val currentTime = System.currentTimeMillis()

        val delay = targetTime - currentTime // Calculate time remaining

        Log.d("EndStudy", "Target time: $targetTime, Current time: $currentTime, Delay: $delay ms")

        return if (delay > 0) delay else 0L // Ensure delay is never negative
    }

    private fun saveStageToPreferences(stage: String) {
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("last_stage", stage)
        editor.apply()
    }
}