package com.example.railticket.ui.login

/**
 * User details post authentication that are exposed to UI.
 */
data class LoggedInUserView(
    val displayName: String,
    val token: String, // Added token field
    val message: String // Added message field
)
