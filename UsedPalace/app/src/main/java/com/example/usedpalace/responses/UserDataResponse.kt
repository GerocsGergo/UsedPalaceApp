package com.example.usedpalace.responses

import com.google.gson.annotations.SerializedName

data class UserDataResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("error") val error: String?,
    @SerializedName("data") val data: UserData?
)

data class UserData(
    @SerializedName("email") val email: String,
    @SerializedName("fullname") val fullname: String,
    @SerializedName("phoneNumber") val phoneNumber: String
)
