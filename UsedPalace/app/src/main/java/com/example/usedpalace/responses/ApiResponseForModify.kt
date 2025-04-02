package com.example.usedpalace.responses

import com.example.usedpalace.SaleWithEverything
import com.google.gson.annotations.SerializedName

data class ApiResponseForModify(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: SaleWithEverything?
)
