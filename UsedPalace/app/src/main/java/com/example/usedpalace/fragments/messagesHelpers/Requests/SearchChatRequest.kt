package com.example.usedpalace.fragments.messagesHelpers.Requests

import com.google.gson.annotations.SerializedName

data class SearchChatRequest(
    @SerializedName("userId") val userId: Int
)
