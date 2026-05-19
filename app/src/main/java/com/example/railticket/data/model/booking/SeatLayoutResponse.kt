package com.example.railticket.data.model.booking

import com.google.gson.annotations.SerializedName

data class SeatLayoutResponse(
    @SerializedName("data") val data: SeatLayoutData?
)
