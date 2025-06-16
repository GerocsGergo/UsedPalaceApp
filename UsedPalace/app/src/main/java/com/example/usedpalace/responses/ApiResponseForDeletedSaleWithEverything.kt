package com.example.usedpalace.responses

import com.example.usedpalace.DeletedSaleWithEverything
import com.google.gson.annotations.SerializedName

data class ApiResponseForDeletedSaleWithEverything(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: DeletedSaleWithEverything?
)
