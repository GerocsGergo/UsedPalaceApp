package com.example.usedpalace.requests

data class DeleteImagesRequest(
    val saleFolder: String,
    val imageIndexes: List<Int>
)

