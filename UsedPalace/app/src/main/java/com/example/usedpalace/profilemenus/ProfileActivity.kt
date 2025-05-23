package com.example.usedpalace.profilemenus

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.usedpalace.MainMenuActivity
import com.example.usedpalace.R
import com.example.usedpalace.profilemenus.forprofileactivity.ModifyEmailActivity
import com.example.usedpalace.profilemenus.forprofileactivity.ModifyPasswordActivity
import com.example.usedpalace.profilemenus.forprofileactivity.ModifyPhoneActivity
import network.ApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ProfileActivity : AppCompatActivity() {

    private lateinit var buttonBack: Button
    private lateinit var buttonModifyPhone: Button
    private lateinit var buttonModifyEmail: Button
    private lateinit var buttonModifyPassword: Button

    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        setupViewItems()
        setupRetrofit()
        setupClickListeners()

    }


    private fun setupViewItems(){
        buttonBack = findViewById(R.id.buttonBack)
        buttonModifyPhone = findViewById(R.id.modifyPhone)
        buttonModifyEmail = findViewById(R.id.modifyEmail)
        buttonModifyPassword = findViewById(R.id.modifyPassword)
    }

    private fun setupClickListeners(){
        buttonBack.setOnClickListener {
            navigateBackToProfile()

        }
        buttonModifyPhone.setOnClickListener {
            val intent = Intent(this, ModifyPhoneActivity::class.java).apply {
            }
            startActivity(intent)
            finish()

        }
        buttonModifyEmail.setOnClickListener {
            val intent = Intent(this, ModifyEmailActivity::class.java).apply {
            }
            startActivity(intent)
            finish()

        }
        buttonModifyPassword.setOnClickListener {
            val intent = Intent(this, ModifyPasswordActivity::class.java).apply {
            }
            startActivity(intent)
            finish()

        }
    }

    private fun setupRetrofit(){
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)
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