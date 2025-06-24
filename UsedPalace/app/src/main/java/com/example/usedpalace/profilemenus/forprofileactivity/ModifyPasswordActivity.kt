package com.example.usedpalace.profilemenus.forprofileactivity

import android.content.Intent
import android.os.Bundle
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
import com.example.usedpalace.UserSession
import com.example.usedpalace.profilemenus.ProfileActivity
import com.example.usedpalace.requests.ChangePasswordRequest
import com.example.usedpalace.requests.ConfirmEmailOrPasswordChangeOrDeleteRequest
import com.example.usedpalace.responses.ApiResponseGeneric
import com.google.android.material.textfield.TextInputLayout
import network.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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
        buttonCancel.setOnClickListener {
            finish()
        }
        buttonRequest.setOnClickListener {
            val oldPassword = inputOldPassword.text.toString()
            val newPassword = inputNewPassword.text.toString()
            val newPasswordRe = inputNewRePassword.text.toString()

            if (newPasswordRe == newPassword){
                if (UserSession.getUserId() != null) {
                    if (oldPassword.isNotEmpty() && newPassword.isNotEmpty()) {
                        //Step 1 request
                        changePasswordRequest(oldPassword, newPassword)
                    } else {
                        Toast.makeText(this, "Please enter the passwords.", Toast.LENGTH_SHORT).show()
                    }
                } else  {
                    Toast.makeText(this, "Could not get UserId.", Toast.LENGTH_SHORT).show()
                }
            } else  {
                Toast.makeText(this, "A jelszavak nem egyeznek", Toast.LENGTH_SHORT).show()
            }


        }
        buttonModify.setOnClickListener {
            val code = inputCode.text.toString()

            if (UserSession.getUserId() != null) {
                if (code.isNotEmpty()) {
                    //Step 2 confirm
                    confirmPassword(code)
                } else {
                    Toast.makeText(this, "Please enter the code.", Toast.LENGTH_SHORT).show()
                }
            } else  {
                Toast.makeText(this, "Could not get UserId.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun changePasswordRequest(oldPassword: String, newPassword: String){
        val request = ChangePasswordRequest(UserSession.getUserId()!!, oldPassword, newPassword)


        apiService.requestPasswordChange(request).enqueue(object : Callback<ApiResponseGeneric> {
            override fun onResponse(call: Call<ApiResponseGeneric>, response: Response<ApiResponseGeneric>
            ) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ModifyPasswordActivity, "Verification code sent!", Toast.LENGTH_SHORT).show()
                    inputNewPassword.visibility = View.GONE
                    inputOldPassword.visibility = View.GONE
                    buttonRequest.visibility = View.GONE
                    oldPasswordText.visibility = View.GONE
                    newPasswordText.visibility = View.GONE
                    inputNewRePassword.visibility = View.GONE
                    newPasswordReText.visibility = View.GONE
                    oldPasswordInputLayout.visibility = View.GONE
                    newPasswordInputLayout.visibility = View.GONE
                    newPasswordReInputLayout.visibility = View.GONE

                    codeInputLayout.visibility = View.VISIBLE
                    codeText.visibility = View.VISIBLE
                    inputCode.visibility = View.VISIBLE
                    buttonModify.visibility = View.VISIBLE
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@ModifyPasswordActivity, "Failed to request password change: $errorBody", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponseGeneric>, t: Throwable) {
                Toast.makeText(
                    this@ModifyPasswordActivity,
                    "Network error.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun confirmPassword(code: String){
        val request = ConfirmEmailOrPasswordChangeOrDeleteRequest(UserSession.getUserId()!!, code)

        apiService.confirmPasswordChange(request).enqueue(object : Callback<ApiResponseGeneric> {
            override fun onResponse(call: Call<ApiResponseGeneric>, response: Response<ApiResponseGeneric>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@ModifyPasswordActivity, "Password changed successfully!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@ModifyPasswordActivity, ProfileActivity::class.java)
                        startActivity(intent)
                        finishAffinity() //cleareli a backstacket elv
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Toast.makeText(this@ModifyPasswordActivity, "Invalid code or error: $errorBody", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponseGeneric>, t: Throwable) {
                    Toast.makeText(
                        this@ModifyPasswordActivity,
                        "Network error.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun setupViewItems() {
        buttonCancel = findViewById(R.id.buttonCancel)
        buttonModify = findViewById(R.id.buttonModify)
        buttonRequest = findViewById(R.id.buttonRequest)
        inputOldPassword = findViewById(R.id.inputOldPassword)
        inputNewPassword = findViewById(R.id.inputNewPassword)
        inputCode = findViewById(R.id.inputCode)
        oldPasswordText = findViewById(R.id.oldPasswordText)
        newPasswordText = findViewById(R.id.newPasswordText)
        codeText = findViewById(R.id.codeText)
        newPasswordReText= findViewById(R.id.newPasswordReText)
        inputNewRePassword = findViewById(R.id.inputNewRePassword)
        oldPasswordInputLayout = findViewById(R.id.oldPasswordInputLayout)
        newPasswordInputLayout = findViewById(R.id.newPasswordInputLayout)
        newPasswordReInputLayout = findViewById(R.id.newPasswordReInputLayout)
        codeInputLayout = findViewById(R.id.codeInputLayout)

    }

    private fun setupRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)
    }


}