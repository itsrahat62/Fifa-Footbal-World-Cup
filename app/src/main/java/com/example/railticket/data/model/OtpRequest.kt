package com.example.railticket.data.model

data class OtpRequest(
    val tripId: String,
    val tripRouteId: String,
    val ticketIds: List<String>,
    val boardingPointId: String
)
