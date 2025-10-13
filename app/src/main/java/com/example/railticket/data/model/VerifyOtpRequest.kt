package com.example.railticket.data.model

import com.google.gson.annotations.SerializedName

data class VerifyOtpRequest(
    @SerializedName("trip_id")
    val tripId: Long, // Changed from String to Long

    @SerializedName("trip_route_id")
    val tripRouteId: Long, // Changed from String to Long

    @SerializedName("ticket_ids")
    val ticketIds: List<Long>, // Changed from List<String> to List<Long>

    val otp: String 
)
