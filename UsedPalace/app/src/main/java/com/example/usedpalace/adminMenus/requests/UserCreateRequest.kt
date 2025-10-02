package com.example.usedpalace.adminMenus.requests

data class UserCreateRequest(
    val adminId: Int,
    val fullname: String,
    val email: String,
    val password: String,
    val phoneNumber: String,
    val isAdmin: Boolean = false,
    val isVerified: Boolean = false
)
