package com.example.usedpalace.requests

data class SaveFcmTokenRequest(
    val userId: Int,
    val fcmToken: String
)
