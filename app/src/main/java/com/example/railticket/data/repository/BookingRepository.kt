package com.example.railticket.data.repository

import com.example.railticket.data.model.booking.ConfirmBookingRequest
import com.example.railticket.data.model.booking.ConfirmBookingResponse
import com.example.railticket.data.model.booking.PassengerDetailsRequest // Added import
import com.example.railticket.data.model.booking.PassengerDetailsResponse // Added import
import com.example.railticket.data.model.booking.ReserveSeatRequest
import com.example.railticket.data.model.booking.ReserveSeatResponse
import com.example.railticket.data.model.booking.SeatLayoutResponse
import com.example.railticket.data.network.BookingService
import retrofit2.Response

class BookingRepository(private val bookingService: BookingService) {

    suspend fun confirmBooking(
        authToken: String,
        appVersion: String,
        deviceId: String,
        request: ConfirmBookingRequest
    ): Response<ConfirmBookingResponse> {
        val completeAuthToken = if (authToken.startsWith("Bearer ")) authToken else "Bearer $authToken"
        return bookingService.confirmBooking(
            authToken = completeAuthToken,
            appVersion = appVersion,
            deviceId = deviceId,
            request = request
        )
    }

    suspend fun getSeatLayout(
        authToken: String,
        appVersion: String,
        deviceId: String,
        tripId: Long,
        tripRouteId: String
    ): Response<SeatLayoutResponse> {
        val completeAuthToken = if (authToken.startsWith("Bearer ")) authToken else "Bearer $authToken"
        return bookingService.getSeatLayout(
            authToken = completeAuthToken,
            appVersion = appVersion,
            deviceId = deviceId,
            tripId = tripId,
            tripRouteId = tripRouteId
        )
    }

    suspend fun reserveSeat(
        authToken: String,
        appVersion: String,
        deviceId: String,
        request: ReserveSeatRequest
    ): Response<ReserveSeatResponse> {
        val completeAuthToken = if (authToken.startsWith("Bearer ")) authToken else "Bearer $authToken"
        return bookingService.reserveSeat(
            authToken = completeAuthToken,
            appVersion = appVersion,
            deviceId = deviceId,
            request = request
        )
    }

    // New function for sending passenger details
    suspend fun sendPassengerDetails(
        authToken: String,
        appVersion: String,
        deviceId: String,
        request: PassengerDetailsRequest
    ): Response<PassengerDetailsResponse> {
        val completeAuthToken = if (authToken.startsWith("Bearer ")) authToken else "Bearer $authToken"
        return bookingService.sendPassengerDetails(
            authToken = completeAuthToken,
            appVersion = appVersion,
            deviceId = deviceId,
            request = request
        )
    }
}
