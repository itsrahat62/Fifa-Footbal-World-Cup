package com.example.railticket.data.model.booking

import com.google.gson.annotations.SerializedName

data class SeatLayoutData(
    // Fields for successful response
    val seatLayout: List<SeatLayoutFloor>? = null,
    val totalSeats: Int? = null,
    val reservationFee: Int? = null,
    val reservationFeeUnit: String? = null,

    // Fields for API-level error within a 200 OK response
    val error: Int? = null,
    @SerializedName("msg") // Assuming JSON field name is "msg"
    val message: String? = null
)
