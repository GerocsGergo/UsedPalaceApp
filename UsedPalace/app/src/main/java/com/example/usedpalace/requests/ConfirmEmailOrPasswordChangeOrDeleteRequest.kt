package com.example.usedpalace.requests

data class ConfirmEmailOrPasswordChangeOrDeleteRequest(
    val userId: Int,
    val code: String
)
