package com.example.railticket.data.model

import com.google.gson.annotations.SerializedName

data class VerifyOtpResponse(
    @SerializedName("data")
    val data: VerifyOtpData?,
    @SerializedName("extra")
    val extra: VerifyOtpExtra?
    // Add any other top-level fields if your API returns them
)
