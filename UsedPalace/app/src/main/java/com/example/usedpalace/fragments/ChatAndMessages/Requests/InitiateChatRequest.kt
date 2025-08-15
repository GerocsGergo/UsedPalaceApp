package com.example.usedpalace.fragments.ChatAndMessages.Requests

data class InitiateChatRequest(
    val sellerId: Int,
    val buyerId: Int,
    val saleId: Int
)
