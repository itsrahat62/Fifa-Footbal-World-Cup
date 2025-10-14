package com.example.railticket.ui.passengerdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.railticket.data.repository.BookingRepository

class PassengerDetailsViewModelFactory(
    private val bookingRepository: BookingRepository,
    private val defaultDeviceId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PassengerDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PassengerDetailsViewModel(bookingRepository, defaultDeviceId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
