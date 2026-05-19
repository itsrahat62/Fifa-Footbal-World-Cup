package com.example.railticket.data.model.booking

import com.google.gson.annotations.SerializedName

data class VerifyOtpRequest(
    @SerializedName("trip_id") val tripId: Long,
    @SerializedName("trip_route_id") val tripRouteId: Long,
    @SerializedName("ticket_ids") val ticketIds: List<Long>,
    @SerializedName("otp") val otp: String
)
