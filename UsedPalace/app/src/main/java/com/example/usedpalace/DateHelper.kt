package com.example.usedpalace

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateHelper {

    private val huLocale = Locale("hu", "HU")

    fun formatCreatedAt(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return "Just now"

        return try {
            // ISO formátum: 2025-09-30T14:21:43.000Z
            val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX", huLocale)
            val zonedDateTime = ZonedDateTime.parse(dateString, inputFormatter)
                .withZoneSameInstant(ZoneId.systemDefault())

            val now = ZonedDateTime.now(ZoneId.systemDefault())
            val diffMillis = java.time.Duration.between(zonedDateTime, now).toMillis()

            val outputFormatter = DateTimeFormatter.ofPattern("yyyy. MM. dd. HH:mm", huLocale)

            when {
                diffMillis < 60 * 1000 -> "Just now"
                diffMillis < 24 * 60 * 60 * 1000 -> zonedDateTime.format(outputFormatter)
                else -> zonedDateTime.format(outputFormatter)
            }
        } catch (e: Exception) {
            ErrorHandler.logToLogcat("DateHelper", "Failed to parse date: $dateString", ErrorHandler.LogLevel.ERROR)
            dateString
        }
    }

}
