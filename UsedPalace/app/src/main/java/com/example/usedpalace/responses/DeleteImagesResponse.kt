package com.example.usedpalace.responses

data class DeleteImagesResponse(
    val success: Boolean,
    val message: String,
    val deletedImages: List<String> // pl. ["image4.jpg", "image5.jpg"]
)

