package com.example.usedpalace.profileMenus

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.usedpalace.MainMenuActivity
import com.example.usedpalace.R

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val buttonBack: Button = findViewById(R.id.buttonBack)
        buttonBack.setOnClickListener {
            navigateBackToProfile()

        }
    }
    private fun navigateBackToProfile() {
        // Create an intent to return to MainMenuActivity
        val intent = Intent(this, MainMenuActivity::class.java).apply {
            // Add flag to indicate we want to show ProfileFragment
            putExtra("SHOW_PROFILE_FRAGMENT", true)
            // Clear the activity stack so we don't have multiple MainMenuActivities
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish() // Close the current SettingsActivity
    }



    // Handle system back button press
    override fun onBackPressed() {
        super.onBackPressed()
        navigateBackToProfile()
    }
}