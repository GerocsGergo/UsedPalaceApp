package com.example.usedpalace.responses

import com.example.usedpalace.dataClasses.SaleWithSid

data class ApiResponseForLoadSales (
    val success: Boolean,
    val currentPage: Int,
    val totalPages: Int,
    val totalItems: Int,
    val data: List<SaleWithSid>,
    val message: String? = null
)
