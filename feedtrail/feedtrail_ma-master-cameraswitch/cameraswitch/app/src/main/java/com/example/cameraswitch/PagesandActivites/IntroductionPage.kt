package com.example.cameraswitch.PagesandActivites

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import com.example.cameraswitch.R

class IntroductionPage  : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.introduction)
        val scrollView = findViewById<ScrollView>(R.id.scrollviewintro)
        val acceptButton = findViewById<Button>(R.id.agree_button)

        scrollView.viewTreeObserver.addOnScrollChangedListener {
            // Check if the scrollView has reached the bottom
            if (scrollView.getChildAt(0).bottom <= (scrollView.height + scrollView.scrollY)) {
                // Enable the button when the user has scrolled to the bottom
                acceptButton.isEnabled = true
                acceptButton.alpha = 1.0f // Make the button fully opaque
            }
             if(acceptButton.isEnabled){
                 acceptButton.setOnClickListener {

                     val intent = Intent(this, EinvUlm::class.java)
                     startActivity(intent)
                 }
             }
        }

    }
}