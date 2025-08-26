package com.example.usedpalace

import android.content.Context
import android.util.Log
import android.widget.Toast
import org.json.JSONException
import org.json.JSONObject

object ErrorHandler {


    //haha pirítós
    fun toaster(context: Context, message: String?, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(context, message ?: "Ismeretlen hiba történt", duration).show()
    }


    fun logToLogcat(tag: String, message: String, level: LogLevel = LogLevel.DEBUG, throwable: Throwable? = null) {
        when (level) {
            LogLevel.DEBUG -> Log.d(tag, message, throwable)
            LogLevel.INFO -> Log.i(tag, message, throwable)
            LogLevel.WARN -> Log.w(tag, message, throwable)
            LogLevel.ERROR -> Log.e(tag, message, throwable)
            LogLevel.VERBOSE -> Log.v(tag, message, throwable)
        }
    }

    enum class LogLevel {
        DEBUG, INFO, WARN, ERROR, VERBOSE
    }

    fun handleNetworkError(context: Context, t: Throwable?) {
        // Log the error for developer
        logToLogcat("NetworkError", "Network failure: ${t?.message}", LogLevel.ERROR)
        // Notify user
        toaster(context, "Hálózati hiba. Kérlek ellenőrizd az internetkapcsolatod.")
    }

    fun handleApiError(
        context: Context,
        statusCode: Int? = null,
        errorBody: String? = null
    ) {
        var userMessage = "Ismeretlen hiba történt"

        errorBody?.let {
            try {
                if (it.trim().startsWith("{")) { // ha JSON
                    val json = JSONObject(it)
                    userMessage = json.optString("message", userMessage)
                } else {
                    // egyszerű string
                    userMessage = it
                }
            } catch (e: JSONException) {
                e.printStackTrace()
                userMessage = it // fallback
            }
        }

        logToLogcat("ApiError", "Status: $statusCode, ErrorBody: $errorBody", LogLevel.ERROR)
        toaster(context, userMessage)
    }



}