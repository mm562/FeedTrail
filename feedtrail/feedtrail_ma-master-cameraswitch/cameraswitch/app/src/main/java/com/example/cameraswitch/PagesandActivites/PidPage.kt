package com.example.cameraswitch.PagesandActivites

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.cameraswitch.R
import com.example.cameraswitch.Workers.DayChecker
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.StorageReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class PidPage  : AppCompatActivity() {
    private lateinit var storageRef: StorageReference
    private val db = FirebaseFirestore.getInstance()
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.enterpid)
        val pidInput = findViewById<EditText>(R.id.pid_input)
        val submit = findViewById<Button>(R.id.submit)
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

        submit.setOnClickListener {

            val intent = Intent(this, IntroductionPage::class.java)
            startActivity(intent)
          //  storeInitialTimestamp()
            //scheduleDayCheckWorker()
        }
        pidInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not needed
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Enable the button if the EditText is not empty
                submit.isEnabled = s.toString().trim().isNotEmpty()
                //submit.alpha = if (submitButton.isEnabled) 1.0f else 0.5f
            }

            override fun afterTextChanged(s: Editable?) {
                // Not needed
            }
        })
        submit.setOnClickListener {
            Log.d("PIDPage", "Submit button clicked")

            val enteredPid = pidInput.text.toString().trim()
            if (enteredPid.isNotEmpty()) {
                Log.d("PIDPage", "Entered PID: $enteredPid")

                // Save PID to SharedPreferences
                with(sharedPreferences.edit()) {
                    putString("PID", enteredPid)
                    apply()
                }

                // Log and call recordFirstTimeEntry
                Log.d("PIDPage", "PID saved to SharedPreferences")
                storeInitialTimestamp()
                scheduleDayCheckWorker()
                recordFirstTimeEntry()

                // Navigate to the next activity
                val intent = Intent(this, IntroductionPage::class.java)
                startActivity(intent)
            } else {
                Log.d("PIDPage", "Entered PID is empty")
            }
        }

    }
    private fun scheduleDayCheckWorker() {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        // Only schedule the worker if the variable hasn't already been saved
        if (!prefs.getBoolean("variableSaved", false)) {
            val workRequest = OneTimeWorkRequestBuilder<DayChecker>()
                .setInitialDelay(8, TimeUnit.DAYS)  // Run after 7 days
                .build()

            WorkManager.getInstance(this).enqueue(workRequest)
            Log.d("PID", "DayCheckWorker scheduled to run after 7 days.")
        } else {
            Log.d("PID", "Variable already saved. No need to schedule worker.")
        }
    }
    private fun storeInitialTimestamp() {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        if (!prefs.contains("firstLaunchTimestamp")) {
            val editor = prefs.edit()
            editor.putLong("firstLaunchTimestamp", System.currentTimeMillis())
            editor.apply()
        }
        Log.d("PID", "saved : ${prefs.getLong("firstLaunchTimestamp",0).toString()}")

    }
    private fun recordFirstTimeEntry() {
        Log.d("FirebaseVar", "saving")
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val participantId = sharedPreferences.getString("PID", null)
        Log.d("PIDPage", "$participantId")
        if (participantId.isNullOrEmpty()) {
            Log.e("Firestore", "Participant ID is null or empty.")
            return // Exit if the participant ID is missing
        }

        //val participantId = "p_ID"  // This should be dynamically set based on user input
        val startedStudy = "StartedStudy"
        val firstLaunchTimestamp_formatted=getFirstLaunchTimestampFormatted(this)

        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)


        val userData = hashMapOf(
            "started the study at" to firstLaunchTimestamp_formatted,
            "pID" to participantId,
            "androidID" to androidId,
            "SocialMediaPrediction App" to packageName,

            )
        db.collection("participants2").document(participantId!!)
            .collection(startedStudy).document("startedStudySuccessfuly")
            .set(userData)
            .addOnSuccessListener {
                Log.d("Firestore", "StartedApp Timestamp saved correctly")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error writing document for first timestamp", e)
            }
    }

    fun getFirstLaunchTimestampFormatted(context: Context): String {
        val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val firstLaunchTimestamp = prefs.getLong("firstLaunchTimestamp", 0L)
        if (firstLaunchTimestamp == 0L) {
            return "Not set" // Handle the case where timestamp is not set
        }

        // Format the timestamp
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return formatter.format(Date(firstLaunchTimestamp))
    }


}