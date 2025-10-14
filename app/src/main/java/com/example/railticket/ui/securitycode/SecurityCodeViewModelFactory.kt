package com.example.railticket.ui.securitycode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SecurityCodeViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SecurityCodeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SecurityCodeViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
