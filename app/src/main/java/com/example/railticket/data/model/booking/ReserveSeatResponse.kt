package com.example.railticket.data.model.booking

// Assuming ReserveSeatData will be in its own file or defined elsewhere if still needed as a nested type.
// For now, let's assume ReserveSeatData will also be a top-level class in its own file.
data class ReserveSeatResponse(
    val data: ReserveSeatData? = null,
    val extra: Map<String, String>? = null
)
