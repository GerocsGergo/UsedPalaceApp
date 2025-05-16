package com.example.usedpalace.fragments.messagesHelpers.responses

import com.example.usedpalace.fragments.messagesHelpers.MessageWithEverything
import com.google.gson.annotations.SerializedName

data class MessageResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("messageId") val messageId: Int
)
