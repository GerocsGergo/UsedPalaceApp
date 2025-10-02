package com.example.usedpalace.adminMenus

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.usedpalace.MainMenuActivity
import com.example.usedpalace.R
import com.example.usedpalace.loginMenus.LogActivity
import com.example.usedpalace.profileMenus.AboutActivity
import com.example.usedpalace.profileMenus.CreateSaleActivity
import com.example.usedpalace.profileMenus.HelpActivity
import com.example.usedpalace.profileMenus.InformationActivity
import com.example.usedpalace.profileMenus.SupportActivity
import com.example.usedpalace.profileMenus.ownSalesActivity.OwnSalesActivity
import com.example.usedpalace.profileMenus.profileActivity.ProfileActivity
import com.google.android.material.textfield.TextInputLayout

class AdminMainMenu : AppCompatActivity() {

    private lateinit var buttonBack : Button
    private lateinit var usersMenu : Button
    private lateinit var salesMenu : Button
    private lateinit var statsMenu : Button
    private lateinit var checkMenu : Button



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_main_menu)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupViews()
        setupClickListeners()
    }

    private fun setupViews() {

        buttonBack = findViewById(R.id.buttonBack)
        usersMenu = findViewById(R.id.usersMenu)
        salesMenu = findViewById(R.id.salesMenu)
        statsMenu = findViewById(R.id.statsMenu)
        checkMenu = findViewById(R.id.checkMenu)
    }

    private fun setupClickListeners() {
        buttonBack.setOnClickListener {
            navigateBackToProfile()
        }
        usersMenu.setOnClickListener {
            startActivity(Intent(this,UsersMenu::class.java))
        }
        salesMenu.setOnClickListener {
            startActivity(Intent(this,UsersMenu::class.java))
        }
        statsMenu.setOnClickListener {
            startActivity(Intent(this,UsersMenu::class.java))
        }
        checkMenu.setOnClickListener {
            startActivity(Intent(this,UsersMenu::class.java))
        }

    }

    private fun navigateBackToProfile() {
        val intent = Intent(this, MainMenuActivity::class.java).apply {
            putExtra("SHOW_PROFILE_FRAGMENT", true)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        navigateBackToProfile()
    }
}