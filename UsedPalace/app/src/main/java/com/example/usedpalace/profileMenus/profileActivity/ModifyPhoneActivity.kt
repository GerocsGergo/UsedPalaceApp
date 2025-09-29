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
import network.RetrofitClient
import com.example.usedpalace.UserSession
import com.example.usedpalace.requests.ChangePhoneNumberRequest
import com.example.usedpalace.requests.ConfirmPhoneNumberChangeRequest
import com.example.usedpalace.responses.ApiResponseGeneric
import com.google.android.material.textfield.TextInputLayout
import network.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ModifyPhoneActivity : AppCompatActivity() {

    private lateinit var inputPassword: EditText
    private lateinit var inputPhoneNumber: EditText
    private lateinit var buttonModify: Button
    private lateinit var buttonRequest: Button
    private lateinit var buttonCancel: Button
    private lateinit var passwordText: TextView
    private lateinit var phoneNumberText: TextView
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var phoneNumberInputLayout: TextInputLayout

    private lateinit var apiService: ApiService
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_modify_phone)
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
            val password = inputPassword.text.toString()

            if (UserSession.getUserId() == null) {
                ErrorHandler.toaster(this, "Ismeretlen hiba történt")
                ErrorHandler.logToLogcat("ModifyPhoneActivity", "Could not get UserId.", ErrorHandler.LogLevel.ERROR)
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                ErrorHandler.toaster(this, "Kérjük, adja meg a jelszavát!")
                return@setOnClickListener
            }

            requestPhoneNumberChange(password)
        }

        buttonModify.setOnClickListener {
            val phoneNumber = inputPhoneNumber.text.toString()

            if (UserSession.getUserId() == null) {
                ErrorHandler.toaster(this, "Ismeretlen hiba történt")
                ErrorHandler.logToLogcat("ModifyPhoneActivity", "Could not get UserId.", ErrorHandler.LogLevel.ERROR)
                return@setOnClickListener
            }

            if (phoneNumber.isEmpty()) {
                ErrorHandler.toaster(this, "Kérjük, adja meg az új telefonszámot!")
                return@setOnClickListener
            }

            confirmPhoneNumberChange(phoneNumber)
        }
    }

    private fun requestPhoneNumberChange(password: String) {
        val request = ChangePhoneNumberRequest(UserSession.getUserId()!!, password)

        apiService.requestPhoneNumberChange(request).enqueue(object : Callback<ApiResponseGeneric> {
            override fun onResponse(call: Call<ApiResponseGeneric>, response: Response<ApiResponseGeneric>) {
                if (response.isSuccessful) {
                    ErrorHandler.toaster(this@ModifyPhoneActivity, "Jelszó érvényes, most megadhatja az új telefonszámot!")

                    inputPassword.visibility = View.GONE
                    buttonRequest.visibility = View.GONE
                    passwordText.visibility = View.GONE
                    passwordInputLayout.visibility = View.GONE

                    phoneNumberInputLayout.visibility = View.VISIBLE
                    phoneNumberText.visibility = View.VISIBLE
                    inputPhoneNumber.visibility = View.VISIBLE
                    buttonModify.visibility = View.VISIBLE
                } else {
                    ErrorHandler.handleApiError(this@ModifyPhoneActivity, null, response.message())
                }
            }

            override fun onFailure(call: Call<ApiResponseGeneric>, t: Throwable) {
                ErrorHandler.handleNetworkError(this@ModifyPhoneActivity, t)
            }
        })
    }

    private fun confirmPhoneNumberChange(phoneNumber: String) {
        val request = ConfirmPhoneNumberChangeRequest(UserSession.getUserId()!!, phoneNumber)

        val response = apiService.confirmPhoneNumberChange(request).enqueue(object : Callback<ApiResponseGeneric> {
            override fun onResponse(call: Call<ApiResponseGeneric>, response: Response<ApiResponseGeneric>) {
                if (response.isSuccessful) {
                    ErrorHandler.toaster(this@ModifyPhoneActivity, "Telefonszám sikeresen módosítva!")
                    val intent = Intent(this@ModifyPhoneActivity, ProfileActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    ErrorHandler.handleApiError(this@ModifyPhoneActivity, null,
                        response.body()?.message
                    )
                }
            }

            override fun onFailure(call: Call<ApiResponseGeneric>, t: Throwable) {
                ErrorHandler.handleNetworkError(this@ModifyPhoneActivity, t)
            }
        })
    }

    private fun setupViewItems() {
        buttonCancel = findViewById(R.id.buttonCancel)
        buttonModify = findViewById(R.id.buttonModify)
        buttonRequest = findViewById(R.id.buttonRequest)
        inputPassword = findViewById(R.id.inputPassword)
        inputPhoneNumber = findViewById(R.id.inputPhoneNumber)
        passwordText = findViewById(R.id.passwordText)
        phoneNumberText = findViewById(R.id.phoneNumberText)
        passwordInputLayout = findViewById(R.id.passwordInputLayout)
        phoneNumberInputLayout = findViewById(R.id.phoneNumberInputLayout)
    }

    private fun setupRetrofit() {
        prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        RetrofitClient.init(applicationContext)
        apiService = RetrofitClient.apiService
    }
}
