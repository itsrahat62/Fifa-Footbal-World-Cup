package com.example.railticket.data.model

import com.google.gson.annotations.SerializedName

data class VerifyOtpData(
    @SerializedName("success")
    val success: Boolean?,
    @SerializedName("user")
    val user: OtpVerificationUser?, // Changed from LoggedInUser?
    @SerializedName("message")
    val message: String?,
    @SerializedName("error") // From LoginViewModel: responseData?.error == 0
    val error: Int? = null // For APIs that use an error code like 0 for success
    // Add any other fields if the 'data' object in the response has more
)
