package com.example.usedpalace.requests

data class ChangePasswordRequest(
    val userId: Int,
    val oldPassword: String,
    val newPassword: String
)
