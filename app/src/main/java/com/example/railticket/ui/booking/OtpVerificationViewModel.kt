package com.example.railticket.ui.booking

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.railticket.data.BookingDataSource
import com.example.railticket.data.Result
// Ensure this is the correct VerifyOtpRequest for the booking flow
import com.example.railticket.data.model.booking.VerifyOtpRequest 
// Ensure this is the correct VerifyOtpResponse for the booking flow
import com.example.railticket.data.model.booking.VerifyOtpResponse 
import com.example.railticket.util.Event
import kotlinx.coroutines.launch
import java.io.IOException

// Data class to hold information for navigating to PassengerDetailsFragment
data class PassengerDetailsNavigationData(
    val passenger1Name: String?,
    val passenger1Email: String?,
    val passenger1Mobile: String?,
    val tripId: String,
    val tripRouteId: String,
    val ticketIds: List<String>,
    val authToken: String,
    val otp: String // Added to carry the verified OTP
)

class OtpVerificationViewModel(
    private val bookingDataSource: BookingDataSource,
    private val appVersion: String, // Added appVersion
    private val deviceId: String    // Added deviceId
) : ViewModel() {

    // Use the booking-specific VerifyOtpResponse
    private val _otpVerificationResult = MutableLiveData<Event<Result<VerifyOtpResponse>>>()
    val otpVerificationResult: LiveData<Event<Result<VerifyOtpResponse>>> = _otpVerificationResult

    private val _navigateToPassengerDetails = MutableLiveData<Event<PassengerDetailsNavigationData>>()
    val navigateToPassengerDetails: LiveData<Event<PassengerDetailsNavigationData>> = _navigateToPassengerDetails

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun verifyOtp(
        otpValue: String, 
        tripIdStr: String,
        tripRouteIdStr: String,
        ticketIdsStr: List<String>,
        authToken: String
    ) {
        _isLoading.value = true

        Log.d("OtpVerificationVM", "Verifying OTP (booking flow). OTP: $otpValue, AppVersion: $appVersion, DeviceID: $deviceId")
        Log.d("OtpVerificationVM", "Trip ID (String): $tripIdStr, Trip Route ID (String): $tripRouteIdStr")
        Log.d("OtpVerificationVM", "Ticket IDs (String): ${ticketIdsStr.joinToString()}, Auth Token (length): ${authToken.length}")

        viewModelScope.launch {
            try {
                val tripIdLong = tripIdStr.toLong()
                val tripRouteIdLong = tripRouteIdStr.toLong()
                val ticketIdsLong = ticketIdsStr.map { it.toLong() }

                Log.d("OtpVerificationVM", "Converted to Long - Trip ID: $tripIdLong, Trip Route ID: $tripRouteIdLong, Ticket IDs: $ticketIdsLong")

                // Use com.example.railticket.data.model.booking.VerifyOtpRequest
                val request = VerifyOtpRequest(
                    tripId = tripIdLong,
                    tripRouteId = tripRouteIdLong,
                    ticketIds = ticketIdsLong,
                    otp = otpValue
                )
                Log.d("OtpVerificationVM", "Request Body (booking flow): $request")
                
                // Call the verifyOtp in BookingDataSource that expects appVersion and deviceId
                val result = bookingDataSource.verifyOtp(request, appVersion, deviceId)
                _otpVerificationResult.postValue(Event(result))

                // Check result type with explicit generic argument for booking VerifyOtpResponse
                if (result is Result.Success<VerifyOtpResponse>) {
                    if (result.data.data?.success == true || result.data.data?.error == 0) {
                        val user = result.data.data?.user
                        val navData = PassengerDetailsNavigationData(
                            passenger1Name = user?.name,
                            passenger1Email = user?.email,
                            passenger1Mobile = user?.mobile,
                            tripId = tripIdStr,
                            tripRouteId = tripRouteIdStr,
                            ticketIds = ticketIdsStr,
                            authToken = authToken,
                            otp = otpValue
                        )
                        _navigateToPassengerDetails.postValue(Event(navData))
                        Log.i("OtpVerificationVM", "Booking OTP Verification successful. Navigating. User: $user")
                    } else {
                        val errorMessage = result.data.data?.message ?: "Booking OTP verification failed (API indicated error)."
                        Log.w("OtpVerificationVM", "Booking OTP Verification API Error: $errorMessage - Full Result: ${result.data}")
                    }
                } else if (result is Result.Error) {
                    Log.w("OtpVerificationVM", "Booking OTP Verification failed (Result.Error): ${result.message}")
                }

            } catch (e: NumberFormatException) {
                Log.e("OtpVerificationVM", "Error converting ID strings to Long: ${e.message}", e)
                _otpVerificationResult.postValue(
                    Event(
                        Result.Error(
                            IOException("Invalid ID format. Please ensure IDs are valid numbers.", e),
                            "Invalid ID format. Please ensure IDs are valid numbers.",
                            null // No specific error body from NumberFormatException
                        )
                    )
                )
            } catch (e: Exception) {
                Log.e("OtpVerificationVM", "Unexpected error during booking OTP verification: ${e.message}", e)
                _otpVerificationResult.postValue(
                    Event(
                        Result.Error(
                            IOException("An unexpected error occurred during booking OTP verification.", e),
                            "An unexpected error occurred. Please try again.",
                            null // No specific error body
                        )
                    )
                )
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}
