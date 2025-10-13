package com.example.railticket.ui.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.railticket.data.BookingDataSource

class OtpVerificationViewModelFactory(
    private val bookingDataSource: BookingDataSource,
    private val appVersion: String,
    private val deviceId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OtpVerificationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Pass appVersion and deviceId to the OtpVerificationViewModel constructor
            return OtpVerificationViewModel(bookingDataSource, appVersion, deviceId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
