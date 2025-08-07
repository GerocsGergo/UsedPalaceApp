package com.example.usedpalace.login

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.usedpalace.MainMenuActivity
import com.example.usedpalace.R
import com.example.usedpalace.RetrofitClient
import com.example.usedpalace.RetrofitClientNoAuth
import com.example.usedpalace.UserSession
import com.example.usedpalace.requests.EmailVerificationRequest
import com.example.usedpalace.requests.RegUser
import com.example.usedpalace.responses.ResponseMessage
import com.google.android.material.textfield.TextInputLayout
import network.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RegActivity : AppCompatActivity(){

    private lateinit var apiServiceNoAuth: ApiService
    private lateinit var prefs: SharedPreferences

    private lateinit var inputFullname: EditText
    private lateinit var inputRePassword: EditText
    private lateinit var inputPassword: EditText
    private lateinit var inputEmail: EditText
    private lateinit var inputPhoneNumber: EditText

    private lateinit var btnRegister :Button
    private lateinit var buttonBack :Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupViews()
        initialize()
        setupClickListeners()



    }

    private fun setupViews() {

        inputFullname = findViewById(R.id.inputUsername)
        inputEmail = findViewById(R.id.inputEmail)
        inputPassword = findViewById(R.id.inputPassword)
        inputRePassword = findViewById(R.id.inputRePassword)
        inputPhoneNumber = findViewById(R.id.inputPhoneNumber)
        btnRegister = findViewById(R.id.buttonReg)
        buttonBack = findViewById(R.id.buttonBack)
    }

    private fun initialize() {
        prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        RetrofitClient.init(applicationContext)
        apiServiceNoAuth = RetrofitClientNoAuth.apiService
    }

    private fun validate(
        fullname: String,
        email: String,
        password: String,
        rePassword: String,
        phoneNumber: String
    ): Boolean {
        if (fullname.isEmpty()) {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!fullname.matches(Regex("^[a-zA-Z\\s'-]+$"))) {
            Toast.makeText(this, "Your name can only contain letters and spaces", Toast.LENGTH_SHORT).show()
            return false
        }
        if (fullname.length > 50) {
            Toast.makeText(this, "Your name can be only 50 characters long", Toast.LENGTH_SHORT).show()
            return false
        }
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.isEmpty()) {
            Toast.makeText(this, "Please enter a password", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.length < 8) {
            Toast.makeText(this, "Your password is too short", Toast.LENGTH_SHORT).show()
            return false
        }
        val passwordPattern = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{8,}$")
        if (!passwordPattern.matches(password)) {
            Toast.makeText(this, "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password != rePassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return false
        }
        if (phoneNumber.isEmpty()) {
            Toast.makeText(this, "Please enter your phone number", Toast.LENGTH_SHORT).show()
            return false
        }
        if (phoneNumber.length != 11) {
            Toast.makeText(this, "Your phone number is in incorrect form", Toast.LENGTH_SHORT).show()
            return false
        }
        val phoneNumberPattern = Regex("^06\\d{9}\$")
        if (!phoneNumberPattern.matches(phoneNumber)) {
            Toast.makeText(this, "Invalid phoneNumber format", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun register(fullname: String, email: String, password: String, phoneNumber: String) {
        // If all validations pass, proceed with registration
        val newUser = RegUser(
            fullname = fullname,
            email = email,
            password = password,
            phoneNumber = phoneNumber
        )

        // Send the user data to the backend
        apiServiceNoAuth.registerUser(newUser).enqueue(object : Callback<ResponseMessage> {
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
                    apiServiceNoAuth.sendVerifyEmail(request).enqueue(object : Callback<ResponseMessage> {
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


    private fun setupClickListeners() {
        buttonBack.setOnClickListener {
            val intent = Intent(this, LogActivity::class.java)
            startActivity(intent)
        }


        btnRegister.setOnClickListener {
            val fullname = inputFullname.text.toString().trim()
            val email = inputEmail.text.toString().trim()
            val password = inputPassword.text.toString().trim()
            val rePassword = inputRePassword.text.toString().trim()
            val phoneNumber = inputPhoneNumber.text.toString().trim()

            if (!validate(fullname, email, password, rePassword, phoneNumber)) {
                return@setOnClickListener
            }

            register(fullname, email, password, phoneNumber)
        }
    }
}




