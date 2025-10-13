package com.example.railticket.data.model

import com.google.gson.annotations.SerializedName

// This class might still be useful for other API error formats.
data class ErrorDetail(
    @SerializedName("field")
    val field: String?,
    @SerializedName("message")
    val message: String?
)

// New class to represent the "messages" object within the "error" object for 422 type errors
data class ErrorMessagesPayload(
    @SerializedName("errors")
    val errors: Map<String, List<String>>? // e.g., "trip_id": ["message1", "message2"]
) {
    // Helper to get the first error message from the map of errors
    fun getFirstNestedErrorMessage(): String? {
        // Iterate through the map values (which are lists of strings),
        // take the first non-empty list, and then take the first message from that list.
        return errors?.values?.firstOrNull { it.isNotEmpty() }?.firstOrNull()
    }
}

// Updated ErrorContainer to hold the error code and the new ErrorMessagesPayload
data class ErrorContainer(
    @SerializedName("code")
    val code: Int?,
    @SerializedName("messages")
    val messages: ErrorMessagesPayload? // This now maps to {"errors": {...}}
)

// Main ErrorResponse class
data class ErrorResponse(
    // General top-level message (some APIs use this)
    @SerializedName("message")
    val message: String?,

    // Alternative general top-level message field (some APIs use this)
    @SerializedName("msg")
    val msg: String?,

    // For APIs that return a flat list of specific error details.
    // This is different from the nested structure handled by ErrorContainer/ErrorMessagesPayload.
    @SerializedName("errors")
    val errors: List<ErrorDetail>?,

    // This is the main "error" object like {"code": 422, "messages": {"errors":{...}} }
    @SerializedName("error")
    val error: ErrorContainer?
)

// Helper extension function to get the most relevant error message from ErrorResponse
fun ErrorResponse.getPrimaryErrorMessage(): String? {
    return this.message // Check top-level "message" first
        ?: this.msg     // Then top-level "msg"
        ?: this.errors?.firstOrNull()?.message // Then from a flat list of detailed errors
        ?: this.error?.messages?.getFirstNestedErrorMessage() // Then from the nested 422-style error structure
        ?: this.error?.code?.toString() // Fallback to error code if no message string found
        ?: "An unknown error occurred" // Ultimate fallback
}
