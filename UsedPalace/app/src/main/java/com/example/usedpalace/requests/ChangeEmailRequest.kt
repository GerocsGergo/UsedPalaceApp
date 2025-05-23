package com.example.usedpalace.requests

data class ChangeEmailRequest(
    val userId: Int,
    val newEmail: String
)
