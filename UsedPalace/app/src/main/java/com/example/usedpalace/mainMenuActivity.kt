package com.example.usedpalace

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.usedpalace.databinding.MainMenuBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

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

        // Check if we're coming from SettingsActivity to show ProfileFragment
        if (intent?.getBooleanExtra("SHOW_PROFILE_FRAGMENT", false) == true) {
            showProfileFragment()
        }
    }

    // Function to show ProfileFragment
    fun showProfileFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView, ProfileFragment())
            .addToBackStack("profile") // Add to back stack with a tag
            .commit()
    }

}