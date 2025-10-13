package com.example.railticket.data.network

import com.example.railticket.data.model.booking.ConfirmBookingRequest
import com.example.railticket.data.model.booking.ConfirmBookingResponse
import com.example.railticket.data.model.booking.PassengerDetailsRequest // Added import
import com.example.railticket.data.model.booking.PassengerDetailsResponse // Added import
import com.example.railticket.data.model.booking.ReserveSeatRequest
import com.example.railticket.data.model.booking.ReserveSeatResponse
import com.example.railticket.data.model.booking.SeatLayoutResponse
import com.example.railticket.data.model.booking.VerifyOtpRequest
import com.example.railticket.data.model.booking.VerifyOtpResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface BookingService {

    @PATCH("v1.0/web/bookings/confirm")
    suspend fun confirmBooking(
        @Header("Authorization") authToken: String,
        @Header("X-App-Version") appVersion: String,
        @Header("X-Device-Id") deviceId: String,
        @Body request: ConfirmBookingRequest
    ): Response<ConfirmBookingResponse>

    @POST("v1.0/web/bookings/verify-otp")
    suspend fun verifyOtp(
        @Header("Authorization") authToken: String,
        @Header("X-App-Version") appVersion: String,
        @Header("X-Device-Id") deviceId: String,
        @Body request: VerifyOtpRequest
    ): Response<VerifyOtpResponse>

    @GET("v1.0/web/bookings/seat-layout")
    suspend fun getSeatLayout(
        @Header("Authorization") authToken: String,
        @Header("X-App-Version") appVersion: String,
        @Header("X-Device-Id") deviceId: String,
        @Query("trip_id") tripId: Long,
        @Query("trip_route_id") tripRouteId: String
    ): Response<SeatLayoutResponse>

    @POST("v1.0/web/bookings/reserve-seat")
    suspend fun reserveSeat(
        @Header("Authorization") authToken: String,
        @Header("X-App-Version") appVersion: String,
        @Header("X-Device-Id") deviceId: String,
        @Body request: ReserveSeatRequest
    ): Response<ReserveSeatResponse>

    // New function for sending passenger details
    @POST("v1.0/web/bookings/passenger-details")
    suspend fun sendPassengerDetails(
        @Header("Authorization") authToken: String,
        @Header("X-App-Version") appVersion: String,
        @Header("X-Device-Id") deviceId: String,
        @Body request: PassengerDetailsRequest
    ): Response<PassengerDetailsResponse>
}
