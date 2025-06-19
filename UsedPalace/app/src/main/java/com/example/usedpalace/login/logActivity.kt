package com.example.usedpalace.login

import android.content.Intent
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
import com.example.usedpalace.MainMenuActivity
import com.example.usedpalace.R
import com.example.usedpalace.UserSession
import com.example.usedpalace.requests.NewLogin
import com.example.usedpalace.responses.ResponseForLoginTokenExpiration
import com.example.usedpalace.responses.ResponseMessageWithUser
import network.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class LogActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }



        // Initialize Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/") // Use 10.0.2.2 for localhost in Android emulator
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // Create an instance of the API service
        val apiService = retrofit.create(ApiService::class.java)


        var prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        var token = prefs.getString("token", null)

        val inputEmail = findViewById<EditText>(R.id.inputEmail)
        val textEmail = findViewById<TextView>(R.id.email)

        val inputPassword = findViewById<EditText>(R.id.inputPassword)
        val textPassword = findViewById<TextView>(R.id.password)

        val welcomeText = findViewById<TextView>(R.id.welcomeText)

        val btnLogin = findViewById<Button>(R.id.buttonLogin)
        val btnOpenMainMenu = findViewById<Button>(R.id.buttonOpenMainMenu)
        val btnLogout =  findViewById<Button>(R.id.buttonLogout)
        val btnForget =  findViewById<Button>(R.id.buttonForget)
        val btnReg =  findViewById<Button>(R.id.buttonReg)

        if (token != null) {
            apiService.verifyToken("Bearer $token").enqueue(object : Callback<ResponseForLoginTokenExpiration> {
                override fun onResponse(call: Call<ResponseForLoginTokenExpiration>, response: Response<ResponseForLoginTokenExpiration>) {
                    if (response.isSuccessful) {
                        Log.d("JWTCheck", "Token is valid")

                        val savedUserName = prefs.getString("userName", null)
                        val savedUserId = prefs.getInt("userId", -1)

                        // Set it back into UserSession
                        UserSession.setUserData(
                            id = savedUserId,
                            name = savedUserName?: ""
                        )

                        // Token exists: show only the two buttons
                        btnLogout.visibility = View.VISIBLE
                        btnOpenMainMenu.visibility = View.VISIBLE
                        welcomeText.visibility = View.VISIBLE
                        welcomeText.text = "Üdvözlöm, " + savedUserName + " !"


                        btnLogin.visibility = View.GONE
                        textPassword.visibility = View.GONE
                        inputPassword.visibility = View.GONE
                        textEmail.visibility = View.GONE
                        inputEmail.visibility = View.GONE
                        btnForget.visibility = View.GONE
                        btnReg.visibility = View.GONE
                    } else {
                        Log.d("JWTCheck", "Token is invalid or expired")
                        // No token: show login inputs
                        btnLogout.visibility = View.GONE
                        btnOpenMainMenu.visibility = View.GONE
                        welcomeText.visibility = View.GONE
                        welcomeText.text = ""

                        btnLogin.visibility = View.VISIBLE
                        textPassword.visibility = View.VISIBLE
                        inputPassword.visibility = View.VISIBLE
                        textEmail.visibility = View.VISIBLE
                        inputEmail.visibility = View.VISIBLE
                        val editor = prefs.edit()
                        editor.clear()
                        editor.apply()

                        val intent = Intent(this@LogActivity, LogActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
                override fun onFailure(call: Call<ResponseForLoginTokenExpiration>, t: Throwable) {
                    Log.e("JWTCheck", "Network error: ${t.message}")
                }
            })
        }

        btnReg.setOnClickListener {
            val intent = Intent(this, RegActivity::class.java)
            startActivity(intent)
        }

        btnForget.setOnClickListener {
            val intent = Intent(this, ForgottenPasswordActivity::class.java)
            startActivity(intent)
        }

        btnOpenMainMenu.setOnClickListener {
            Log.d("Login activity","Logged in user id: " + UserSession.getUserId())
            val intent = Intent(this@LogActivity, MainMenuActivity::class.java)
            startActivity(intent)
        }

        btnLogout.setOnClickListener {
            prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
            val editor = prefs.edit()
            editor.clear()
            editor.apply()
            UserSession.clear()

            // Open login screen again
            val intent = Intent(this@LogActivity, LogActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK  //clear the backstack
            }
            startActivity(intent)
            finish()
        }


        btnLogin.setOnClickListener {
            val email = inputEmail.text.toString().trim()
            val password = inputPassword.text.toString().trim()

            // Validate email and password
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newLogin = NewLogin(
                email = email,
                password = password
            )

            // Send the login request to the server
            apiService.loginUser(newLogin).enqueue(object : Callback<ResponseMessageWithUser> {
                override fun onResponse(call: Call<ResponseMessageWithUser>, response: Response<ResponseMessageWithUser>) {
                    if (response.isSuccessful) {
                        val message = response.body()?.message
                        Toast.makeText(this@LogActivity, message, Toast.LENGTH_SHORT).show()

                        // Clear input fields
                        inputEmail.text.clear()
                        inputPassword.text.clear()

                        val userData = response.body()?.user
                        userData?.let {
                            // Store user data in singleton
                            UserSession.setUserData(
                                id = it.id,
                                name = it.name
                            )
                        }

                        token = response.body()?.token
                        prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                        val editor = prefs.edit()
                        editor.putString("token", token)
                        editor.putString("userName", userData?.name)
                        editor.putInt("userId", userData?.id ?: -1)
                        editor.apply()

                        // Navigate to the main menu
                        Log.d("Login activity","Logged in user id: " + UserSession.getUserId())
                        val intent = Intent(this@LogActivity, MainMenuActivity::class.java)
                        startActivity(intent)
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val statusCode = response.code()
                        val headers = response.headers().toString()

                        println("Status Code: $statusCode")
                        println("Headers: $headers")
                        println("Error Response: $errorBody")

                        Toast.makeText(this@LogActivity, "Login failed: $errorBody", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseMessageWithUser>, t: Throwable) {
                    // Handle network errors
                    Toast.makeText(this@LogActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

    }
}