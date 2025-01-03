package com.example.gps_image_notes

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find view by ID
        val buttonLogin = findViewById<Button>(R.id.login)

        // Animate/Rotate Icon
        val icon = findViewById<ImageView>(R.id.icon)

        // Select Icon & Button-Background depending on Mode
        val nightModeFlags = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            icon.setImageResource(R.drawable.ic_event_icon_night)
            buttonLogin.setBackgroundColor(resources.getColor(R.color.blue, theme))
        } else {
            icon.setImageResource(R.drawable.ic_event_icon)
            buttonLogin.setBackgroundColor(resources.getColor(R.color.glootie, theme))
        }

        // Smooth Infinite Rotation
        val rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_infinite)
        icon.startAnimation(rotateAnimation)

        // Set OnClickListener
        buttonLogin.setOnClickListener{
            // Show Signed-In Toast
            Toast.makeText(this, R.string.signed_in, Toast.LENGTH_LONG).show()

            // Create Intent and start activity (NoteListActivity)
            val intent = Intent(this, NoteListActivity::class.java)
            startActivity(intent)

            // Finish MainActivity
            finish()
        }
    }
}