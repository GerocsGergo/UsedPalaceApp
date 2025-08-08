package com.example.usedpalace.profilemenus.forprofileactivity

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.usedpalace.R
import com.example.usedpalace.RetrofitClient
import com.example.usedpalace.UserSession
import com.example.usedpalace.profilemenus.ProfileActivity
import com.example.usedpalace.requests.ChangeEmailRequest
import com.example.usedpalace.requests.ConfirmEmailOrPasswordChangeOrDeleteRequest
import com.example.usedpalace.responses.ApiResponseGeneric
import com.google.android.material.textfield.TextInputLayout
import network.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ModifyEmailActivity : AppCompatActivity() {

    private lateinit var inputNewEmail: EditText
    private lateinit var inputCode: EditText
    private lateinit var buttonModify: Button
    private lateinit var buttonRequest: Button
    private lateinit var buttonCancel: Button
    private lateinit var newEmailText: TextView
    private lateinit var codeText: TextView

    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var codeInputLayout: TextInputLayout

    private lateinit var apiService: ApiService
    private lateinit var prefs: SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_modify_email)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRetrofit()
        setupViewItems()
        setupClickListeners()
    }


    private fun setupViewItems() {
        buttonCancel = findViewById(R.id.buttonCancel)
        buttonModify = findViewById(R.id.buttonModify)
        buttonRequest = findViewById(R.id.buttonRequest)
        inputNewEmail = findViewById(R.id.inputNewEmail)
        inputCode = findViewById(R.id.inputCode)
        newEmailText = findViewById(R.id.newEmailText)
        codeText = findViewById(R.id.codeText)
        emailInputLayout = findViewById(R.id.emailInputLayout)
        codeInputLayout = findViewById(R.id.codeInputLayout)

    }

    private fun setupClickListeners() {
        buttonCancel.setOnClickListener {
            finish()
        }
        buttonRequest.setOnClickListener {
            val email = inputNewEmail.text.toString()


            if (UserSession.getUserId() != null) {
                if (email.isNotEmpty()) {
                    // Step 1: Request email change
                    changeEmailRequest(email)

                } else {
                    Toast.makeText(this, "Please enter an email.", Toast.LENGTH_SHORT).show()
                }
            } else  {
                Toast.makeText(this, "Could not get UserId.", Toast.LENGTH_SHORT).show()
            }


        }
        buttonModify.setOnClickListener {
            val code = inputCode.text.toString()

            if (UserSession.getUserId() != null) {
                if (code.isNotEmpty()) {
                    // Step 2: Confirm email change
                    confirmEmail(code)
                } else {
                    Toast.makeText(this, "Please enter a code.", Toast.LENGTH_SHORT).show()
                }
            } else  {
                Toast.makeText(this, "Could not get UserId.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun changeEmailRequest(email: String){

        val request = ChangeEmailRequest(UserSession.getUserId()!!, email)
        apiService.requestEmailChange(request).enqueue(object : Callback<ApiResponseGeneric> {
            override fun onResponse(call: Call<ApiResponseGeneric>, response: Response<ApiResponseGeneric>
            ) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ModifyEmailActivity, "Verification code sent!", Toast.LENGTH_SHORT).show()
                    newEmailText.visibility = View.GONE
                    inputNewEmail.visibility = View.GONE
                    buttonRequest.visibility = View.GONE
                    emailInputLayout.visibility = View.GONE

                    codeInputLayout.visibility = View.VISIBLE
                    codeText.visibility = View.VISIBLE
                    inputCode.visibility = View.VISIBLE
                    buttonModify.visibility = View.VISIBLE
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@ModifyEmailActivity, "Failed to request email change: $errorBody", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponseGeneric>, t: Throwable) {
                Toast.makeText(
                    this@ModifyEmailActivity,
                    "Network error.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun confirmEmail(code: String){
        val confirmRequest =
            ConfirmEmailOrPasswordChangeOrDeleteRequest(UserSession.getUserId()!!, code)
        apiService.confirmEmailChange(confirmRequest).enqueue(object : Callback<ApiResponseGeneric> {
            override fun onResponse(call: Call<ApiResponseGeneric>, response: Response<ApiResponseGeneric>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@ModifyEmailActivity, "Email changed successfully!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@ModifyEmailActivity, ProfileActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@ModifyEmailActivity, "Invalid code or error.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponseGeneric>, t: Throwable) {
                    Toast.makeText(
                        this@ModifyEmailActivity,
                        "Network error.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }


    private fun setupRetrofit() {
        prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        RetrofitClient.init(applicationContext)
        apiService = RetrofitClient.apiService
    }
}
