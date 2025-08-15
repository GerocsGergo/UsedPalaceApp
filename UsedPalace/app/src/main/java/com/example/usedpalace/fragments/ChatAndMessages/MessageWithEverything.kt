package com.example.usedpalace.fragments.ChatAndMessages

import com.google.gson.annotations.SerializedName

data class MessageWithEverything(
    @SerializedName("MessageID") val messageId: Int,
    @SerializedName("ChatID") val chatId: Int,
    @SerializedName("SenderID") val senderId: Int,
    @SerializedName("Content") val content: String,
    @SerializedName("SentAt") val sentAt: String,

    )



