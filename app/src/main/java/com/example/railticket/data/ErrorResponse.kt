package com.example.railticket.data

data class ErrorResponse(
    val error: ErrorDetails? = null
)

data class ErrorDetails(
    val code: Int? = null,
    val messages: List<String>? = null
)
