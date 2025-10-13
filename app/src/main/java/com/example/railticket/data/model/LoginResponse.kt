package com.example.railticket.data.model

import com.google.gson.annotations.SerializedName

// Corresponds to the 'user' object within the 'data' part of the Login API response
data class LoginResponseUser(
    @SerializedName("display_name")
    val displayName: String?
    // Add other fields from the actual API's 'user' object if present
    // For example, if 'id', 'email', 'mobile' are part of this user object:
    // val id: String?,
    // val email: String?,
    // val mobile: String?
)

// Corresponds to the 'data' object within the Login API response
data class LoginResponseData(
    @SerializedName("message")
    val message: String?,
    @SerializedName("token")
    val token: String?,
    @SerializedName("user")
    val user: LoginResponseUser? // This user object structure should match LoginDataSource
)

// Top-level response for the Login API
data class LoginResponse(
    @SerializedName("data")
    val data: LoginResponseData?
    // Add other top-level fields if your API returns them, e.g.,
    // @SerializedName("success") val success: Boolean?
)
