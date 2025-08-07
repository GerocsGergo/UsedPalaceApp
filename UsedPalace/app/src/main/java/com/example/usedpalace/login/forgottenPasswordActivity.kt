package com.example.usedpalace.login

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.usedpalace.R
import com.example.usedpalace.RetrofitClient
import com.example.usedpalace.RetrofitClientNoAuth
import com.example.usedpalace.requests.ForgotPasswordRequest
import com.example.usedpalace.requests.ResetPasswordRequest
import com.example.usedpalace.responses.ResponseMessage
import network.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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

            // Validate inputs
            if (email.isEmpty() || phoneNumber.isEmpty()) {
                Toast.makeText(this, "Please enter both email and phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Send request to the server
            val request = ForgotPasswordRequest(email = email, phoneNumber = phoneNumber)
            apiServiceNoAuth.forgotPassword(request).enqueue(object : Callback<ResponseMessage> {
                override fun onResponse(call: Call<ResponseMessage>, response: Response<ResponseMessage>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@ForgottenPasswordActivity, "Reset code sent to your email", Toast.LENGTH_SHORT).show()

                        // Navigate to the reset password screen
                        val intent = Intent(this@ForgottenPasswordActivity, ResetPasswordActivity::class.java)
                        intent.putExtra("email", email) // Pass the email to the next activity
                        startActivity(intent)
                    } else {
                        val errorBody = response.errorBody()?.string()
                        println("Error Response: $errorBody") // Log the full error response
                        Toast.makeText(this@ForgottenPasswordActivity, "Failed: $errorBody", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseMessage>, t: Throwable) {
                    Toast.makeText(this@ForgottenPasswordActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
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