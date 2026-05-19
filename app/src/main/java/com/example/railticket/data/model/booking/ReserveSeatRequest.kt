package com.example.railticket.data.model.booking

import com.google.gson.annotations.SerializedName

data class ReserveSeatRequest(
    @SerializedName("ticket_id") val ticketId: Long,
    @SerializedName("route_id") val routeId: Long // Assuming route_id is always a Long
)
