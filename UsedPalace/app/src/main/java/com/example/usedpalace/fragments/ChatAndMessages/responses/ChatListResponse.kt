package com.example.usedpalace.fragments.ChatAndMessages.responses

import com.example.usedpalace.fragments.ChatAndMessages.ChatItem
import com.google.gson.annotations.SerializedName

data class ChatListResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<ChatItem>
)
