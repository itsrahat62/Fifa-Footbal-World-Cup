package com.example.railticket.data.model.booking

data class Seat(
    val isHidden: Boolean? = null,
    val seat_availability: Int? = null, // 0 for unavailable/booked, 1 for available, 2 for some other state (e.g., ladies seat)
    val seat_number: String? = null,
    val ticket_id: Long? = null,
    val ticket_type: Int? = null
)
