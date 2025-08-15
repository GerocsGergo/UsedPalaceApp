package com.example.usedpalace.fragments.ChatAndMessages.responses

data class InitiateChatResponse(
    val success: Boolean,
    val chatId: Int? = null,
    val isNew: Boolean = false,
    val message: String? = null
)
