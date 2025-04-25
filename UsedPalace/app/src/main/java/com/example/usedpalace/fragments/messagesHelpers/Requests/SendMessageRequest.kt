package com.example.usedpalace.fragments.messagesHelpers.Requests

data class SendMessageRequest(
    val chatId: Int,
    val senderId: Int,
    val content: String
)
