package com.example.railticket.ui.passengerdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.railticket.data.repository.BookingRepository
// It's good practice to get these from a central place, e.g., a utility or constants file
// For now, the factory will expect them to be passed in.
// import com.example.railticket.util.Constants

class PassengerDetailsViewModelFactory(
    private val bookingRepository: BookingRepository,
    private val appVersion: String,
    private val deviceId: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PassengerDetailsViewModel::class.java)) {
            // Consider how you'll get appVersion and deviceId here.
            // They could be passed from the fragment, which gets them from a utility/constants.
            return PassengerDetailsViewModel(
                bookingRepository,
                appVersion, // e.g., Constants.APP_VERSION
                deviceId    // e.g., DeviceInfoUtil.getDeviceId(context)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
