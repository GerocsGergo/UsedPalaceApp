package com.example.usedpalace.adminMenus.requests

data class UserDeleteRequest(
    val adminId: Int,
    val targetUserId: Int
)
