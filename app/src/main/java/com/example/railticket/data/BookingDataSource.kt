package com.example.railticket.data

import android.util.Log
import com.example.railticket.data.model.ErrorResponse
// Import new request/response types for confirmBooking
import com.example.railticket.data.model.booking.ConfirmBookingRequest
import com.example.railticket.data.model.booking.ConfirmBookingResponse
import com.example.railticket.data.model.booking.PassengerDetailsRequest
import com.example.railticket.data.model.booking.PassengerDetailsResponse
import com.example.railticket.data.model.booking.ReserveSeatRequest
import com.example.railticket.data.model.booking.ReserveSeatResponse
import com.example.railticket.data.model.booking.SearchTripsResponse
import com.example.railticket.data.model.booking.SeatLayoutResponse
import com.example.railticket.data.model.getPrimaryErrorMessage
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Scanner

class BookingDataSource {

    private val gson = Gson()
    private val connectTimeoutMs = 15000 // 15 seconds
    private val readTimeoutMs = 15000    // 15 seconds
    private val APP_BOOKINGS_BASE_URL = "https://railspaapi.shohoz.com/v1.0/app/bookings"
    private val WEB_BOOKINGS_BASE_URL = "https://railspaapi.shohoz.com/v1.0/web/bookings" // Added for confirmBooking

