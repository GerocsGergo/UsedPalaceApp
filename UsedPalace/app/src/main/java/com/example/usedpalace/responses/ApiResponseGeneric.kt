package com.example.usedpalace.responses

import com.google.gson.annotations.SerializedName

data class ApiResponseGeneric(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String
)
