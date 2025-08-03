package com.example.usedpalace.dataClasses

import com.google.gson.annotations.SerializedName

data class DeletedSaleWithEverything(
    @SerializedName("Id") val id: Int,
    @SerializedName("Sid") val sid: Int,
    @SerializedName("Uid") val uid: Int,
    @SerializedName("DeletedAt") val deletedAt: String
    )
