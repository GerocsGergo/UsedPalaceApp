package com.example.usedpalace.responses

import com.example.usedpalace.Sale
import com.google.gson.annotations.SerializedName

data class ApiResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<Sale>
)
