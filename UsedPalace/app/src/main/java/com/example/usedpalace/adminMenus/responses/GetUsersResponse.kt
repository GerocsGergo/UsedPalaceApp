package com.example.usedpalace.adminMenus.responses

data class GetUsersResponse(
    val success: Boolean,
    val currentPage: Int,
    val totalPages: Int,
    val totalItems: Int,
    val data: List<UserData>
) {
    data class UserData(
        val id: Int,
        val name: String,
        val isAdmin: Boolean
    )
}
