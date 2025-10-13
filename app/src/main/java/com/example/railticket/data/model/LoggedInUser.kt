package com.example.railticket.data.model

data class LoggedInUser(
    val userId: String,
    val displayName: String,
    val token: String
)
