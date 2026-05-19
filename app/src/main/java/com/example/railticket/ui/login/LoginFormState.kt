package com.example.railticket.ui.login

/**
 * Data validation state of the login form.
 */
data class LoginFormState(
    val mobileNumberError: String? = null,
    val passwordError: String? = null,
    val isDataValid: Boolean = false
)
