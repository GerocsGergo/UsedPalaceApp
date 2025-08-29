package com.example.usedpalace.responses

import com.google.gson.annotations.SerializedName

data class UsernameResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("Fullname") val fullname: String?
)
