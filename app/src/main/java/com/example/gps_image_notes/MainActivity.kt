package com.example.gps_image_notes

import android.animation.ObjectAnimator
import android.content.Intent
import android.media.Image
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find view by ID
        val buttonLogin = findViewById<Button>(R.id.login)

        // Animate/Rotate Icon
        val icon = findViewById<ImageView>(R.id.icon)

        // Rotate Once
//        val rotateAnimator = ObjectAnimator.ofFloat(icon, "rotation", 0f, 360f)
//        rotateAnimator.duration = 1000
//        rotateAnimator.start()

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