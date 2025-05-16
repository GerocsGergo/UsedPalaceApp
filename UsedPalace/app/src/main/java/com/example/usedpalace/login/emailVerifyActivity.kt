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
import com.example.usedpalace.requests.EmailVerificationWithCodeRequest
import com.example.usedpalace.responses.ResponseMessage
import network.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class EmailVerifyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.email_verify)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get the email from the previous activity
        val email = intent.getStringExtra("email") ?: ""

        // Initialize Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/") // Use 10.0.2.2 for localhost in Android emulator
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        // Find views
        val inputCode = findViewById<EditText>(R.id.inputCode)
        val buttonVerifyEmail = findViewById<Button>(R.id.buttonSubmit)
        buttonVerifyEmail.setOnClickListener {
            val code = inputCode.text.toString().trim()

            // Validate inputs
            if (code.isEmpty()){
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Send request to the server
            val request = EmailVerificationWithCodeRequest(email = email, code = code)
            apiService.verifyEmail(request).enqueue(object : Callback<ResponseMessage> {
                override fun onResponse(call: Call<ResponseMessage>, response: Response<ResponseMessage>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@EmailVerifyActivity, "Email verified successfully", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this@EmailVerifyActivity, LogActivity::class.java)
                        startActivity(intent)

                    } else {
                        val errorBody = response.errorBody()?.string()
                        Toast.makeText(this@EmailVerifyActivity, "Failed: $errorBody", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseMessage>, t: Throwable) {
                    Toast.makeText(this@EmailVerifyActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })

        }


    }
}