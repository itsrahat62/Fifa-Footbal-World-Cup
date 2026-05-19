package com.example.railticket.data.model.booking

import com.google.gson.annotations.SerializedName

data class ConfirmBookingResponse(
    @SerializedName("data") val data: ConfirmBookingData?,
    @SerializedName("extra") val extra: ConfirmBookingExtra?
)

data class ConfirmBookingData(
    @SerializedName("message") val message: String?,
    @SerializedName("redirectUrl") val redirectUrl: String?
)

data class ConfirmBookingExtra(
    @SerializedName("hash") val hash: String?
)
