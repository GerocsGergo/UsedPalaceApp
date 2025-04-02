package com.example.usedpalace.responses

data class ResponseMessageWithFolder(
    val success: Boolean,
    val message: String,
    val saleId: Int?,
    val saleFolder: String?
)
