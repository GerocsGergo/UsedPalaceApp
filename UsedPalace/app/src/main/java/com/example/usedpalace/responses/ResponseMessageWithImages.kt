package com.example.usedpalace.responses

data class ResponseMessageWithImages(
    val success: Boolean,
    val message: String,
    val images: List<String>?
)
