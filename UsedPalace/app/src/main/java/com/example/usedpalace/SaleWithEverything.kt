package com.example.usedpalace

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class SaleWithEverything(

    @SerializedName("Name") val Name: String,
    @SerializedName("Cost") val Cost: Int,
    @SerializedName("Description") val Description: String,
    @SerializedName("SaleFolder") val SaleFolder: String,
    @SerializedName("BigCategory") val mainCategory: String,
    @SerializedName("SmallCategory") val subCategory: String
)

