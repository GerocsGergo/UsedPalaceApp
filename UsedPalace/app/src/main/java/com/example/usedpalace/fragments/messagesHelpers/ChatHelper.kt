package com.example.usedpalace.fragments.messagesHelpers

import android.util.Log
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
            Log.e("DateHelper", "Failed to parse date string: $dateString", e)
            "Just now"
        }
    }



    fun formatDateString(dateString: String): String {
        return try {
            val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX", huLocale)
            val outputFormatter = DateTimeFormatter.ofPattern("yyyy MMM dd, HH:mm", huLocale)
            val dateTime = LocalDateTime.parse(dateString, inputFormatter)
            dateTime.format(outputFormatter)
        } catch (e: Exception) {
            Log.e("DateUtils", "Failed to parse date: $dateString", e)
            dateString
        }
    }

    suspend fun getUserName(apiService: ApiService, userId: Int): String? {
        return try {
            val response = apiService.searchUsername(SearchRequestID(userId))
            if (response.success && response.fullname != null) {
                response.fullname
            } else {
                Log.e("Search", "Username not found: ${response.message}")
                null
            }
        } catch (e: Exception) {
            Log.e("Search", "Error fetching username", e)
            null
        }
    }
}