package com.example.railticket.data.model.booking

// Assuming SeatLayoutData will be in SeatLayoutDataNew.kt
data class SeatLayoutResponse(
    val data: SeatLayoutData? = null,
    val extra: Map<String, String>? = null
)
