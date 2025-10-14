package com.example.railticket.data.model.booking

import com.google.gson.annotations.SerializedName

data class ConfirmBookingRequest(
    @SerializedName("trip_id") val tripId: Long,
    @SerializedName("trip_route_id") val tripRouteId: Long,
    @SerializedName("ticket_ids") val ticketIds: List<Long>,
    @SerializedName("boarding_point_id") val boardingPointId: Long,
    @SerializedName("contactperson") val contactPerson: Int,
    @SerializedName("passengerType") val passengerType: List<String>,
    @SerializedName("pemail") val passengerEmail: String,
    @SerializedName("pmobile") val passengerMobile: String,
    @SerializedName("pname") val passengerName: List<String>,
    @SerializedName("gender") val gender: List<String>,
    @SerializedName("selected_mobile_transaction") val selectedMobileTransaction: String, // Corrected to String
    @SerializedName("otp") val otp: String
)
