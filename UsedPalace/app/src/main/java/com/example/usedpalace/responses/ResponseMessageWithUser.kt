package com.example.usedpalace.responses

data class ResponseMessageWithUser(
    val message: String,
    val token: String,
    val user: UserData

)
{
    data class UserData(
        val id: Int,
        val name: String
    )
}
