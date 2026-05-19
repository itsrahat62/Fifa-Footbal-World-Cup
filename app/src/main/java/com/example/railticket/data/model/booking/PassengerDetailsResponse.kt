package com.example.railticket.data.model.booking

import com.google.gson.annotations.SerializedName

data class PassengerDetailsResponse(
    @SerializedName("data") val data: PassengerDetailsResponseData?,
    @SerializedName("extra") val extra: Map<String, Any>? // Assuming 'extra' can be a generic map
)

data class PassengerDetailsResponseData(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("msg") val message: String?, // Or your API might use "message"
    @SerializedName("otp_sent") val otpSent: Boolean?, // Example, if your API confirms OTP sending
    @SerializedName("error") val error: Int?, // If your API includes an error code here
    // Add any other fields that are part of the 'data' object in the response
)

