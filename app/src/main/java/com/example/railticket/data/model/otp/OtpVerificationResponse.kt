package com.example.railticket.data.model.otp

import com.google.gson.annotations.SerializedName

data class OtpVerificationResponse(
    @SerializedName("data")
    val data: OtpData?,

    @SerializedName("extra")
    val extra: OtpExtra?
)

data class OtpData(
    @SerializedName("success")
    val success: Boolean?,

    @SerializedName("user")
    val user: OtpUser?,

    @SerializedName("message")
    val message: String?
)

data class OtpUser(
    @SerializedName("name")
    val name: String?,

    @SerializedName("email")
    val email: String?,

    @SerializedName("mobile")
    val mobile: String?
)

data class OtpExtra(
    @SerializedName("hash")
    val hash: String?
)
