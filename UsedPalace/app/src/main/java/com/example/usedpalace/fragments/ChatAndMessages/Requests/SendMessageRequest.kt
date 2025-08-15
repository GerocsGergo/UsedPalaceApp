package com.example.usedpalace.fragments.ChatAndMessages.Requests

data class SendMessageRequest(
    val chatId: Int,
    val senderId: Int,
    val content: String
)
