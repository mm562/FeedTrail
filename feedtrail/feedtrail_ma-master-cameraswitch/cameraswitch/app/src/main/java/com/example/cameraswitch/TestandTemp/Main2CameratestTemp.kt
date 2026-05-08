package com.example.cameraswitch.TestandTemp



import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.cameraswitch.R

class Main2CameratestTemp : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Launch CameraActivity when the app starts
        val intent = Intent(this, CameraAlterTestTemp::class.java)
        startActivity(intent)

        // Close MainActivity since it's not needed
        finish()
    }
}
