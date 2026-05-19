package com.example.railticket.data.model.otp

import com.google.gson.annotations.SerializedName

data class OtpVerificationRequest(
    @SerializedName("trip_id")
    val tripId: String, // Or Int/Long if applicable

    @SerializedName("trip_route_id")
    val tripRouteId: String, // Or Int/Long if applicable

    @SerializedName("ticket_ids")
    val ticketIds: List<String>, // Assuming a list of String IDs

    @SerializedName("otp")
    val otp: String
)