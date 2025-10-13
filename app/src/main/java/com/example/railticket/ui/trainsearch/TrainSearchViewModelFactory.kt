package com.example.railticket.ui.trainsearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.railticket.data.BookingDataSource
import com.example.railticket.data.LoginRepository
import com.example.railticket.data.LoginDataSource
import com.google.gson.Gson // Import Gson

/**
 * ViewModel provider factory to instantiate TrainSearchViewModel.
 * Required given TrainSearchViewModel has a non-empty constructor
 */
class TrainSearchViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrainSearchViewModel::class.java)) {
            val gson = Gson() // Create Gson instance
            return TrainSearchViewModel(
                bookingDataSource = BookingDataSource(), // BookingDataSource can be a new instance
                loginRepository = LoginRepository.getInstance(
                    dataSource = LoginDataSource(), // LoginDataSource for the LoginRepository singleton
                    gson = gson // Pass Gson instance
                )
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
