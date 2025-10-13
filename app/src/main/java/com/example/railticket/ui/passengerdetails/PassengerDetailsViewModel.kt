package com.example.railticket.ui.passengerdetails

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.railticket.data.model.booking.ConfirmBookingRequest // Ensure this is the updated one
import com.example.railticket.data.repository.BookingRepository
import com.example.railticket.util.Event
import kotlinx.coroutines.launch
import java.lang.NumberFormatException

class PassengerDetailsViewModel(
    private val bookingRepository: BookingRepository,
    private val defaultAppVersion: String,
    private val defaultDeviceId: String
) : ViewModel() {

    private val _bookingConfirmationState = MutableLiveData<BookingConfirmationUiState>()
    val bookingConfirmationState: LiveData<BookingConfirmationUiState> = _bookingConfirmationState

    private val TAG = "PassengerDetailsVM"

    fun confirmBooking(
        tripIdStr: String,
        tripRouteIdStr: String, // This will be used directly as String
        ticketIdsStr: List<String>,
        boardingPointIdStr: String,
        contactPersonEmail: String,
        contactPersonMobile: String,
        passengerNames: List<String>,
        passengerTypes: List<String>,
        genders: List<String>,
        otpParam: String,
        authToken: String,
        // Parameters now part of ConfirmBookingRequest
        fromCity: String,
        toCity: String,
        dateOfJourney: String,
        seatClass: String,
        latitude: Double? = null,
        longitude: Double? = null,
        ipAddress: String? = null,
        isBkashOnline: Boolean? = null,      // Added for Bkash
        selectedMobileTransaction: Int? = null // Added for Bkash
    ) {
        _bookingConfirmationState.value = BookingConfirmationUiState(isLoading = true)

        viewModelScope.launch {
            try {
                val tripId = tripIdStr.toLong()
                val ticketIds = ticketIdsStr.map { it.toLong() }
                val boardingPointId = boardingPointIdStr.toLong()

                val numberOfPassengers = ticketIds.size
                val emptyListForPassengers = List(numberOfPassengers) { "" }
                val nullListForPassengers = List(numberOfPassengers) { null as String? }

                val request = ConfirmBookingRequest(
                    tripId = tripId,
                    tripRouteId = tripRouteIdStr,
                    ticketIds = ticketIds,
                    boardingPointId = boardingPointId,
                    passengerType = passengerTypes, 
                    passengerEmail = contactPersonEmail,
                    passengerMobile = contactPersonMobile,
                    passengerName = passengerNames,   
                    gender = genders,                 
                    otp = otpParam,
                    fromCity = fromCity,
                    toCity = toCity,
                    dateOfJourney = dateOfJourney,
                    seatClass = seatClass,
                    latitude = latitude,
                    longitude = longitude,
                    ipAddress = ipAddress,
                    page = emptyListForPassengers,
                    passengerPassport = emptyListForPassengers,
                    firstName = nullListForPassengers,
                    middleName = nullListForPassengers,
                    lastName = nullListForPassengers,
                    dateOfBirth = nullListForPassengers,
                    nationality = nullListForPassengers,
                    passportType = nullListForPassengers,
                    passportNo = nullListForPassengers,
                    passportExpiryDate = nullListForPassengers,
                    visaType = nullListForPassengers,
                    visaNo = nullListForPassengers,
                    visaIssuePlace = nullListForPassengers,
                    visaIssueDate = nullListForPassengers,
                    visaExpireDate = nullListForPassengers,
                    // Pass new Bkash parameters to the request object
                    isBkashOnline = isBkashOnline,
                    selectedMobileTransaction = selectedMobileTransaction
                    // Other fields like contactPerson, priyojonOrderId, etc., 
                    // will use defaults from ConfirmBookingRequest if not specified here.
                )

                Log.d(TAG, "ConfirmBookingRequest (Kotlin object): $request")

                val response = bookingRepository.confirmBooking(
                    authToken,
                    defaultAppVersion,
                    defaultDeviceId,
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
