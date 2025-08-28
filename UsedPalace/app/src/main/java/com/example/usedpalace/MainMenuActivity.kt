package com.example.usedpalace

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.usedpalace.databinding.MainMenuBinding
import com.example.usedpalace.fragments.ProfileFragment
import com.example.usedpalace.loginMenus.LogActivity
import com.example.usedpalace.requests.SaveFcmTokenRequest
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MainMenuActivity : AppCompatActivity() {
    private  lateinit var binding: MainMenuBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        //ez lett modositva
        binding = MainMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val navView:BottomNavigationView = binding.navView
        val navController = findNavController(R.id.fragmentContainerView)
        navView.setupWithNavController(navController)

        // Check if we're coming from profile activities to show ProfileFragment
        if (intent?.getBooleanExtra("SHOW_PROFILE_FRAGMENT", false) == true) {
            showProfileFragment()
        }




        // Back button dispatcher
        onBackPressedDispatcher.addCallback(this) {

            if (!navController.popBackStack()) {
                val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                val token = prefs.getString("token", null)

                val intent = Intent(this@MainMenuActivity, LogActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

    }

    // Function to show ProfileFragment
    private fun showProfileFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView, ProfileFragment())
            .addToBackStack("profile") // Add to back stack with a tag
            .commit()
    }


}