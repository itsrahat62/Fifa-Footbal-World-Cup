package com.example.railticket.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.railticket.data.LoginDataSource
import com.example.railticket.data.LoginRepository
import com.google.gson.Gson // Import Gson

/**
 * ViewModel provider factory to instantiate LoginViewModel. Required given LoginViewModel has
 * a non-empty constructor
 */
class LoginViewModelFactory : ViewModelProvider.Factory {

    // TODO: Replace with actual app version and device ID, possibly from BuildConfig or a provider
    private val defaultAppVersion: String = "1.0.0" // Placeholder
    private val defaultDeviceId: String = "rail_ticket_device_id_placeholder" // Placeholder

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            val gson = Gson() // Create Gson instance
            val loginRepository = LoginRepository.getInstance(
                dataSource = LoginDataSource(),
                gson = gson // Pass Gson to LoginRepository
            )
            return LoginViewModel(
                loginRepository = loginRepository,
                defaultAppVersion = defaultAppVersion, // Pass app version
                defaultDeviceId = defaultDeviceId      // Pass device ID
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
    }
}
