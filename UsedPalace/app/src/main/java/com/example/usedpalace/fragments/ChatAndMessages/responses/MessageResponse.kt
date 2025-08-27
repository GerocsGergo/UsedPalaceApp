package com.example.usedpalace.fragments.ChatAndMessages.responses

import com.google.gson.annotations.SerializedName

data class MessageResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("messageId") val messageId: Int,
    @SerializedName("sentAt") val sentAt: String,
    @SerializedName("isRead") val isRead: Boolean
)
