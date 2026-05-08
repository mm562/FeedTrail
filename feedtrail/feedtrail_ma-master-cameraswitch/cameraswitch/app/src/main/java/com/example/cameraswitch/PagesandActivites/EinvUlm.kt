package com.example.cameraswitch.PagesandActivites

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import com.example.cameraswitch.R

class EinvUlm : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.uulm)
        val scrollView = findViewById<ScrollView>(R.id.scrollulm)
        val accept = findViewById<Button>(R.id.accept)
        scrollView.viewTreeObserver.addOnScrollChangedListener {
            if (scrollView.getChildAt(0).bottom <= (scrollView.height + scrollView.scrollY)) {
                accept.isEnabled = true

            }
            if(accept.isEnabled){
                accept.setOnClickListener {

                    val intent = Intent(this, Permissions::class.java)
                    startActivity(intent)
                }
            }
        }


    }
}