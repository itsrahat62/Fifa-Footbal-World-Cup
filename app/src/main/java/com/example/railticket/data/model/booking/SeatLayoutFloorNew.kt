package com.example.railticket.data.model.booking

// Assuming Seat will be in SeatNew.kt
data class SeatLayoutFloor(
    val floor_name: String? = null,
    val seat_floor: Int? = null,
    val seat_type: Int? = null,
    val fare_type_id: Int? = null,
    val seat_fare_type: String? = null,
    val seat_availability: Boolean? = null, // Reverted to Boolean as per API response
    val seat_fare: String? = null,
    val layout: List<List<Seat>>? = null // Represents a 2D array of seats
)
