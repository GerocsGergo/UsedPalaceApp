package com.example.usedpalace.requests

import com.google.gson.annotations.SerializedName

data class SearchRequestName(
    @SerializedName("searchParam") val searchParam: String,
    @SerializedName("min") val min: Int?,
    @SerializedName("max") val max: Int?,
)
