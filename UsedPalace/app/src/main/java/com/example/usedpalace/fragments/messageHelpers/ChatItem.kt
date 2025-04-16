package com.example.usedpalace.fragments.messageHelpers

import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime

data class ChatItem(
    @SerializedName("ChatId") val chatId: Int,
    @SerializedName("SellerId") val sellerId: Int,
    @SerializedName("BuyerId") val buyerId: Int, //Same as User id
    @SerializedName("SaleId") val saleId: Int,
    @SerializedName("CreatedAt") val createdAt: String,
    @SerializedName("LastMessageAt") val lastMessageAt: String

)
