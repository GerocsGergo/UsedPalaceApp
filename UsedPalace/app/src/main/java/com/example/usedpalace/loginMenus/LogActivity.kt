package com.example.usedpalace.loginMenus

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
import com.example.usedpalace.MainMenuActivity
import com.example.usedpalace.R
import network.RetrofitClient
import network.RetrofitClientNoAuth
import com.example.usedpalace.UserSession
import com.example.usedpalace.requests.NewLogin
import com.example.usedpalace.responses.ResponseForLoginTokenExpiration
import com.example.usedpalace.responses.ResponseMessage
import com.example.usedpalace.responses.ResponseMessageWithUser
import com.google.android.material.textfield.TextInputLayout
import network.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class LogActivity : AppCompatActivity() {

    private lateinit var apiServiceNoAuth: ApiService
    private lateinit var apiServiceAuth: ApiService
    private lateinit var prefs: SharedPreferences
    private var token: String? = null
    private lateinit var deviceInfo: String
    private var logout: Boolean = false //Akkor kell ha a profil menüből jelentkezik ki a felhasználó

    private lateinit var inputEmail: EditText
    private lateinit var inputEmailLayout :TextInputLayout
    private lateinit var textEmail :TextView

    private lateinit var inputPassword :EditText
    private lateinit var inputPasswordLayout :TextInputLayout
    private lateinit var textPassword :TextView

    private lateinit var welcomeText :TextView

    private lateinit var btnLogin :Button
    private lateinit var btnOpenMainMenu :Button
    private lateinit var btnLogout :Button
    private lateinit var btnForget :Button
    private lateinit var btnReg :Button

    private lateinit var buttonVerifyEmail :Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupViews()
        initialize()
        setupClickListeners()
        logoutFromProfile()


        if (token != null) {
            apiServiceAuth.verifyToken("Bearer $token").enqueue(object : Callback<ResponseForLoginTokenExpiration> {
                override fun onResponse(call: Call<ResponseForLoginTokenExpiration>, response: Response<ResponseForLoginTokenExpiration>) {
                    if (response.isSuccessful) {
                        ErrorHandler.logToLogcat("JWTCheck", "Token is valid", ErrorHandler.LogLevel.DEBUG)

                        val savedUserName = prefs.getString("userName", null)
                        val savedUserId = prefs.getInt("userId", -1)
                        val savedIsAdmin = prefs.getBoolean("isAdmin", false)

                        // Set it back into UserSession
                        UserSession.setUserData(
                            id = savedUserId,
                            name = savedUserName?: "",
                            isAdmin = savedIsAdmin
                        )

                        // Token exists: show only the two buttons
                        btnLogout.visibility = View.VISIBLE
                        btnOpenMainMenu.visibility = View.VISIBLE
                        welcomeText.visibility = View.VISIBLE
                        welcomeText.text = "Üdvözlöm, " + savedUserName + " !"


                        btnLogin.visibility = View.GONE
                        textPassword.visibility = View.GONE
                        inputPassword.visibility = View.GONE
                        inputPasswordLayout.visibility = View.GONE
                        textEmail.visibility = View.GONE
                        inputEmail.visibility = View.GONE
                        inputEmailLayout.visibility = View.GONE
                        btnForget.visibility = View.GONE
                        btnReg.visibility = View.GONE
                        buttonVerifyEmail.visibility = View.GONE
                    } else {
                         ErrorHandler.logToLogcat("JWTCheck", "Token is invalid or expired", ErrorHandler.LogLevel.DEBUG)
                        val errorBody = response.errorBody()?.string()
                        ErrorHandler.handleApiError(this@LogActivity, response.code(), errorBody)
                        // No token: show login inputs
                        btnLogout.visibility = View.GONE
                        btnOpenMainMenu.visibility = View.GONE
                        welcomeText.visibility = View.GONE
                        welcomeText.text = ""

                        btnLogin.visibility = View.VISIBLE
                        textPassword.visibility = View.VISIBLE
                        inputPassword.visibility = View.VISIBLE
                        inputPasswordLayout.visibility = View.VISIBLE
                        inputEmailLayout.visibility = View.VISIBLE
                        textEmail.visibility = View.VISIBLE
                        inputEmail.visibility = View.VISIBLE
                        buttonVerifyEmail.visibility = View.VISIBLE
                        val editor = prefs.edit()
                        editor.clear()
                        editor.apply()

                        clearSessionAndGoToLogin()

                    }
                }
                override fun onFailure(call: Call<ResponseForLoginTokenExpiration>, t: Throwable) {
                    ErrorHandler.handleNetworkError(this@LogActivity, t)
                }
            })
        }
    }


    private fun logoutFromProfile() {
        logout = intent.getBooleanExtra("forceLogout", false)
        if (logout) {
            logout()
        }
    }

    private fun setupClickListeners() {
        btnReg.setOnClickListener {
            val intent = Intent(this, RegActivity::class.java)
            startActivity(intent)
        }

        buttonVerifyEmail.setOnClickListener {
            val intent = Intent(this, EmailVerifyActivity::class.java)
            startActivity(intent)
        }

        btnForget.setOnClickListener {
            val intent = Intent(this, ForgottenPasswordActivity::class.java)
            startActivity(intent)
        }

        btnOpenMainMenu.setOnClickListener {
            ErrorHandler.logToLogcat("Login activity","Logged in user id: " + UserSession.getUserId(), ErrorHandler.LogLevel.DEBUG)
            val intent = Intent(this@LogActivity, MainMenuActivity::class.java)
            startActivity(intent)
        }

        btnLogout.setOnClickListener {
            logout()
        }

        btnLogin.setOnClickListener {
            val email = inputEmail.text.toString().trim()
            val password = inputPassword.text.toString().trim()

            // Validate email and password
            if (email.isEmpty() || password.isEmpty()) {
                ErrorHandler.toaster(this, "Kérjük töltse ki az összes mezőt!")
                return@setOnClickListener
            }

            val newLogin = NewLogin(
                email = email,
                password = password,
                deviceInfo = deviceInfo
            )

            // Send the login request to the server
            apiServiceNoAuth.loginUser(newLogin).enqueue(object : Callback<ResponseMessageWithUser> {
                override fun onResponse(call: Call<ResponseMessageWithUser>, response: Response<ResponseMessageWithUser>) {
                    if (response.isSuccessful) {
                        // Clear input fields
                        inputEmail.text.clear()
                        inputPassword.text.clear()

                        val userData = response.body()?.user
                        userData?.let {
                            // Store user data in singleton
                            UserSession.setUserData(
                                id = it.id,
                                name = it.name,
                                isAdmin = it.isAdmin
                            )
                        }

                        token = response.body()?.token
                        prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                        val editor = prefs.edit()
                        editor.putString("token", token)
                        editor.putString("userName", userData?.name)
                        editor.putInt("userId", userData?.id ?: -1)
                        editor.putBoolean("isAdmin", userData?.isAdmin ?: false)
                        editor.apply()

                        // Navigate to the main menu
                        ErrorHandler.logToLogcat("Login activity","Logged in user id: " + UserSession.getUserId(), ErrorHandler.LogLevel.DEBUG)
                        val intent = Intent(this@LogActivity, MainMenuActivity::class.java)
                        startActivity(intent)
                    } else {
                        val errorBody = response.errorBody()?.string()
                        ErrorHandler.handleApiError(this@LogActivity, response.code(), errorBody)

                    }
                }

                override fun onFailure(call: Call<ResponseMessageWithUser>, t: Throwable) {
                    // Handle network errors
                    ErrorHandler.handleNetworkError(this@LogActivity, t)
                }
            })
        }

    }

    private fun logout(){
        val token = prefs.getString("token", null)
        if (token != null) {
            apiServiceAuth.logoutUser("Bearer $token").enqueue(object : Callback<ResponseMessage> {
                override fun onResponse(call: Call<ResponseMessage>, response: Response<ResponseMessage>) {
                    if (response.isSuccessful) {
                         ErrorHandler.toaster(this@LogActivity, "Sikeres kijelentkezés")
                    } else {
                         val errorBody = response.errorBody()?.string()
                        ErrorHandler.handleApiError(this@LogActivity, response.code(), errorBody)
                    }
                    // Minden esetben töröljük a helyi adatokat és session-t
                    clearSessionAndGoToLogin()
                }

                override fun onFailure(call: Call<ResponseMessage>, t: Throwable) {
                      ErrorHandler.handleNetworkError(this@LogActivity, t)

                    clearSessionAndGoToLogin()
                }
            })
        } else {
            clearSessionAndGoToLogin()
        }
    }


    private fun setupViews() {
         inputEmail = findViewById(R.id.inputEmail)
         inputEmailLayout = findViewById(R.id.emailInputLayout)
         textEmail = findViewById(R.id.email)

         inputPassword = findViewById(R.id.inputPassword)
         inputPasswordLayout = findViewById(R.id.passwordInputLayout)
         textPassword = findViewById(R.id.password)

         welcomeText = findViewById(R.id.welcomeText)

         btnLogin = findViewById(R.id.buttonLogin)
         btnOpenMainMenu = findViewById(R.id.buttonOpenMainMenu)
         btnLogout =  findViewById(R.id.buttonLogout)
         btnForget =  findViewById(R.id.buttonForget)
         btnReg =  findViewById(R.id.buttonReg)

        buttonVerifyEmail = findViewById(R.id.buttonEmailVerification)
    }

    private fun initialize() {
        prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        token = prefs.getString("token", null)
        deviceInfo = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})"
        RetrofitClient.init(applicationContext)
        apiServiceNoAuth = RetrofitClientNoAuth.apiService
        apiServiceAuth = RetrofitClient.apiService
    }

    private fun clearSessionAndGoToLogin() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
        UserSession.clear()
        val intent = Intent(this@LogActivity, LogActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }


}