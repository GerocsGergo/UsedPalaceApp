package com.example.usedpalace.requests

data class ModifySaleRequest(
    val saleId: Int,
    val name: String,
    val description: String,
    val cost: Int,
    val bigCategory: String,
    val smallCategory: String? = null,
    val userId: Int?
)
