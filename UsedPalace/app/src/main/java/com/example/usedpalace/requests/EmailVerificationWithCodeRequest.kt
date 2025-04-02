package com.example.usedpalace.requests

data class EmailVerificationWithCodeRequest(
    val email: String,
    val code: String
)
