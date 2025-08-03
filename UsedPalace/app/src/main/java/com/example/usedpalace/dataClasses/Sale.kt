package com.example.usedpalace.dataClasses

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class Sale(

@SerializedName("Name") val Name: String,
@SerializedName("Cost") val Cost: Int,
@SerializedName("Description") val Description: String,
@SerializedName("SaleFolder") val SaleFolder: String

) : Parcelable {

    // Constructor that reads from a Parcel
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "", // Handle null case with empty string
        parcel.readInt(),
        parcel.readString() ?: "", // Handle null case with empty string
        parcel.readString() ?: ""  // Handle null case with empty string
    )

    // Write object values to Parcel
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(Name)
        parcel.writeInt(Cost)
        parcel.writeString(Description)
        parcel.writeString(SaleFolder)
    }

    // Describes special objects (not needed in most cases)
    override fun describeContents(): Int {
        return 0
    }

    // CREATOR field that generates instances of your Parcelable class
    companion object CREATOR : Parcelable.Creator<Sale> {
        override fun createFromParcel(parcel: Parcel): Sale {
            return Sale(parcel)
        }

        override fun newArray(size: Int): Array<Sale?> {
            return arrayOfNulls(size)
        }
    }
}
