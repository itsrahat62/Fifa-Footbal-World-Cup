package com.example.railticket.data.model

import com.google.gson.annotations.SerializedName

data class VerifyOtpExtra(
    @SerializedName("hash")
    val hash: String?
    // Add any other fields if the 'extra' object in the response has more
)
