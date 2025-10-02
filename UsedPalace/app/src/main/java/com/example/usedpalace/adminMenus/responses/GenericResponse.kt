package com.example.usedpalace.adminMenus.responses

data class GenericResponse(
    val success: Boolean,
    val message: String,
    val error: String? = null // csak hiba esetén lehet kitöltve
)
