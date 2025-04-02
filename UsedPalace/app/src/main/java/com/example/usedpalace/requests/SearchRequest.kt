package com.example.usedpalace.requests

import com.google.gson.annotations.SerializedName

data class SearchRequest(
    @SerializedName("searchParam")
    val searchParam: String
)
