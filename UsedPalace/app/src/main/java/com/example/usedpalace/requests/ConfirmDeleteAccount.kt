package com.example.usedpalace.requests

data class ConfirmDeleteAccount(
    val userId: Int,
    val password: String,
    val email: String,
    val code: String,

)
