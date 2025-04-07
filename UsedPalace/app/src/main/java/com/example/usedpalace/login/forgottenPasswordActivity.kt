package com.example.usedpalace.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.usedpalace.R
import com.example.usedpalace.requests.ForgotPasswordRequest
import com.example.usedpalace.responses.ResponseMessage
import network.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ForgottenPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.forgotten_password)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val buttonBack: Button = findViewById(R.id.buttonBack)
        buttonBack.setOnClickListener {
            val intent = Intent(this, LogActivity::class.java)
            startActivity(intent)
        }

        // Initialize Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/") // Use 10.0.2.2 for localhost in Android emulator
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        // Find views
        val inputEmail = findViewById<EditText>(R.id.inputEmail)
        val inputPhoneNumber = findViewById<EditText>(R.id.inputPhoneNumber)
        val buttonSendCode = findViewById<Button>(R.id.buttonSubmit)

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
            apiService.forgotPassword(request).enqueue(object : Callback<ResponseMessage> {
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
}