package com.example.usedpalace.requests

data class DeleteImagesRequest(
    val saleFolder: String,
    val imageNames: List<String>
)


