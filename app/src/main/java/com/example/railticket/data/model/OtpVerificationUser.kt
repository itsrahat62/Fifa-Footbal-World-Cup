package com.example.railticket.data.model

import com.google.gson.annotations.SerializedName

data class OtpVerificationUser(
    @SerializedName("name")
    val name: String?,
    @SerializedName("email")
    val email: String?,
    @SerializedName("mobile")
    val mobile: String?
)
