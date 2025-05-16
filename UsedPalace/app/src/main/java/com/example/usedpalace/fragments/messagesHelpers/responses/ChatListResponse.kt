package com.example.usedpalace.fragments.messagesHelpers.responses

import com.example.usedpalace.fragments.messagesHelpers.ChatItem
import com.google.gson.annotations.SerializedName

data class ChatListResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<ChatItem>
)
