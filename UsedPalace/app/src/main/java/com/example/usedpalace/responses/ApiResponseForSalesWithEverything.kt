package com.example.usedpalace.responses

import com.example.usedpalace.dataClasses.SaleWithEverything
import com.google.gson.annotations.SerializedName

data class ApiResponseForSalesWithEverything(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: SaleWithEverything?
)
