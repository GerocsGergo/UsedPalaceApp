package com.example.usedpalace.requests

data class ConfirmPhoneNumberChangeRequest(
    val userId: Int,
    val phoneNumber: String
)
