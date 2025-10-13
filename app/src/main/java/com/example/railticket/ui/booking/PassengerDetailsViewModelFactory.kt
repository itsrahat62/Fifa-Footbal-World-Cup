package com.example.railticket.ui.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.railticket.data.repository.BookingRepository // Ensure this path is correct

/**
 * ViewModelProvider.Factory that takes BookingRepository, appVersion, and deviceId as arguments
 * and creates a PassengerDetailsViewModel.
 */
class PassengerDetailsViewModelFactory(
    private val bookingRepository: BookingRepository,
    private val appVersion: String,
    private val deviceId: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PassengerDetailsViewModel::class.java)) {
            return PassengerDetailsViewModel(bookingRepository, appVersion, deviceId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
    }
}
