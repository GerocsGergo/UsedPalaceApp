package com.example.usedpalace.fragments.messageHelpers

import com.example.usedpalace.SaleWithSid
import com.google.gson.annotations.SerializedName

data class ChatListResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<ChatItem>
)
