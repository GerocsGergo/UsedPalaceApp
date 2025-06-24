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
import com.example.usedpalace.requests.ChangePhoneNumberRequest
import com.example.usedpalace.requests.ConfirmPhoneNumberChangeRequest
import com.example.usedpalace.responses.ApiResponseGeneric
import com.google.android.material.textfield.TextInputLayout
import network.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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
        buttonCancel.setOnClickListener {
            finish()
        }
        buttonRequest.setOnClickListener {
            val password = inputPassword.text.toString()

            if (UserSession.getUserId() != null) {
                if (password.isNotEmpty()) {
                    //Step 1 request
                    requestPhoneNumberChange(password)
                } else {
                    Toast.makeText(this, "Please enter the password.", Toast.LENGTH_SHORT).show()
                }
            } else  {
                Toast.makeText(this, "Could not get UserId.", Toast.LENGTH_SHORT).show()
            }
        }
        buttonModify.setOnClickListener {
            val phoneNumber = inputPhoneNumber.text.toString()

            if (UserSession.getUserId() != null) {
                if (phoneNumber.isNotEmpty()) {
                    //Step 2 confirm
                    confirmPhoneNumberChange(phoneNumber)
                } else {
                    Toast.makeText(this, "Please enter the new phoneNumber.", Toast.LENGTH_SHORT).show()
                }
            } else  {
                Toast.makeText(this, "Could not get UserId.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestPhoneNumberChange(password: String){
        val request = ChangePhoneNumberRequest(UserSession.getUserId()!! , password)

        apiService.requestPhoneNumberChange(request).enqueue(object : Callback<ApiResponseGeneric> {
            override fun onResponse(call: Call<ApiResponseGeneric>, response: Response<ApiResponseGeneric>
            ) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ModifyPhoneActivity, "Password valid, you can now enter new phone number!", Toast.LENGTH_SHORT).show()
                    inputPassword.visibility = View.GONE
                    buttonRequest.visibility = View.GONE
                    passwordText.visibility = View.GONE
                    passwordInputLayout.visibility = View.GONE

                    phoneNumberInputLayout.visibility = View.VISIBLE
                    phoneNumberText.visibility = View.VISIBLE
                    inputPhoneNumber.visibility = View.VISIBLE
                    buttonModify.visibility = View.VISIBLE
                } else {
                    Toast.makeText(this@ModifyPhoneActivity, "Failed to request password change.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponseGeneric>, t: Throwable) {
                Toast.makeText(
                    this@ModifyPhoneActivity,
                    "Network error.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

    }

    private fun confirmPhoneNumberChange(phoneNumber: String){
        val request = ConfirmPhoneNumberChangeRequest(UserSession.getUserId()!!, phoneNumber)

        apiService.confirmPhoneNumberChange(request).enqueue(object : Callback<ApiResponseGeneric> {
            override fun onResponse(call: Call<ApiResponseGeneric>, response: Response<ApiResponseGeneric>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ModifyPhoneActivity, "Phone number changed successfully!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@ModifyPhoneActivity, ProfileActivity::class.java)
                    startActivity(intent)
                    finishAffinity()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@ModifyPhoneActivity, "Invalid phone number or error: $errorBody", Toast.LENGTH_SHORT).show()

                }
            }

            override fun onFailure(call: Call<ApiResponseGeneric>, t: Throwable) {
                Toast.makeText(
                    this@ModifyPhoneActivity,
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

        inputPassword = findViewById(R.id.inputPassword)
        inputPhoneNumber = findViewById(R.id.inputPhoneNumber)
        passwordText = findViewById(R.id.passwordText)
        phoneNumberText = findViewById(R.id.phoneNumberText)

        passwordInputLayout = findViewById(R.id.passwordInputLayout)
        phoneNumberInputLayout = findViewById(R.id.phoneNumberInputLayout)

    }

    private fun setupRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)
    }
}