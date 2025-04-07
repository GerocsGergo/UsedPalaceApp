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
import com.example.usedpalace.MainMenuActivity
import com.example.usedpalace.R
import com.example.usedpalace.UserSession
import com.example.usedpalace.requests.NewLogin
import com.example.usedpalace.responses.ResponseMessageWithUser
import network.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LogActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val buttonReg: Button = findViewById(R.id.buttonReg)
        buttonReg.setOnClickListener {
            val intent = Intent(this, RegActivity::class.java)
            startActivity(intent)
        }

        val buttonForget: Button = findViewById(R.id.buttonForget)
        buttonForget.setOnClickListener {
            val intent = Intent(this, ForgottenPasswordActivity::class.java)
            startActivity(intent)
        }

        // Initialize Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/") // Use 10.0.2.2 for localhost in Android emulator
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // Create an instance of the API service
        val apiService = retrofit.create(ApiService::class.java)

        //Register user
        val inputEmail = findViewById<EditText>(R.id.inputEmail)
        val inputPassword = findViewById<EditText>(R.id.inputPassword)
        val btnLogin: Button = findViewById(R.id.buttonLogin)

        btnLogin.setOnClickListener {
            val email = inputEmail.text.toString().trim()
            val password = inputPassword.text.toString().trim()

            // Validate email and password
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newLogin = NewLogin(
                email = email,
                password = password
            )

            // Send the login request to the server
            apiService.loginUser(newLogin).enqueue(object : Callback<ResponseMessageWithUser> {
                override fun onResponse(call: Call<ResponseMessageWithUser>, response: Response<ResponseMessageWithUser>) {
                    if (response.isSuccessful) {
                        val message = response.body()?.message
                        Toast.makeText(this@LogActivity, message, Toast.LENGTH_SHORT).show()

                        // Clear input fields
                        inputEmail.text.clear()
                        inputPassword.text.clear()

                        val userData = response.body()?.user
                        userData?.let {
                            // Store user data in singleton
                            UserSession.setUserData(
                                id = it.id,
                                name = it.name
                            )
                        }
                        // Navigate to the main menu
                        val intent = Intent(this@LogActivity, MainMenuActivity::class.java)
                        startActivity(intent)
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val statusCode = response.code()
                        val headers = response.headers().toString()

                        println("Status Code: $statusCode")
                        println("Headers: $headers")
                        println("Error Response: $errorBody")

                        Toast.makeText(this@LogActivity, "Login failed: $errorBody", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseMessageWithUser>, t: Throwable) {
                    // Handle network errors
                    Toast.makeText(this@LogActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

    }
}