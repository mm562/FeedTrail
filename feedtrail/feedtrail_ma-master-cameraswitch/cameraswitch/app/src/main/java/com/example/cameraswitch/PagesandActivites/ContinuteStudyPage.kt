package com.example.cameraswitch.PagesandActivites



import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.cameraswitch.MainViewModel
import com.example.cameraswitch.R

class ContinueStudyPage  : AppCompatActivity() {
    private lateinit var viewModel: MainViewModel
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.continuedsuccessfully)

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
    }

    private fun saveStageToPreferences(stage: String) {
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("last_stage", stage)
        editor.apply()
    }
}