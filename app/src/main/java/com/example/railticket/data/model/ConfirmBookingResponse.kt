package com.example.railticket.data.model

import com.google.gson.annotations.SerializedName

data class ConfirmBookingResponse(
    @SerializedName("data") val data: ConfirmBookingData?,
    @SerializedName("extra") val extra: ConfirmBookingExtra? // Or Map<String, Any> if structure varies
)

data class ConfirmBookingData(
    @SerializedName("message") val message: String?,
    @SerializedName("redirectUrl") val redirectUrl: String?
)

data class ConfirmBookingExtra(
    @SerializedName("hash") val hash: String?
)
