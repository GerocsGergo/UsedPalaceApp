package com.example.usedpalace.loginMenus

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.usedpalace.ErrorHandler
import com.example.usedpalace.R
import network.RetrofitClient
import network.RetrofitClientNoAuth
import com.example.usedpalace.requests.ForgotPasswordRequest
import com.example.usedpalace.responses.ResponseMessage
import network.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ForgottenPasswordActivity : AppCompatActivity() {
    private lateinit var apiServiceNoAuth: ApiService
    private lateinit var prefs: SharedPreferences

    private lateinit var inputEmail: EditText
    private lateinit var inputPhoneNumber: EditText

    private lateinit var buttonBack :Button
    private lateinit var buttonSendCode :Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgotten_password)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupViews()
        initialize()
        setupClickListeners()

    }


    private fun setupClickListeners() {
        buttonBack.setOnClickListener {
            val intent = Intent(this, LogActivity::class.java)
            startActivity(intent)
        }

        // Handle "Send Code" button click
        buttonSendCode.setOnClickListener {
            val email = inputEmail.text.toString().trim()
            val phoneNumber = inputPhoneNumber.text.toString().trim()

            if (!phoneNumber.matches(Regex("^06\\d{9}$"))) {
                ErrorHandler.toaster(this, "Helytelen telefonszám formátum")
                return@setOnClickListener
            }

            // Validate inputs
            if (email.isEmpty() || phoneNumber.isEmpty()) {
                 ErrorHandler.toaster(this, "Kérjük töltsön ki minden mezőt")
                return@setOnClickListener
            }

            // Send request to the server
            val request = ForgotPasswordRequest(email = email, phoneNumber = phoneNumber)
            apiServiceNoAuth.forgotPassword(request).enqueue(object : Callback<ResponseMessage> {
                override fun onResponse(call: Call<ResponseMessage>, response: Response<ResponseMessage>) {
                    if (response.isSuccessful) {
                        ErrorHandler.toaster(this@ForgottenPasswordActivity, "A kódot elküldtük az e-mail címére.")
                        // Navigate to the reset password screen
                        val intent = Intent(this@ForgottenPasswordActivity, ResetPasswordActivity::class.java)
                        intent.putExtra("email", email) // Pass the email to the next activity
                        startActivity(intent)
                    } else {
                        val errorBody = response.errorBody()?.string()
                        ErrorHandler.handleApiError(this@ForgottenPasswordActivity, null, errorBody)
                        }
                }

                override fun onFailure(call: Call<ResponseMessage>, t: Throwable) {
                     ErrorHandler.handleNetworkError(this@ForgottenPasswordActivity,t)
                }
            })
        }
    }

    private fun setupViews() {

        inputEmail = findViewById(R.id.inputEmail)
        inputPhoneNumber = findViewById(R.id.inputPhoneNumber)
        buttonSendCode = findViewById(R.id.buttonSubmit)


        buttonBack = findViewById(R.id.buttonBack)
    }

    private fun initialize() {
        prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        RetrofitClient.init(applicationContext)
        apiServiceNoAuth = RetrofitClientNoAuth.apiService
    }
}