package com.example.usedpalace.fragments.messagesHelpers.responses

import com.example.usedpalace.fragments.messagesHelpers.MessageWithEverything
import com.google.gson.annotations.SerializedName

data class ChatResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<MessageWithEverything>
)
