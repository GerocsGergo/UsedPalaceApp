package com.example.usedpalace.responses

import com.example.usedpalace.SaleWithSid
import com.google.gson.annotations.SerializedName

data class ApiResponseForSaleWithSid(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<SaleWithSid>
)
