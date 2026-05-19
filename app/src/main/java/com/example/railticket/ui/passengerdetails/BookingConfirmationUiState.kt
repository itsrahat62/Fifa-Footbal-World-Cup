package com.example.railticket.ui.passengerdetails

import com.example.railticket.util.Event // Assuming Event class is in com.example.railticket.util

data class BookingConfirmationUiState(
    val isLoading: Boolean = false,
    val successUrl: Event<String>? = null,
    val error: Event<String>? = null
)
