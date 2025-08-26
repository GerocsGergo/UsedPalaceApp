package com.example.usedpalace.loginMenus

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.usedpalace.ErrorHandler
import com.example.usedpalace.R
import com.example.usedpalace.RetrofitClient
import com.example.usedpalace.RetrofitClientNoAuth
import com.example.usedpalace.requests.EmailVerificationWithCodeRequest
import com.example.usedpalace.responses.ResponseMessage
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import network.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EmailVerifyActivity : AppCompatActivity() {
    private lateinit var apiServiceNoAuth: ApiService
    private lateinit var prefs: SharedPreferences

    private lateinit var inputCode: TextInputEditText
    private lateinit var buttonVerifyEmail: Button
    private lateinit var bactToLogin: Button
    private lateinit var mainLayout: ScrollView

    private lateinit var inputEmail: TextInputEditText
    private lateinit var inputEmailLayout: TextInputLayout
    private lateinit var emailLabel: TextView

    private var code: String = "null"
    private var email: String? = "null"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_email_verify)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRetrofit()
        initializeViews()
        getIntentData()
        setupButtons()
    }

    private fun verifyEmail(email: String) {
        code = inputCode.text.toString().trim()

        // Validate inputs
        if (code.isEmpty()){
            //Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            ErrorHandler.toaster(this, "Kérjük töltsön ki minden mezőt")
        } else  {
            // Send request to the server
            val request = EmailVerificationWithCodeRequest(email = email, code = code)
            apiServiceNoAuth.verifyEmail(request).enqueue(object : Callback<ResponseMessage> {
                override fun onResponse(call: Call<ResponseMessage>, response: Response<ResponseMessage>) {
                    if (response.isSuccessful) {
                        //Toast.makeText(this@EmailVerifyActivity, "Email verified successfully", Toast.LENGTH_SHORT).show()
                        ErrorHandler.toaster(this@EmailVerifyActivity, "Email verified successfully")
                        val intent = Intent(this@EmailVerifyActivity, LogActivity::class.java)
                        startActivity(intent)

                    } else {
                        val errorBody = response.errorBody()?.string()
                        //Toast.makeText(this@EmailVerifyActivity, "Failed: $errorBody", Toast.LENGTH_SHORT).show()
                        ErrorHandler.handleApiError(this@EmailVerifyActivity, null, errorBody)
                    }
                }
                override fun onFailure(call: Call<ResponseMessage>, t: Throwable) {
                    //Toast.makeText(this@EmailVerifyActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    ErrorHandler.handleNetworkError(this@EmailVerifyActivity,t)
                }
            })
        }


    }

    private fun initializeViews() {
        mainLayout = findViewById(R.id.main)
        inputCode = findViewById(R.id.inputCode)
        buttonVerifyEmail = findViewById(R.id.buttonSubmit)
        inputEmail = findViewById(R.id.inputEmail)
        inputEmailLayout = findViewById(R.id.inputEmailLayout)
        emailLabel = findViewById(R.id.emailLabel)
        bactToLogin = findViewById(R.id.buttonBackToLogin)
    }

    private fun getIntentData() {
        email = intent.getStringExtra("email") ?: ""

        if (email.isNullOrEmpty()) {
            emailLabel.visibility = View.VISIBLE
            inputEmailLayout.visibility = View.VISIBLE
        }
    }

    private fun setupButtons() {
        buttonVerifyEmail.setOnClickListener {
            if (email.isNullOrEmpty()) {
                email = inputEmail.text.toString().trim()
            }

            if (email.isNullOrEmpty()) {
                //Toast.makeText(this, "Kérjük, add meg az email címet", Toast.LENGTH_SHORT).show()
                ErrorHandler.toaster(this, "Kérjük, add meg az email címet")
                return@setOnClickListener
            }

            email?.let { verifyEmail(it) }
        }

        bactToLogin.setOnClickListener {
            val intent = Intent(this, LogActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRetrofit() {
        prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        RetrofitClient.init(applicationContext)
        apiServiceNoAuth = RetrofitClientNoAuth.apiService
    }

}