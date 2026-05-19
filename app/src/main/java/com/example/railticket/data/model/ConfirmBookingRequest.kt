package com.example.railticket.data.model

import com.google.gson.annotations.SerializedName

data class ConfirmBookingRequest(
    @SerializedName("trip_id") val tripId: String,
    @SerializedName("trip_route_id") val tripRouteId: String,
    @SerializedName("ticket_ids") val ticketIds: List<String>,
    @SerializedName("boarding_point_id") val boardingPointId: String,

    // Contact Person Details - maps to existing JSON fields if possible, or define clearly
    @SerializedName("pname") val contactName: String, // Assuming "pname" is for the primary contact
    @SerializedName("pemail") val contactEmail: String,
    @SerializedName("pmobile") val contactMobile: String,

    // Details for ALL passengers. 
    // It's assumed the API supports receiving names for all passengers.
    // If the API only took one name via "pname" and expected types/genders for multiple, that would be an API limitation.
    @SerializedName("passenger_names") val passengerNames: List<String>, // List of names for ALL passengers
    
    // Uses existing JSON field names "passengerType" and "gender" for the lists
    @SerializedName("passengerType") val passengerTypes: List<String>,
    @SerializedName("gender") val genders: List<String>,

    @SerializedName("selected_mobile_transaction") val selectedMobileTransaction: String = "3" // Default value as in original
    // Removed "otp" field
    // Removed "contactperson: Int" field
)
