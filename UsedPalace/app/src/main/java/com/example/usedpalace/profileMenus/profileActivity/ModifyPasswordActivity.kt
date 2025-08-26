package com.example.usedpalace.profileMenus.profileActivity

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.usedpalace.ErrorHandler
import com.example.usedpalace.R
import com.example.usedpalace.RetrofitClient
import com.example.usedpalace.UserSession
import com.example.usedpalace.requests.ChangePasswordRequest
import com.example.usedpalace.requests.ConfirmEmailOrPasswordChangeOrDeleteRequest
import com.example.usedpalace.responses.ApiResponseGeneric
import com.google.android.material.textfield.TextInputLayout
import network.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ModifyPasswordActivity : AppCompatActivity() {

    private lateinit var inputOldPassword: EditText
    private lateinit var inputNewPassword: EditText
    private lateinit var inputNewRePassword: EditText
    private lateinit var inputCode: EditText
    private lateinit var oldPasswordInputLayout: TextInputLayout
    private lateinit var newPasswordInputLayout: TextInputLayout
    private lateinit var newPasswordReInputLayout: TextInputLayout
    private lateinit var codeInputLayout: TextInputLayout
    private lateinit var buttonModify: Button
    private lateinit var buttonRequest: Button
    private lateinit var buttonCancel: Button
    private lateinit var newPasswordText: TextView
    private lateinit var newPasswordReText: TextView
    private lateinit var oldPasswordText: TextView
    private lateinit var codeText: TextView

    private lateinit var apiService: ApiService
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_modify_password)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRetrofit()
        setupViewItems()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        buttonCancel.setOnClickListener { finish() }

        buttonRequest.setOnClickListener {
            val oldPassword = inputOldPassword.text.toString()
            val newPassword = inputNewPassword.text.toString()
            val newPasswordRe = inputNewRePassword.text.toString()

            if (newPasswordRe != newPassword) {
                ErrorHandler.toaster(this, "A jelszavak nem egyeznek!")
                return@setOnClickListener
            }

            if (UserSession.getUserId() == null) {
                ErrorHandler.toaster(this, "Ismeretlen hiba történt")
                ErrorHandler.logToLogcat("ModifyPasswordActivity", "Could not get UserId.", ErrorHandler.LogLevel.ERROR)
                return@setOnClickListener
            }

            if (oldPassword.isEmpty() || newPassword.isEmpty()) {
                ErrorHandler.toaster(this, "Kérjük, adja meg a jelszavakat!")
                return@setOnClickListener
            }

            changePasswordRequest(oldPassword, newPassword)
        }

        buttonModify.setOnClickListener {
            val code = inputCode.text.toString()

            if (UserSession.getUserId() == null) {
                ErrorHandler.toaster(this, "Ismeretlen hiba történt")
                ErrorHandler.logToLogcat("ModifyPasswordActivity", "Could not get UserId.", ErrorHandler.LogLevel.ERROR)
                return@setOnClickListener
            }

            if (code.isEmpty()) {
                ErrorHandler.toaster(this, "Kérjük, adja meg a kódot!")
                return@setOnClickListener
            }

            confirmPassword(code)
        }
    }

    private fun changePasswordRequest(oldPassword: String, newPassword: String) {
        val request = ChangePasswordRequest(UserSession.getUserId()!!, oldPassword, newPassword)
        apiService.requestPasswordChange(request).enqueue(object : Callback<ApiResponseGeneric> {
            override fun onResponse(call: Call<ApiResponseGeneric>, response: Response<ApiResponseGeneric>) {
                if (response.isSuccessful) {
                    ErrorHandler.toaster(this@ModifyPasswordActivity, "Hitelesítő kód elküldve!")

                    // UI módosítás
                    inputOldPassword.visibility = View.GONE
                    inputNewPassword.visibility = View.GONE
                    inputNewRePassword.visibility = View.GONE
                    oldPasswordText.visibility = View.GONE
                    newPasswordText.visibility = View.GONE
                    newPasswordReText.visibility = View.GONE
                    oldPasswordInputLayout.visibility = View.GONE
                    newPasswordInputLayout.visibility = View.GONE
                    newPasswordReInputLayout.visibility = View.GONE
                    buttonRequest.visibility = View.GONE

                    codeInputLayout.visibility = View.VISIBLE
                    codeText.visibility = View.VISIBLE
                    inputCode.visibility = View.VISIBLE
                    buttonModify.visibility = View.VISIBLE
                } else {
                    ErrorHandler.handleApiError(this@ModifyPasswordActivity, null, response.message())
                }
            }

            override fun onFailure(call: Call<ApiResponseGeneric>, t: Throwable) {
                ErrorHandler.handleNetworkError(this@ModifyPasswordActivity, t)
            }
        })
    }

    private fun confirmPassword(code: String) {
        val request = ConfirmEmailOrPasswordChangeOrDeleteRequest(UserSession.getUserId()!!, code)
        apiService.confirmPasswordChange(request).enqueue(object : Callback<ApiResponseGeneric> {
            override fun onResponse(call: Call<ApiResponseGeneric>, response: Response<ApiResponseGeneric>) {
                if (response.isSuccessful) {
                    ErrorHandler.toaster(this@ModifyPasswordActivity, "Jelszó sikeresen módosítva!")
                    val intent = Intent(this@ModifyPasswordActivity, ProfileActivity::class.java)
                    startActivity(intent)
                    finishAffinity()
                } else {
                    ErrorHandler.handleApiError(this@ModifyPasswordActivity, null, response.message())
                }
            }

            override fun onFailure(call: Call<ApiResponseGeneric>, t: Throwable) {
                ErrorHandler.handleNetworkError(this@ModifyPasswordActivity, t)
            }
        })
    }

    private fun setupViewItems() {
        buttonCancel = findViewById(R.id.buttonCancel)
        buttonModify = findViewById(R.id.buttonModify)
        buttonRequest = findViewById(R.id.buttonRequest)
        inputOldPassword = findViewById(R.id.inputOldPassword)
        inputNewPassword = findViewById(R.id.inputNewPassword)
        inputNewRePassword = findViewById(R.id.inputNewRePassword)
        inputCode = findViewById(R.id.inputCode)
        oldPasswordText = findViewById(R.id.oldPasswordText)
        newPasswordText = findViewById(R.id.newPasswordText)
        newPasswordReText = findViewById(R.id.newPasswordReText)
        codeText = findViewById(R.id.codeText)
        oldPasswordInputLayout = findViewById(R.id.oldPasswordInputLayout)
        newPasswordInputLayout = findViewById(R.id.newPasswordInputLayout)
        newPasswordReInputLayout = findViewById(R.id.newPasswordReInputLayout)
        codeInputLayout = findViewById(R.id.codeInputLayout)
    }

    private fun setupRetrofit() {
        prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        RetrofitClient.init(applicationContext)
        apiService = RetrofitClient.apiService
    }
}
