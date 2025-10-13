package com.example.railticket.data.model

import com.google.gson.annotations.SerializedName

data class OtpResponseData(
    @SerializedName("data") // Assuming the inner 'data' is also named 'data' in JSON
    val innerData: OtpInnerData?,
    val message: String? // Or this might be at the same level as innerData
    // Add other fields if the actual API response's outer 'data' object has more
)
