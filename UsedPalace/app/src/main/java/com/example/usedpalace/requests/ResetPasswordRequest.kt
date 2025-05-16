package com.example.usedpalace.requests

data class ResetPasswordRequest(
    val email: String,
    val code: String,
    val newPassword: String
)
