package com.example.usedpalace.fragments.messagesHelpers

import java.util.Date

data class MessageWithEverything(
    val messageId: Int,          // Unique ID of the message
    val chatId: Int,             // ID of the chat this message belongs to
    val senderId: Int,           // ID of the user who sent the message
    val messageText: String,     // The actual message content
    val sentAt: Date,            // When the message was sent

){
    fun isSentByMe(currentUserId: Int): Boolean {
        return senderId == currentUserId
    }
}


