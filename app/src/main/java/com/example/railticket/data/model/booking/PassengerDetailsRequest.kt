package com.example.railticket.data.model.booking

import com.google.gson.annotations.SerializedName

data class PassengerDetailsRequest(
    @SerializedName("trip_id") val tripId: Long,
    @SerializedName("trip_route_id") val tripRouteId: Long,
    @SerializedName("ticket_ids") val ticketIds: List<Long>,
    @SerializedName("hash") val hash: String? // Added hash field
)
