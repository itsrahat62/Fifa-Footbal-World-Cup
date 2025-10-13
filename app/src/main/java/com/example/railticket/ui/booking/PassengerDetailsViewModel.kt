package com.example.railticket.ui.booking

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.railticket.data.model.booking.ConfirmBookingRequest
import com.example.railticket.data.repository.BookingRepository
import com.example.railticket.util.Event
import kotlinx.coroutines.launch

class PassengerDetailsViewModel(
    private val bookingRepository: BookingRepository,
    private val appVersion: String,
    private val deviceId: String
) : ViewModel() {

    private val _navigateToPaymentUrl = MutableLiveData<Event<String>>()
    val navigateToPaymentUrl: LiveData<Event<String>> = _navigateToPaymentUrl

    private val _confirmBookingError = MutableLiveData<Event<String>>()
    val confirmBookingError: LiveData<Event<String>> = _confirmBookingError

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val TAG = "PassengerDetailsVM_Booking"

    fun executeConfirmBooking(
        tripId: Long,
        tripRouteId: Long,
        ticketIds: List<Long>,
        boardingPointId: Long,
        passengerTypeInput: List<String>, // Dynamic list of types from Fragment
        passengerEmail: String,
        passengerMobile: String,
        passengerNameInput: List<String>, // Dynamic list of names from Fragment
        genderInput: List<String>,        // Dynamic list of genders from Fragment
        otp: String,
        authToken: String,
        fromCity: String,
        toCity: String,
        dateOfJourney: String,
        seatClass: String,
        latitude: Double? = null,
        longitude: Double? = null,
        ipAddress: String? = null
    ) {
        _isLoading.value = true
        viewModelScope.launch {
            val numberOfPassengers = ticketIds.size
            val emptyListForPassengers = List(numberOfPassengers) { "" }
            val nullListForPassengers = List(numberOfPassengers) { null as String? }

            val finalPassengerNames = mutableListOf<String>()
            val finalGenders = mutableListOf<String>()
            val finalPassengerTypes = mutableListOf<String>()

            for (i in 0 until numberOfPassengers) {
                finalPassengerNames.add(passengerNameInput.getOrElse(i) { "Passenger ${i + 1}" }) // Use name from input list, default if missing
                finalGenders.add(genderInput.getOrElse(i) { "male" }) // Use gender from input list, default if missing
                finalPassengerTypes.add(passengerTypeInput.getOrElse(i) { "Adult" }) // Use type from input list, default if missing
            }
            
            val request = ConfirmBookingRequest(
                tripId = tripId,
                tripRouteId = tripRouteId.toString(),
                ticketIds = ticketIds,
                boardingPointId = boardingPointId,
                passengerType = finalPassengerTypes, // Fully dynamic list
                passengerEmail = passengerEmail,
                passengerMobile = passengerMobile,
                passengerName = finalPassengerNames,   // Fully dynamic list
                gender = finalGenders,                 // Fully dynamic list
                otp = otp,
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
                visaExpireDate = nullListForPassengers
            )

            Log.d(TAG, "Executing confirmBooking. Final Names: $finalPassengerNames, Final Genders: $finalGenders, Final Types: $finalPassengerTypes")
            Log.d(TAG, "AuthToken: ${authToken.take(15)}..., AppVersion: $appVersion, DeviceID: $deviceId, Request (Kotlin Object): $request") // Changed log message slightly for clarity

            try {
                val response = bookingRepository.confirmBooking(
                    authToken = authToken,
                    appVersion = appVersion,
                    deviceId = deviceId,
                    request = request
                )

                if (response.isSuccessful) {
                    val confirmBookingResponse = response.body()
                    if (confirmBookingResponse?.data?.redirectUrl != null) {
                        Log.i(TAG, "Confirm booking SUCCESS. Message: ${confirmBookingResponse.data.message}, Redirect URL: ${confirmBookingResponse.data.redirectUrl}")
                        _navigateToPaymentUrl.postValue(Event(confirmBookingResponse.data.redirectUrl!!))
                    } else {
                        val errorMessage = confirmBookingResponse?.data?.message ?: "Booking confirmed but no redirect URL provided."
                        Log.w(TAG, "Confirm booking API OK, but issue: $errorMessage. Full response: $confirmBookingResponse")
                        _confirmBookingError.postValue(Event(errorMessage))
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = "Confirm booking API error: ${response.code()} - ${errorBody ?: "Unknown error"}"
                    Log.e(TAG, errorMessage)
                    _confirmBookingError.postValue(Event(errorMessage))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Confirm booking exception: ${e.message}", e)
                _confirmBookingError.postValue(Event("Confirm booking failed: ${e.message ?: "Network error"}"))
            }
            _isLoading.postValue(false)
        }
    }
}
