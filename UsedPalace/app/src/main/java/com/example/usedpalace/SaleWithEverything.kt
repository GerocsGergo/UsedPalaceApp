package com.example.usedpalace

import com.google.gson.annotations.SerializedName

data class SaleWithEverything(

    @SerializedName("Name") val Name: String,
    @SerializedName("Cost") val Cost: Int,
    @SerializedName("Description") val Description: String,
    @SerializedName("SaleFolder") val SaleFolder: String,
    @SerializedName("BigCategory") val mainCategory: String,
    @SerializedName("SmallCategory") val subCategory: String,
    @SerializedName("Uid") val Uid: Int
)

