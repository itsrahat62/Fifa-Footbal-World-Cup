package com.example.railticket.data.model.booking

import com.google.gson.annotations.SerializedName

data class VerifyOtpResponse(
    @SerializedName("data") val data: VerifyOtpData?,
    @SerializedName("extra") val extra: OtpExtra?
)

data class VerifyOtpData(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("user") val user: OtpUser?,
    @SerializedName("message") val message: String?,
    @SerializedName("error") val error: Int? // Added back the error field
)

data class OtpUser(
    @SerializedName("name") val name: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("mobile") val mobile: String?
)

// Specific class for the "extra" object, can be expanded if its structure is known and useful
data class OtpExtra(
    @SerializedName("hash") val hash: String?
)
