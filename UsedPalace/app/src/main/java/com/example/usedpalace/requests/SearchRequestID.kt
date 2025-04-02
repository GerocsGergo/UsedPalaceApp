package com.example.usedpalace.requests

import com.google.gson.annotations.SerializedName

data class SearchRequestID(
    @SerializedName("searchParam")
    val searchParam: Int
)
