package com.example.usedpalace.fragments.ChatAndMessages

import com.google.gson.annotations.SerializedName

data class ChatItem(
    @SerializedName("ChatID") val chatId: Int,
    @SerializedName("SellerID") val sellerId: Int,
    @SerializedName("BuyerID") val buyerId: Int, //Same as User id
    @SerializedName("SaleID") val saleId: Int,
    @SerializedName("CreatedAt") val createdAt: String,
    @SerializedName("LastMessageAt") val lastMessageAt: String,
    @SerializedName("unreadCount") val unreadCount: Int = 0,
    @SerializedName("username") val username: String? = null,
)
