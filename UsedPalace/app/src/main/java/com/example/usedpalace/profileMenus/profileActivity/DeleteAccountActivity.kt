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
import com.example.usedpalace.loginMenus.LogActivity
import com.example.usedpalace.requests.ConfirmDeleteAccount
import com.example.usedpalace.requests.DeleteAccountRequest
import com.example.usedpalace.responses.ApiResponseGeneric
import com.google.android.material.textfield.TextInputLayout
import network.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DeleteAccountActivity : AppCompatActivity() {

    private lateinit var inputEmail: EditText
    private lateinit var inputCode: EditText
    private lateinit var inputPassword: EditText

    private lateinit var buttonConfirm: Button
    private lateinit var buttonRequest: Button
    private lateinit var buttonCancel: Button

    private lateinit var text: TextView
    private lateinit var passwordText: TextView
    private lateinit var emailText: TextView
    private lateinit var codeText: TextView

    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var codeInputLayout: TextInputLayout
    private lateinit var passwordInputLayout: TextInputLayout

    private lateinit var apiService: ApiService
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_delete_account)
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

            if (UserSession.getUserId() != null) { //Csak request
                    deleteUserRequest()

            } else  {
                ErrorHandler.toaster(this, "Ismeretlen hiba történt")
                ErrorHandler.logToLogcat("DeleteAccountActivity", "Could not get UserId.", ErrorHandler.LogLevel.ERROR)
            }


        }
        buttonConfirm.setOnClickListener {
            val email = inputEmail.text.toString()
            val code = inputCode.text.toString()
            val password = inputPassword.text.toString()

            if (UserSession.getUserId() != null) { //Confirm és egyebek
                if (code.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                    deleteUserConfirm(code, email, password)        //serveren végre hajtani

                } else {
                    ErrorHandler.toaster(this, "Kérjük töltse ki az összes mezőt!")
                }
            } else  {
                ErrorHandler.logToLogcat("DeleteAccountActivity", "Could not get UserId.", ErrorHandler.LogLevel.ERROR)
                ErrorHandler.toaster(this, "Ismeretlen hiba történt")
            }
        }
    }

    private fun deleteUserRequest(){
        val request = DeleteAccountRequest(UserSession.getUserId()!!)
        apiService.requestDeleteUser(request).enqueue(object : Callback<ApiResponseGeneric> {
            override fun onResponse(call: Call<ApiResponseGeneric>, response: Response<ApiResponseGeneric>
            ) {
                if (response.isSuccessful) {
                    ErrorHandler.toaster(this@DeleteAccountActivity,"Hitelesítő kód elküldve!")
                    buttonRequest.visibility = View.GONE
                    text.visibility = View.GONE

                    inputEmail.visibility = View.VISIBLE
                    inputCode.visibility = View.VISIBLE
                    inputPassword.visibility = View.VISIBLE
                    emailInputLayout.visibility = View.VISIBLE
                    codeInputLayout.visibility = View.VISIBLE
                    passwordInputLayout.visibility = View.VISIBLE

                    buttonConfirm.visibility = View.VISIBLE

                    passwordText.visibility = View.VISIBLE
                    emailText.visibility = View.VISIBLE
                    codeText.visibility = View.VISIBLE

                } else {
                     ErrorHandler.handleApiError(this@DeleteAccountActivity, null, response.message())
                }
            }

            override fun onFailure(call: Call<ApiResponseGeneric>, t: Throwable) {
                ErrorHandler.handleNetworkError(this@DeleteAccountActivity, t)
            }
        })
    }

    private fun deleteUserConfirm(code: String, email: String, password: String){
        val confirmRequest = ConfirmDeleteAccount(UserSession.getUserId()!!, password, email, code)
        apiService.confirmDeleteUser(confirmRequest).enqueue(object : Callback<ApiResponseGeneric> {
            override fun onResponse(call: Call<ApiResponseGeneric>, response: Response<ApiResponseGeneric>) {
                if (response.isSuccessful) {

                    ErrorHandler.toaster(this@DeleteAccountActivity,"Sikeresen törölve!")
                    //Token törlés
                    val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                    val editor = prefs.edit()
                    editor.clear()
                    editor.apply()

                    //Usersession törlés
                    UserSession.clear()

                    //Vissza irányítás a loginhoz.
                    val intent = Intent(this@DeleteAccountActivity, LogActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK  //clear the backstack
                    }
                    startActivity(intent)
                    finish()
                } else {
                    ErrorHandler.handleApiError(this@DeleteAccountActivity, null, response.message())
                }
            }

            override fun onFailure(call: Call<ApiResponseGeneric>, t: Throwable) {
                ErrorHandler.handleNetworkError(this@DeleteAccountActivity,t)
            }
        })
    }


    private fun setupViewItems() {
        inputEmail = findViewById(R.id.inputEmail)
        inputCode = findViewById(R.id.inputCode)
        inputPassword = findViewById(R.id.inputPassword)

        buttonCancel = findViewById(R.id.buttonCancel)
        buttonConfirm = findViewById(R.id.buttonConfirm)
        buttonRequest = findViewById(R.id.buttonRequest)

        text = findViewById(R.id.text)
        passwordText = findViewById(R.id.passwordText)
        emailText = findViewById(R.id.emailText)
        codeText = findViewById(R.id.codeText)

        emailInputLayout = findViewById(R.id.emailInputLayout)
        codeInputLayout = findViewById(R.id.codeInputLayout)
        passwordInputLayout = findViewById(R.id.passwordInputLayout)
    }

    private fun setupRetrofit() {
        prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        RetrofitClient.init(applicationContext)
        apiService = RetrofitClient.apiService
    }

}