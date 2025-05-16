package com.example.usedpalace.responses

data class ResponseForLoginTokenExpiration(
val valid: Boolean? = null,
val user: TokenUser? = null,
val error: String? = null
)

data class TokenUser(
    val id: String,
    val iat: Long,
    val exp: Long
)