    suspend fun searchTrips(
        fromCity: String,
        toCity: String,
        dateOfJourney: String,
        seatClass: String,
        token: String
    ): Result<SearchTripsResponse> = withContext(Dispatchers.IO) {
        val urlString = "$WEB_BOOKINGS_BASE_URL/search-trips-v2?from_city=$fromCity&to_city=$toCity&date_of_journey=$dateOfJourney&seat_class=$seatClass"
        Log.d("BookingDataSource", "SearchTrips URL: $urlString")
        var errorResponseBody: String? = null

        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseBody = Scanner(connection.inputStream).useDelimiter("\\A").next()
                Log.d("BookingDataSource", "SearchTrips Response: $responseBody")
                val searchTripsResponse = gson.fromJson(responseBody, SearchTripsResponse::class.java)
                Result.Success(searchTripsResponse, "Trips fetched successfully")
            } else {
                val errorStream = connection.errorStream
                errorResponseBody = errorStream?.let { Scanner(it).useDelimiter("\\A").next() } ?: ""
                val errorMessage = try {
                    if (errorResponseBody!!.isNotEmpty()) {
                        Log.d("BookingDataSource", "SearchTrips Error Response Body: $errorResponseBody")
                        val errorDetails = gson.fromJson(errorResponseBody, ErrorResponse::class.java)
                        errorDetails.getPrimaryErrorMessage() ?: "Unknown server error (code $responseCode)"
                    } else {
                         "Unknown server error: No error stream (code $responseCode)"
                    }
                } catch (e: Exception) {
                    Log.e("BookingDataSource", "Error parsing error stream for SearchTrips code $responseCode: $errorResponseBody", e)
                    "Error parsing server response (code $responseCode)"
                }
                Log.w("BookingDataSource", "SearchTrips failed. HTTP Code: $responseCode - Message: $errorMessage")
                Result.Error(IOException("Server error: $responseCode - $errorMessage"), errorMessage, errorResponseBody)
            }
        } catch (e: Exception) {
            val exceptionType = e.javaClass.simpleName
            val detailedErrorMessage = e.message ?: "An unexpected error occurred during trip search."
            Log.e("BookingDataSource", "SearchTrips Exception ($exceptionType): $detailedErrorMessage", e)
            Result.Error(IOException("Network Error ($exceptionType): $detailedErrorMessage", e), "Network Error: $detailedErrorMessage. Please check connection.", null)
        }
    }

    suspend fun getSeatLayout(
        tripId: Long,
        tripRouteId: String, // Changed from Long to String
        token: String
    ): Result<SeatLayoutResponse> = withContext(Dispatchers.IO) {
        val urlString = "$WEB_BOOKINGS_BASE_URL/seat-layout?trip_id=$tripId&trip_route_id=$tripRouteId"
        Log.d("BookingDataSource", "GetSeatLayout URL: $urlString")
        var errorResponseBody: String? = null
        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseBody = Scanner(connection.inputStream).useDelimiter("\\A").next()
                Log.d("BookingDataSource", "GetSeatLayout Response: $responseBody")
                val seatLayoutResponse = gson.fromJson(responseBody, SeatLayoutResponse::class.java)
                Result.Success(seatLayoutResponse, "Seat layout fetched successfully")
            } else {
                val errorStream = connection.errorStream
                errorResponseBody = errorStream?.let { Scanner(it).useDelimiter("\\A").next() } ?: ""
                val errorMessage = try {
                     if (errorResponseBody!!.isNotEmpty()) {
                        Log.d("BookingDataSource", "GetSeatLayout Error Response Body: $errorResponseBody")
                        val errorDetails = gson.fromJson(errorResponseBody, ErrorResponse::class.java)
                        errorDetails.getPrimaryErrorMessage() ?: "Unknown server error (code $responseCode)"
                    } else {
                        "Unknown server error: No error stream (code $responseCode)"
                    }
                } catch (e: Exception) {
                    Log.e("BookingDataSource", "Error parsing error stream for GetSeatLayout code $responseCode: $errorResponseBody", e)
                    "Error parsing server response (code $responseCode)"
                }
                Log.w("BookingDataSource", "GetSeatLayout failed. HTTP Code: $responseCode - Message: $errorMessage")
                Result.Error(IOException("Server error: $responseCode - $errorMessage"), errorMessage, errorResponseBody)
            }
        } catch (e: Exception) {
            val exceptionType = e.javaClass.simpleName
            val detailedErrorMessage = e.message ?: "An unexpected error occurred during seat layout fetch."
            Log.e("BookingDataSource", "GetSeatLayout Exception ($exceptionType): $detailedErrorMessage", e)
            Result.Error(IOException("Network Error ($exceptionType): $detailedErrorMessage", e), "Network Error: $detailedErrorMessage. Please check connection.", null)
        }
    }

    suspend fun reserveSeat(
        request: ReserveSeatRequest,
        token: String
    ): Result<ReserveSeatResponse> = withContext(Dispatchers.IO) {
        val urlString = "$WEB_BOOKINGS_BASE_URL/reserve-seat"
        Log.d("BookingDataSource", "ReserveSeat URL: $urlString")
        var errorResponseBody: String? = null
        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "PATCH"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.doOutput = true
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs

            val jsonRequestBody = gson.toJson(request)
            Log.d("BookingDataSource", "ReserveSeat Request Body: $jsonRequestBody")

            OutputStreamWriter(connection.outputStream).use {
                it.write(jsonRequestBody)
                it.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseBody = Scanner(connection.inputStream).useDelimiter("\\A").next()
                Log.d("BookingDataSource", "ReserveSeat Response: $responseBody")
                val reserveSeatResponse = gson.fromJson(responseBody, ReserveSeatResponse::class.java)
                if (reserveSeatResponse.data?.error == 0) {
                    Result.Success(reserveSeatResponse, reserveSeatResponse.data.message ?: "Reserved successfully")
                } else {
                    val apiErrorMessage = reserveSeatResponse.data?.message ?: "Seat reservation failed (API error)"
                    Result.Error(IOException(apiErrorMessage), apiErrorMessage, responseBody)
                }
            } else {
                val errorStream = connection.errorStream
                errorResponseBody = errorStream?.let { Scanner(it).useDelimiter("\\A").next() } ?: ""
                val errorMessage = try {
                    if (errorResponseBody!!.isNotEmpty()) {
                        Log.d("BookingDataSource", "ReserveSeat Error Response Body: $errorResponseBody")
                        // Try parsing with ReserveSeatResponse first for specific error, then fallback to ErrorResponse
                        try {
                            val apiError = gson.fromJson(errorResponseBody, ReserveSeatResponse::class.java)
                            apiError.data?.message ?: run {
                                val genError = gson.fromJson(errorResponseBody, ErrorResponse::class.java)
                                genError.getPrimaryErrorMessage() ?: "Unknown server error (code $responseCode)"
                            }
                        } catch (e: Exception) { // Catching specific JsonSyntaxException or general Exception
                             val genError = gson.fromJson(errorResponseBody, ErrorResponse::class.java)
                             genError.getPrimaryErrorMessage() ?: errorResponseBody ?: "Unknown server error (code $responseCode)"
                        }
                    } else {
                        "Unknown server error: No error stream (code $responseCode)"
                    }
                } catch (e: Exception) {
                    Log.e("BookingDataSource", "Error parsing error stream for ReserveSeat code $responseCode: $errorResponseBody", e)
                    "Error parsing server response (code $responseCode)"
                }
                Log.w("BookingDataSource", "ReserveSeat failed. HTTP Code: $responseCode - Message: $errorMessage")
                Result.Error(IOException("Server error: $responseCode - $errorMessage"), errorMessage, errorResponseBody)
            }
        } catch (e: Exception) {
            val exceptionType = e.javaClass.simpleName
            val detailedErrorMessage = e.message ?: "An unexpected error occurred during seat reservation."
            Log.e("BookingDataSource", "ReserveSeat Exception ($exceptionType): $detailedErrorMessage", e)
            Result.Error(IOException("Network Error ($exceptionType): $detailedErrorMessage", e), "Network Error: $detailedErrorMessage. Please check connection.", null)
        }
    }

    suspend fun sendPassengerDetails(
        request: PassengerDetailsRequest,
        token: String,
        appVersion: String,
        deviceId: String
    ): Result<PassengerDetailsResponse> = withContext(Dispatchers.IO) {
        val urlString = "$WEB_BOOKINGS_BASE_URL/passenger-details" // Removed hardcoded appVersion & deviceId
        Log.d("BookingDataSource", "SendPassengerDetails URL: $urlString, Token: Bearer ${token.take(15)}...")
        var errorResponseBody: String? = null
        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("X-App-Version", appVersion)      // Send as Header
            connection.setRequestProperty("X-Device-Id", deviceId)        // Send as Header
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.doOutput = true
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs

            val jsonRequestBody = gson.toJson(request)
            Log.d("BookingDataSource", "SendPassengerDetails Request Body: $jsonRequestBody")

            OutputStreamWriter(connection.outputStream).use {
                it.write(jsonRequestBody)
                it.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseBody = Scanner(connection.inputStream).useDelimiter("\\A").next()
                Log.d("BookingDataSource", "SendPassengerDetails Response: $responseBody")
                val passengerDetailsResponse = gson.fromJson(responseBody, PassengerDetailsResponse::class.java)
                if (passengerDetailsResponse.data?.success == true) {
                    Result.Success(passengerDetailsResponse, passengerDetailsResponse.data?.message ?: "Passenger details sent successfully")
                } else {
                    val apiErrorMessage = passengerDetailsResponse.data?.message ?: "Sending passenger details failed (API error)"
                    Result.Error(IOException(apiErrorMessage), apiErrorMessage, responseBody)
                }
            } else {
                val errorStream = connection.errorStream
                errorResponseBody = errorStream?.let { Scanner(it).useDelimiter("\\A").next() } ?: ""
                val errorMessage = try {
                    if (errorResponseBody!!.isNotEmpty()) {
                        Log.d("BookingDataSource", "SendPassengerDetails Error Response Body: $errorResponseBody")
                         try {
                            val apiError = gson.fromJson(errorResponseBody, PassengerDetailsResponse::class.java)
                            apiError.data?.message ?: run {
                                val genError = gson.fromJson(errorResponseBody, ErrorResponse::class.java)
                                genError.getPrimaryErrorMessage() ?: "Unknown server error (code $responseCode)"
                            }
                        } catch (e: Exception) { 
                             val genError = gson.fromJson(errorResponseBody, ErrorResponse::class.java)
                             genError.getPrimaryErrorMessage() ?: errorResponseBody ?: "Unknown server error (code $responseCode)"
                        }
                    } else {
                        "Unknown server error: No error stream (code $responseCode)"
                    }
                } catch (e: Exception) {
                    Log.e("BookingDataSource", "Error parsing error stream for SendPassengerDetails code $responseCode: $errorResponseBody", e)
                    "Error parsing server response (code $responseCode)"
                }
                Log.w("BookingDataSource", "SendPassengerDetails failed. HTTP Code: $responseCode - Message: $errorMessage")
                Result.Error(IOException("Server error: $responseCode - $errorMessage"), errorMessage, errorResponseBody)
            }
        } catch (e: Exception) {
            val exceptionType = e.javaClass.simpleName
            val detailedErrorMessage = e.message ?: "An unexpected error occurred during passenger details submission."
            Log.e("BookingDataSource", "SendPassengerDetails Exception ($exceptionType): $detailedErrorMessage", e)
            Result.Error(IOException("Network Error ($exceptionType): $detailedErrorMessage", e), "Network Error: $detailedErrorMessage. Please check connection.", null)
        }
    }

    suspend fun verifyOtp(
        request: com.example.railticket.data.model.booking.VerifyOtpRequest,
        token: String,
        appVersion: String,
        deviceId: String
    ): Result<com.example.railticket.data.model.booking.VerifyOtpResponse> = withContext(Dispatchers.IO) {
        val urlString = "$APP_BOOKINGS_BASE_URL/verify-otp" // Removed appVersion & deviceId from query
        val TAG_BOOKING_OTP = "BookingDataSource_BookingOTP"
        Log.d(TAG_BOOKING_OTP, "verifyOtp (booking) called. URL: $urlString, Token: Bearer ${token.take(15)}..., Request: $request")
        var errorResponseBodyForBookingOtp: String? = null
        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("X-App-Version", appVersion)      // Send as Header
            connection.setRequestProperty("X-Device-Id", deviceId)        // Send as Header
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            // connection.setRequestProperty("App-Version", appVersion) // Redundant if X-App-Version is used
            // connection.setRequestProperty("Device-ID", deviceId) // Redundant if X-Device-Id is used
            connection.doOutput = true
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs

            val jsonRequestBody = gson.toJson(request)
            Log.d(TAG_BOOKING_OTP, "VerifyOtp (booking) Request Body: $jsonRequestBody")

            OutputStreamWriter(connection.outputStream).use {
                it.write(jsonRequestBody)
                it.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseBody = Scanner(connection.inputStream).useDelimiter("\\A").next()
                Log.d(TAG_BOOKING_OTP, "VerifyOtp (booking) Successful Response Body: $responseBody")
                val verifyOtpResponse = gson.fromJson(responseBody, com.example.railticket.data.model.booking.VerifyOtpResponse::class.java)

                if (verifyOtpResponse.data?.success == true || verifyOtpResponse.data?.error == 0) {
                    Result.Success(verifyOtpResponse, verifyOtpResponse.data?.message ?: "Booking OTP Verified Successfully")
                } else {
                    val apiErrorMessage = verifyOtpResponse.data?.message ?: "Booking OTP verification failed by API (within HTTP OK)."
                    Log.w(TAG_BOOKING_OTP, "VerifyOtp (booking) API Error (within HTTP OK): $apiErrorMessage - Response: $responseBody")
                    Result.Error(IOException("VerifyOtp (booking) API Error: $apiErrorMessage"), apiErrorMessage, responseBody)
                }
            } else {
                val errorStream = connection.errorStream
                errorResponseBodyForBookingOtp = errorStream?.let { Scanner(it).useDelimiter("\\A").next() } ?: ""
                val errorMessage = try {
                    if (errorResponseBodyForBookingOtp!!.isNotEmpty()) {
                        Log.d(TAG_BOOKING_OTP, "VerifyOtp (booking) Error Response Body: $errorResponseBodyForBookingOtp")
                        val parsedError = gson.fromJson(errorResponseBodyForBookingOtp, ErrorResponse::class.java)
                        parsedError?.getPrimaryErrorMessage() ?: "Unknown server error (code $responseCode)"
                    } else {
                         "Unknown server error: No error stream (code $responseCode)"
                    }
                } catch (e: Exception) {
                    Log.e(TAG_BOOKING_OTP, "Error parsing error stream for booking OTP code $responseCode: $errorResponseBodyForBookingOtp", e)
                    "Error parsing server response (code $responseCode)"
                }
                Log.w(TAG_BOOKING_OTP, "VerifyOtp (booking) failed. HTTP Code: $responseCode - Message: $errorMessage")
                Result.Error(IOException("Server error: $responseCode - $errorMessage"), errorMessage, errorResponseBodyForBookingOtp)
            }
        } catch (e: Exception) {
            val exceptionType = e.javaClass.simpleName
            val detailedErrorMessage = e.message ?: "An unexpected error occurred during booking OTP verification."
            Log.e(TAG_BOOKING_OTP, "VerifyOtp (booking) Exception ($exceptionType): $detailedErrorMessage", e)
            Result.Error(IOException("Network Error ($exceptionType): $detailedErrorMessage", e), "Network Error: $detailedErrorMessage. Please check connection.", null)
        }
    }

    // New method to confirm booking and get payment redirect URL
    suspend fun confirmBooking(
        request: ConfirmBookingRequest,
        token: String,
        appVersion: String,
        deviceId: String
    ): Result<ConfirmBookingResponse> = withContext(Dispatchers.IO) {
        val urlString = "$WEB_BOOKINGS_BASE_URL/confirm" // Removed appVersion & deviceId from query
        val TAG_CONFIRM_BOOKING = "BookingDataSource_ConfirmBooking"
        Log.d(TAG_CONFIRM_BOOKING, "confirmBooking called. URL: $urlString, Token: Bearer ${token.take(15)}..., Request: $request")
        var errorResponseBody: String? = null

        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "PATCH" // HTTP Method: PATCH
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("X-App-Version", appVersion)      // Send as Header
            connection.setRequestProperty("X-Device-Id", deviceId)        // Send as Header
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            // connection.setRequestProperty("App-Version", appVersion) // Redundant
            // connection.setRequestProperty("Device-ID", deviceId) // Redundant
            connection.doOutput = true
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs

            val jsonRequestBody = gson.toJson(request)
            Log.d(TAG_CONFIRM_BOOKING, "ConfirmBooking Request Body: $jsonRequestBody")

            OutputStreamWriter(connection.outputStream).use {
                it.write(jsonRequestBody)
                it.flush()
            }

            val responseCode = connection.responseCode
            Log.d(TAG_CONFIRM_BOOKING, "ConfirmBooking Response Code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseBody = Scanner(connection.inputStream).useDelimiter("\\A").next()
                Log.d(TAG_CONFIRM_BOOKING, "ConfirmBooking Successful Response Body: $responseBody")
                val confirmBookingResponse = gson.fromJson(responseBody, ConfirmBookingResponse::class.java)
                
                // Check for non-null data and redirectUrl as per your example successful response
                if (confirmBookingResponse.data?.redirectUrl != null) {
                    Result.Success(confirmBookingResponse, confirmBookingResponse.data.message ?: "Booking confirmed, redirecting to payment.")
                } else {
                    val apiErrorMessage = confirmBookingResponse.data?.message ?: "Booking confirmation failed by API (missing redirectUrl)."
                    Log.w(TAG_CONFIRM_BOOKING, "ConfirmBooking API Error (within HTTP OK): $apiErrorMessage - Response: $responseBody")
                    Result.Error(IOException("ConfirmBooking API Error: $apiErrorMessage"), apiErrorMessage, responseBody)
                }
            } else {
                val errorStream = connection.errorStream
                errorResponseBody = errorStream?.let { Scanner(it).useDelimiter("\\A").next() } ?: ""
                Log.d(TAG_CONFIRM_BOOKING, "ConfirmBooking Error Response Body: $errorResponseBody")
                val errorMessage = try {
                    if (errorResponseBody!!.isNotEmpty()) {
                         // Try to parse with ConfirmBookingResponse first, then fallback to generic ErrorResponse
                        try {
                            val apiError = gson.fromJson(errorResponseBody, ConfirmBookingResponse::class.java)
                            apiError.data?.message ?: run {
                                val genError = gson.fromJson(errorResponseBody, ErrorResponse::class.java)
                                genError.getPrimaryErrorMessage() ?: "Unknown server error (code $responseCode)"
                            }
                        } catch (e: Exception) { 
                            val genError = gson.fromJson(errorResponseBody, ErrorResponse::class.java)
                            genError.getPrimaryErrorMessage() ?: errorResponseBody ?: "Unknown server error (code $responseCode)"
                        }
                    } else {
                        "Unknown server error: No error stream (code $responseCode)"
                    }
                } catch (e: Exception) {
                    Log.e(TAG_CONFIRM_BOOKING, "Error parsing error stream for confirm booking, code $responseCode: $errorResponseBody", e)
                    "Error parsing server response (code $responseCode)"
                }
                Log.w(TAG_CONFIRM_BOOKING, "ConfirmBooking failed. HTTP Code: $responseCode - Message: $errorMessage")
                Result.Error(IOException("Server error: $responseCode - $errorMessage"), errorMessage, errorResponseBody)
            }
        } catch (e: Exception) {
            val exceptionType = e.javaClass.simpleName
            val detailedErrorMessage = e.message ?: "An unexpected error occurred during booking confirmation."
            Log.e(TAG_CONFIRM_BOOKING, "ConfirmBooking Exception ($exceptionType): $detailedErrorMessage", e)
            Result.Error(IOException("Network Error ($exceptionType): $detailedErrorMessage", e), "Network Error: $detailedErrorMessage. Please check connection.", null)
        }
    }
}
