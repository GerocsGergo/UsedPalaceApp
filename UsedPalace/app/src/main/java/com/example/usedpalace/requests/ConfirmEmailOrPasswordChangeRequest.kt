package com.example.usedpalace.requests

data class ConfirmEmailOrPasswordChangeRequest(
    val userId: Int,
    val code: String
)
