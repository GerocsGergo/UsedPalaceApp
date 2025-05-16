package com.example.usedpalace.requests

data class ForgotPasswordRequest(
    val email: String,
    val phoneNumber: String
)
