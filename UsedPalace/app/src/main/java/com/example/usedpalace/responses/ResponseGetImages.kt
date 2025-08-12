package com.example.usedpalace.responses

data class ResponseGetImages(
    val success: Boolean,
    val message: String,
    val images: List<String>
)

