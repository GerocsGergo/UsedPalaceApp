package com.example.usedpalace.adminMenus.requests

data class UserModifyRequest(
    val adminId: Int,
    val userId: Int,
    val fullname: String?,
    val email: String?,
    val password: String?, // üres, ha nem módosítjuk
    val phoneNumber: String?,
    val isAdmin: Boolean,
    val isVerified: Boolean
)
