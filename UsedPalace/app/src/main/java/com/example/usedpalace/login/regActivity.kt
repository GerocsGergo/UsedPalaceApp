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
import com.example.usedpalace.requests.EmailVerificationRequest
import com.example.usedpalace.requests.RegUser
import com.example.usedpalace.responses.ResponseMessage
import network.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RegActivity : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.reg)
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

        // Create an instance of the API service
        val apiService = retrofit.create(ApiService::class.java)


        //Register user
        val inputFullname = findViewById<EditText>(R.id.inputUsername)
        val inputEmail = findViewById<EditText>(R.id.inputEmail)
        val inputPassword = findViewById<EditText>(R.id.inputPassword)
        val inputRePassword = findViewById<EditText>(R.id.inputRePassword)
        val inputPhoneNumber = findViewById<EditText>(R.id.inputPhoneNumber)
        val btnRegister: Button = findViewById(R.id.buttonReg)

        btnRegister.setOnClickListener {
            val fullname = inputFullname.text.toString().trim()
            val email = inputEmail.text.toString().trim()
            val password = inputPassword.text.toString().trim()
            val rePassword = inputRePassword.text.toString().trim()
            val phoneNumber = inputPhoneNumber.text.toString().trim()

            // Validate full name
            if (fullname.isEmpty()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!fullname.matches(Regex("^[a-zA-Z\\s'-]+$"))) {
                Toast.makeText(this, "Your name can only contain letters and spaces", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (fullname.length > 50) {
                Toast.makeText(this, "Your name can be only 50 characters long", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate email
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate password
            if (password.isEmpty()) {
                Toast.makeText(this, "Please enter a password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 8) {
                Toast.makeText(this, "Your password is too short", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val passwordPattern = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{8,}$")
            if (!passwordPattern.matches(password)) {
                Toast.makeText(this, "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != rePassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate phone number
            if (phoneNumber.isEmpty()) {
                Toast.makeText(this, "Please enter your phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (phoneNumber.length != 11) {
                Toast.makeText(this, "Your phone number is in incorrect form", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val phoneNumberPattern = Regex("^06\\d{9}\$")
            if (!phoneNumberPattern.matches(phoneNumber)) {
                Toast.makeText(this, "Invalid phoneNumber format", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // If all validations pass, proceed with registration
            val newUser = RegUser(
                fullname = fullname,
                email = email,
                password = password,
                phoneNumber = phoneNumber
            )

            // Send the user data to the backend
            apiService.registerUser(newUser).enqueue(object : Callback<ResponseMessage> {
                override fun onResponse(call: Call<ResponseMessage>, response: Response<ResponseMessage>) {
                    if (response.isSuccessful) {
                        val message = response.body()?.message
                        Toast.makeText(this@RegActivity, message, Toast.LENGTH_SHORT).show()
                        // Clear input fields after successful registration
                        inputFullname.text.clear()
                        inputEmail.text.clear()
                        inputPassword.text.clear()
                        inputRePassword.text.clear()
                        inputPhoneNumber.text.clear()

                        // Send email verification request to the server
                        val request = EmailVerificationRequest(email = email)
                        apiService.sendVerifyEmail(request).enqueue(object : Callback<ResponseMessage> {
                            override fun onResponse(call: Call<ResponseMessage>, response: Response<ResponseMessage>) {
                                if (response.isSuccessful) {
                                    Toast.makeText(this@RegActivity, "Email verification code sent to your email", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this@RegActivity, EmailVerifyActivity::class.java)
                                    intent.putExtra("email", email)
                                    startActivity(intent)
                                } else {
                                    val errorBody = response.errorBody()?.string()
                                    Toast.makeText(this@RegActivity, "Failed to send verification email: $errorBody", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(call: Call<ResponseMessage>, t: Throwable) {
                                Toast.makeText(this@RegActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                            }
                        })

                    } else {
                        val errorBody = response.errorBody()?.string()
                        Toast.makeText(this@RegActivity, "Registration failed: $errorBody", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseMessage>, t: Throwable) {
                    Toast.makeText(this@RegActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }



}   }