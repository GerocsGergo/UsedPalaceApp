package com.example.usedpalace.profileMenus.profileActivity

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.usedpalace.ErrorHandler
import com.example.usedpalace.MainMenuActivity
import com.example.usedpalace.R
import network.RetrofitClient
import com.example.usedpalace.UserSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.ApiService

class ProfileActivity : AppCompatActivity() {

    private lateinit var buttonBack: Button
    private lateinit var buttonModifyPhone: Button
    private lateinit var buttonModifyEmail: Button
    private lateinit var buttonModifyPassword: Button
    private lateinit var buttonDeleteUser: Button

    private lateinit var userFullname: TextView
    private lateinit var userEmail: TextView
    private lateinit var userPhone: TextView


    private lateinit var apiService: ApiService
    private lateinit var prefs: SharedPreferences

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
        loadUserData(UserSession.getUserId()!!)

    }


    private fun setupViewItems(){
        buttonBack = findViewById(R.id.buttonBack)
        buttonModifyPhone = findViewById(R.id.modifyPhone)
        buttonModifyEmail = findViewById(R.id.modifyEmail)
        buttonModifyPassword = findViewById(R.id.modifyPassword)
        buttonDeleteUser = findViewById(R.id.deleteUser)

        userFullname = findViewById(R.id.userFullname)
        userEmail = findViewById(R.id.userEmail)
        userPhone = findViewById(R.id.userPhone)
    }

    private fun loadUserData(userID: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.getSafeUserData(mapOf("userID" to userID))

                withContext(Dispatchers.Main) {
                    if (response.success && response.data != null) {
                        val user = response.data
                        userFullname.text = "Név: ${user.fullname}"
                        userEmail.text = "Email: ${user.email}"
                        userPhone.text = "Telefon: ${user.phoneNumber}"
                    } else {
                        ErrorHandler.handleApiError(this@ProfileActivity, null, response.message)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    ErrorHandler.handleNetworkError(this@ProfileActivity, e)
                }
            }
        }
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
        buttonDeleteUser.setOnClickListener {
            val intent = Intent(this, DeleteAccountActivity::class.java).apply {
            }
            startActivity(intent)
            finish()
        }

    }

    private fun setupRetrofit(){
        prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        RetrofitClient.init(applicationContext)
        apiService = RetrofitClient.apiService
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