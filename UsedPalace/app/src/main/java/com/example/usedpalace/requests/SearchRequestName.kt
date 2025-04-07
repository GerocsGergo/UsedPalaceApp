package com.example.usedpalace.requests

import com.google.gson.annotations.SerializedName

data class SearchRequestName(
    @SerializedName("searchParam")
    val searchParam: String
)
