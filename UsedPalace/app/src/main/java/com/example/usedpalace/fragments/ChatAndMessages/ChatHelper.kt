package com.example.usedpalace.fragments.ChatAndMessages

import android.util.Log
import com.example.usedpalace.ErrorHandler
import com.example.usedpalace.requests.SearchRequestID
import network.ApiService
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object ChatHelper {

    private val huLocale = Locale("hu", "HU")


    fun formatMessageTime(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return "Just now"

        return try {
            val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
            //Log.d("FormatMessageTime", "date: " + dateString)
            ErrorHandler.logToLogcat("FormatMessageTime", "date: $dateString")
            val messageTime = ZonedDateTime.parse(dateString, inputFormatter).withZoneSameInstant(ZoneId.systemDefault())

            val now = ZonedDateTime.now(ZoneId.systemDefault())
            val diffMillis = java.time.Duration.between(messageTime, now).toMillis()

            when {
                diffMillis < 60 * 1000 -> "Just now"
                diffMillis < 24 * 60 * 60 * 1000 -> {
                    val formatter = DateTimeFormatter.ofPattern("HH:mm", huLocale)
                    messageTime.format(formatter)
                    messageTime.plusHours(2).format(formatter)
                }
                else -> {
                    val formatter = DateTimeFormatter.ofPattern("yyyy MMM dd, HH:mm", huLocale)
                    messageTime.format(formatter)
                    messageTime.plusHours(2).format(formatter) //hozzá kellett adni 2 órát mert az idő zonákkal valami nem jó
                }
            }
        } catch (e: Exception) {
            //Log.e("DateHelper", "Failed to parse date string: $dateString", e)
            ErrorHandler.logToLogcat("DateHelper", "Failed to parse date string: $dateString", ErrorHandler.LogLevel.ERROR)
            "Just now"
        }
    }



    fun formatDateString(dateString: String): String {
        return try {
            val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX", huLocale)
            val outputFormatter = DateTimeFormatter.ofPattern("yyyy MMM dd, HH:mm", huLocale)

            //Log.d("formatDateString", "date: " + dateString)
            ErrorHandler.logToLogcat("formatDateString", "date: $dateString")
            val dateTime = LocalDateTime.parse(dateString, inputFormatter)
            dateTime.format(outputFormatter)
            dateTime.plusHours(2).format(outputFormatter)
        } catch (e: Exception) {
            //Log.e("DateUtils", "Failed to parse date: $dateString", e)
            ErrorHandler.logToLogcat("DateUtils", "Failed to parse date: $dateString", ErrorHandler.LogLevel.ERROR)
            dateString
        }
    }

    suspend fun getUserName(apiService: ApiService, userId: Int): String? {
        return try {
            val response = apiService.searchUsername(SearchRequestID(userId))
            if (response.success && response.fullname != null) {
                response.fullname
            } else {
                //Log.e("Search", "Username not found: ${response.message}")
                ErrorHandler.logToLogcat("Search", "Username not found: ${response.message}", ErrorHandler.LogLevel.ERROR)
                null
            }
        } catch (e: Exception) {
            //Log.e("Search", "Error fetching username", e)
            ErrorHandler.logToLogcat("Search", "Error fetching username", ErrorHandler.LogLevel.ERROR)
            null
        }
    }
}