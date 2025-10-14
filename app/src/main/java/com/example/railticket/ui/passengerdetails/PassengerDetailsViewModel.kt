package com.example.railticket.ui.passengerdetails

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.railticket.data.model.booking.ConfirmBookingRequest
import com.example.railticket.data.repository.BookingRepository
import com.example.railticket.util.Event
import kotlinx.coroutines.launch
import java.lang.NumberFormatException

class PassengerDetailsViewModel(
    private val bookingRepository: BookingRepository,
    private val defaultDeviceId: String
) : ViewModel() {

    private val _bookingConfirmationState = MutableLiveData<BookingConfirmationUiState>()
    val bookingConfirmationState: LiveData<BookingConfirmationUiState> = _bookingConfirmationState

    private val TAG = "PassengerDetailsVM"

    fun confirmBooking(
        tripId: Long,
        tripRouteId: Long,
        ticketIds: List<Long>,
        boardingPointId: Long,
        passengerTypes: List<String>,
        contactEmail: String,
        contactMobile: String,
        passengerNames: List<String>,
        genders: List<String>,
        selectedMobileTransaction: String,
        otp: String,
        isBkashOnline: Boolean, // Corrected to non-nullable Boolean
        authToken: String,
        deviceKey: String
    ) {
        _bookingConfirmationState.value = BookingConfirmationUiState(isLoading = true)

        viewModelScope.launch {
            try {
                val request = ConfirmBookingRequest(
                    tripId = tripId,
                    tripRouteId = tripRouteId,
                    ticketIds = ticketIds,
                    boardingPointId = boardingPointId,
                    contactPerson = 0,
                    passengerType = passengerTypes,
                    passengerEmail = contactEmail,
                    passengerMobile = contactMobile,
                    passengerName = passengerNames,
                    gender = genders,
                    selectedMobileTransaction = selectedMobileTransaction,
                    otp = otp,
                    isBkashOnline = isBkashOnline
                )

                Log.d(TAG, "Executing MINIMAL confirmBooking. Request: $request")

                val response = bookingRepository.confirmBooking(
                    authToken,
                    defaultDeviceId,
                    deviceKey,
                    request
                )

                if (response.isSuccessful && response.body() != null) {
                    val responseData = response.body()?.data
                    if (responseData?.redirectUrl != null) {
                        _bookingConfirmationState.value = BookingConfirmationUiState(successUrl = Event(responseData.redirectUrl))
                    } else {
                        _bookingConfirmationState.value = BookingConfirmationUiState(error = Event(responseData?.message ?: "Booking confirmed, but no redirect URL."))
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: response.message() ?: "Booking confirmation failed"
                    Log.e(TAG, "Booking confirmation failed. Error: $errorMsg, Response: $response")
                    _bookingConfirmationState.value = BookingConfirmationUiState(error = Event(errorMsg))
                }
            } catch (nfe: NumberFormatException) {
                Log.e(TAG, "Invalid ID format for booking", nfe)
                _bookingConfirmationState.value = BookingConfirmationUiState(error = Event("Invalid ID format provided for booking."))
            } catch (e: Exception) {
                Log.e(TAG, "Exception during booking confirmation", e)
                _bookingConfirmationState.value = BookingConfirmationUiState(error = Event(e.message ?: "An unexpected error occurred"))
            }
        }
    }
}
