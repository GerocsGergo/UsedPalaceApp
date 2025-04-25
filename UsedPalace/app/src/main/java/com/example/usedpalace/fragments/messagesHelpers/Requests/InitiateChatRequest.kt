package com.example.usedpalace.fragments.messagesHelpers.Requests

data class InitiateChatRequest(
    val sellerId: Int,
    val buyerId: Int,
    val saleId: Int
)
