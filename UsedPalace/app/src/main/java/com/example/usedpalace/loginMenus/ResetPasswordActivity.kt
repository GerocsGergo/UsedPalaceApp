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
import com.example.usedpalace.requests.ResetPasswordRequest
import com.example.usedpalace.responses.ResponseMessage
import network.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ResetPasswordActivity : AppCompatActivity() {
    private lateinit var apiServiceNoAuth: ApiService
    private lateinit var prefs: SharedPreferences

    private lateinit var inputCode: EditText
    private lateinit var inputNewPassword: EditText
    private lateinit var inputConfirmPassword: EditText

    private lateinit var buttonResetPassword :Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reset_password)
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
        buttonResetPassword.setOnClickListener {
            val code = inputCode.text.toString().trim()
            val newPassword = inputNewPassword.text.toString().trim()
            val confirmPassword = inputConfirmPassword.text.toString().trim()
            val email = intent.getStringExtra("email") ?: ""

            // Validate inputs
            if (code.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                ErrorHandler.toaster(this,"Kérjük töltsön ki minden mezőt")
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                 ErrorHandler.toaster(this,"A jelszavak nem egyeznek")
                return@setOnClickListener
            }

            // Send request to the server
            val request = ResetPasswordRequest(email = email, code = code, newPassword = newPassword)
            apiServiceNoAuth.confirmPasswordReset(request).enqueue(object : Callback<ResponseMessage> {
                override fun onResponse(call: Call<ResponseMessage>, response: Response<ResponseMessage>) {
                    if (response.isSuccessful) {
                        ErrorHandler.toaster(this@ResetPasswordActivity, "Jelszó sikeresen visszaállítva")
                        val intent = Intent(this@ResetPasswordActivity, LogActivity::class.java)
                        startActivity(intent)
                        finishAffinity() //close and clear backstack
                    } else {
                        val errorBody = response.errorBody()?.string()
                        ErrorHandler.handleApiError(this@ResetPasswordActivity, response.code(), errorBody)
                         }
                }

                override fun onFailure(call: Call<ResponseMessage>, t: Throwable) {
                    ErrorHandler.handleNetworkError(this@ResetPasswordActivity, t)

                     }
            })
        }
    }
    private fun setupViews() {

        inputCode = findViewById(R.id.inputCode)
        inputNewPassword = findViewById(R.id.inputPassword)
        inputConfirmPassword = findViewById(R.id.inputRePassword)
        buttonResetPassword = findViewById(R.id.buttonSubmit)
    }

    private fun initialize() {
        prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        RetrofitClient.init(applicationContext)
        apiServiceNoAuth = RetrofitClientNoAuth.apiService
    }
}