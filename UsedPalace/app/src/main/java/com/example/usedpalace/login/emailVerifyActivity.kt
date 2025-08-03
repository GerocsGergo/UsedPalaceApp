package com.example.usedpalace.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.usedpalace.R
import com.example.usedpalace.requests.EmailVerificationWithCodeRequest
import com.example.usedpalace.responses.ResponseMessage
import com.google.android.material.textfield.TextInputEditText
import network.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class EmailVerifyActivity : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var inputCode: TextInputEditText
    private lateinit var buttonVerifyEmail: Button
    private lateinit var mainLayout: ConstraintLayout

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
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
        } else  {
            // Send request to the server
            val request = EmailVerificationWithCodeRequest(email = email, code = code)
            apiService.verifyEmail(request).enqueue(object : Callback<ResponseMessage> {
                override fun onResponse(call: Call<ResponseMessage>, response: Response<ResponseMessage>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@EmailVerifyActivity, "Email verified successfully", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this@EmailVerifyActivity, LogActivity::class.java)
                        startActivity(intent)

                    } else {
                        val errorBody = response.errorBody()?.string()
                        Toast.makeText(this@EmailVerifyActivity, "Failed: $errorBody", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ResponseMessage>, t: Throwable) {
                    Toast.makeText(this@EmailVerifyActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }


    }

    private fun initializeViews() {
        inputCode = findViewById(R.id.inputCode)
        buttonVerifyEmail = findViewById(R.id.buttonSubmit)
    }

    private fun getIntentData() {
        email = intent.getStringExtra("email") ?: ""

        if (email.isNullOrEmpty()){
            showErrorMessage("Could not get email")
            finish()
        }
    }

    private fun setupButtons() {
        buttonVerifyEmail.setOnClickListener {
            email?.let { it1 -> verifyEmail(it1) }
        }
    }

    private fun setupRetrofit() {
        apiService = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private fun showErrorMessage(message: String) {
        mainLayout.removeAllViews()
        val errorView = layoutInflater.inflate(
            R.layout.show_error_message,
            mainLayout,
            false
        )

        errorView.findViewById<TextView>(R.id.messageText).text = message
        mainLayout.addView(errorView)
    }

}