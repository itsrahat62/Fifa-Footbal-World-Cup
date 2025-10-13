package com.example.railticket.ui.trainsearch

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.railticket.data.LoginRepository
import com.example.railticket.data.Result
import com.example.railticket.data.model.booking.PassengerDetailsRequest
import com.example.railticket.data.model.booking.ReserveSeatRequest
import kotlinx.coroutines.launch

// Event class remains the same
open class Event<out T>(private val content: T) {
    var hasBeenHandled = false
        private set

    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }
    fun peekContent(): T = content
}

// BookingProgress remains useful for general loading/error states not tied to S vs N
data class BookingProgress(
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: Event<String>? = null,
    val success: Event<String>? = null // Can be used for OTP success/failure messages
)

// This data class will hold the outcome of the seat reservation sweep
data class SeatReservationAttemptDetails(
    val requestedSeatsN: Int,
    val reservedSeatsS: Int,
    val reservedTicketIds: List<Long>,
    // val reservationHash: String?, // Removed as per user confirmation
    val error: String? = null,
    val requiresUserConfirmation: Boolean = false, // Kept for logic, though Fragment might ignore it
    // Data needed to proceed to OTP/next step
    val tripId: String,
    val tripRouteId: String,
    val authToken: String,
    val boardingPointId: String,
    val fromCity: String,
    val toCity: String,
    val dateOfJourney: String,
    val seatClass: String
)

// OtpNavigationDataFromSearch remains the same for actual navigation
data class OtpNavigationDataFromSearch(
    val tripId: String,
    val tripRouteId: String, 
    val ticketIds: Array<String>,
    val authToken: String,
    val numberOfSeats: Int, // This will be S (actual reserved count)
    val boardingPointId: String,
    val fromCity: String,
    val toCity: String,
    val dateOfJourney: String,
    val seatClass: String
)

class TrainSearchViewModel(
    private val bookingDataSource: com.example.railticket.data.BookingDataSource,
    private val loginRepository: LoginRepository
) : ViewModel() {

    private val _bookingState = MutableLiveData<BookingProgress>()
    val bookingState: LiveData<BookingProgress> = _bookingState

    private val _seatReservationAttemptEvent = MutableLiveData<Event<SeatReservationAttemptDetails>>()
    val seatReservationAttemptEvent: LiveData<Event<SeatReservationAttemptDetails>> = _seatReservationAttemptEvent

    private val _navigateToOtpEvent = MutableLiveData<Event<OtpNavigationDataFromSearch>>()
    val navigateToOtpEvent: LiveData<Event<OtpNavigationDataFromSearch>> = _navigateToOtpEvent
    
    private val TAG = "TrainSearchViewModel"

    fun clearBookingProgressState() {
        _bookingState.value = BookingProgress()
    }

    fun startBookingProcess(
        fromCity: String, 
        toCity: String,
        dateOfJourney: String,
        seatClassForSearch: String,
        targetTrainNameFromInput: String,
        targetSeatClassForBookingFromInput: String,
        numberOfSeatsToBookN: Int // Renamed to N for clarity
    ) {
        Log.d(TAG, "startBookingProcess called with numberOfSeatsToBookN: $numberOfSeatsToBookN")
        viewModelScope.launch {
            _bookingState.value = BookingProgress(isLoading = true, message = "Starting booking process...")

            val token = loginRepository.getAuthToken()
            if (token == null) {
                _bookingState.value = BookingProgress(isLoading = false, error = Event("Authentication token not found. Please log in again."))
                return@launch
            }

            if (numberOfSeatsToBookN <= 0) {
                Log.w(TAG, "Invalid number of seats requested: $numberOfSeatsToBookN.")
                _bookingState.value = BookingProgress(isLoading = false, error = Event("Please request a valid number of seats (more than 0)."))
                return@launch
            }

            _bookingState.value = BookingProgress(isLoading = true, message = "Searching for trips...")
            val searchResult = bookingDataSource.searchTrips(fromCity, toCity, dateOfJourney, seatClassForSearch, token)

            if (searchResult is Result.Error) {
                _bookingState.value = BookingProgress(isLoading = false, error = Event(searchResult.message ?: "Failed to search trips."))
                return@launch
            }

            val searchTripsResponseData = (searchResult as Result.Success).data.data
            val targetTrain = searchTripsResponseData?.trains?.find { it.trip_number == targetTrainNameFromInput }
            if (targetTrain == null) {
                _bookingState.value = BookingProgress(isLoading = false, error = Event("Target train '$targetTrainNameFromInput' not found."))
                return@launch
            }

            val firstBoardingPoint = targetTrain.boarding_points?.firstOrNull()
            if (firstBoardingPoint == null || firstBoardingPoint.trip_point_id == null) {
                _bookingState.value = BookingProgress(isLoading = false, error = Event("Boarding point information not found for train '${targetTrain.trip_number}'."))
                return@launch
            }
            val numericBoardingPointId = firstBoardingPoint.trip_point_id

            val targetSeatType = targetTrain.seat_types?.find { it.type == targetSeatClassForBookingFromInput }
            if (targetSeatType == null || targetSeatType.trip_id == null || targetSeatType.trip_route_id == null) {
                _bookingState.value = BookingProgress(isLoading = false, error = Event("Target seat type '$targetSeatClassForBookingFromInput' not found or crucial IDs are missing in train '${targetTrain.trip_number}'."))
                return@launch
            }
            val apiTripId: Long = targetSeatType.trip_id
            val apiTripRouteIdLong: Long = targetSeatType.trip_route_id
            
            _bookingState.value = BookingProgress(isLoading = true, message = "Fetching seat layout for ${targetTrain.trip_number}...")
            val seatLayoutResult = bookingDataSource.getSeatLayout(apiTripId, apiTripRouteIdLong.toString(), token)

            if (seatLayoutResult is Result.Error) {
                _bookingState.value = BookingProgress(isLoading = false, error = Event(seatLayoutResult.message ?: "Failed to fetch seat layout."))
                return@launch
            }

            val seatLayoutDetails = (seatLayoutResult as Result.Success).data.data 
            if (seatLayoutDetails?.seatLayout == null) {
                Log.e(TAG, "SeatLayoutDetails or seatLayout array is null.")
                _bookingState.value = BookingProgress(isLoading = false, error = Event("Failed to retrieve valid seat layout structure."))
                return@launch
            }

            val allAvailableTicketIds = mutableListOf<Long>()
            seatLayoutDetails.seatLayout.forEach { floor ->
                floor.layout?.forEach { row ->
                    row.forEach { seat ->
                        if (seat.isHidden == false && seat.seat_availability == 1 && seat.ticket_id != null) {
                            allAvailableTicketIds.add(seat.ticket_id)
                        }
                    }
                }
            }

            if (allAvailableTicketIds.isEmpty()) {
                _bookingState.value = BookingProgress(isLoading = false, error = Event("No available seats found for '$targetSeatClassForBookingFromInput'."))
                return@launch
            }
            Log.d(TAG, "Found ${allAvailableTicketIds.size} available seats initially.")

            val successfullyReservedTicketIdsS = mutableListOf<Long>()
            // var latestReservationHash: String? = null // Removed
            var seatsAttempted = 0

            _bookingState.value = BookingProgress(isLoading = true, message = "Attempting to reserve $numberOfSeatsToBookN seat(s)..." )

            for (ticketIdToTry in allAvailableTicketIds) {
                if (successfullyReservedTicketIdsS.size >= numberOfSeatsToBookN) {
                    Log.d(TAG, "Successfully reserved desired $numberOfSeatsToBookN seats. Halting further attempts.")
                    break 
                }
                seatsAttempted++
                Log.d(TAG, "Reserving seat ${successfullyReservedTicketIdsS.size + 1} of $numberOfSeatsToBookN (Attempt $seatsAttempted/${allAvailableTicketIds.size}, ID: $ticketIdToTry)..." )
                
                val reserveResult = bookingDataSource.reserveSeat(
                    ReserveSeatRequest(ticketId = ticketIdToTry, routeId = apiTripRouteIdLong), 
                    token
                )

                when (reserveResult) {
                    is Result.Error -> {
                        Log.w(TAG, "Failed to reserve seat ID $ticketIdToTry (Network/IO Error): ${reserveResult.message}")
                    }
                    is Result.Success -> {
                        val reserveResponseData = reserveResult.data.data 
                        if (reserveResponseData?.error == 0) { 
                            Log.i(TAG, "Successfully reserved seat ID $ticketIdToTry. Message: ${reserveResponseData.message}")
                            successfullyReservedTicketIdsS.add(ticketIdToTry)
                            // latestReservationHash = reserveResult.data.extra?.get("hash") // Removed
                        } else {
                            Log.w(TAG, "API error reserving seat ID $ticketIdToTry: ${reserveResponseData?.message} (Code: ${reserveResponseData?.error})")
                        }
                    }
                }
            }

            val actualReservedCountS = successfullyReservedTicketIdsS.size
            var outcomeError: String? = null
            if (actualReservedCountS == 0) {
                outcomeError = "Could not reserve any of the $numberOfSeatsToBookN requested seats. All attempts failed or no seats met the request."
                Log.e(TAG, outcomeError)
            }

            val attemptDetails = SeatReservationAttemptDetails(
                requestedSeatsN = numberOfSeatsToBookN,
                reservedSeatsS = actualReservedCountS,
                reservedTicketIds = ArrayList(successfullyReservedTicketIdsS),
                // reservationHash = latestReservationHash, // Removed
                error = outcomeError,
                requiresUserConfirmation = actualReservedCountS > 0 && actualReservedCountS < numberOfSeatsToBookN,
                tripId = apiTripId.toString(),
                tripRouteId = apiTripRouteIdLong.toString(),
                authToken = token,
                boardingPointId = numericBoardingPointId.toString(),
                fromCity = fromCity,
                toCity = toCity,
                dateOfJourney = dateOfJourney,
                seatClass = targetSeatClassForBookingFromInput
            )
            _seatReservationAttemptEvent.postValue(Event(attemptDetails))
            _bookingState.value = BookingProgress(isLoading = false) 
        }
    }

    fun proceedToOtpStage(details: SeatReservationAttemptDetails) {
        viewModelScope.launch {
            _bookingState.value = BookingProgress(isLoading = true, message = "Initiating OTP for ${details.reservedSeatsS} seat(s)...")
            
            // No longer need to check for details.reservationHash
            if (details.reservedSeatsS <= 0) { 
                Log.e(TAG, "proceedToOtpStage called with 0 reserved seats.")
                 _bookingState.value = BookingProgress(isLoading = false, error = Event("Internal error: No seats to proceed with for OTP."))
                return@launch
            }

            val passengerDetailsRequest = PassengerDetailsRequest(
                tripId = details.tripId.toLong(), 
                tripRouteId = details.tripRouteId.toLong(), 
                ticketIds = details.reservedTicketIds,
                hash = null // Explicitly setting to null, or remove if field is optional in data class
            )
            
            val passengerDetailsResultFromApi = bookingDataSource.sendPassengerDetails(
                request = passengerDetailsRequest, 
                token = details.authToken,
                appVersion = "1.0.0", // Placeholder - Consider making this dynamic
                deviceId = "placeholder-device-id" // Placeholder - Consider making this dynamic
            )

            if (passengerDetailsResultFromApi is Result.Error) {
                _bookingState.value = BookingProgress(isLoading = false, error = Event(passengerDetailsResultFromApi.message ?: "Failed to send passenger details for OTP."))
                return@launch
            }

            val passengerDetailsResponseData = (passengerDetailsResultFromApi as Result.Success).data.data
            if (passengerDetailsResponseData?.success == true && passengerDetailsResponseData.message == "Otp is sent") {
                val navData = OtpNavigationDataFromSearch(
                    tripId = details.tripId,
                    tripRouteId = details.tripRouteId,
                    ticketIds = details.reservedTicketIds.map { it.toString() }.toTypedArray(),
                    authToken = details.authToken,
                    numberOfSeats = details.reservedSeatsS, 
                    boardingPointId = details.boardingPointId,
                    fromCity = details.fromCity,
                    toCity = details.toCity,
                    dateOfJourney = details.dateOfJourney,
                    seatClass = details.seatClass
                )
                _navigateToOtpEvent.postValue(Event(navData))
                _bookingState.value = BookingProgress(isLoading = false, success = Event("OTP Sent for ${details.reservedSeatsS} seat(s)."))
            } else {
                val errorMsg = passengerDetailsResponseData?.message ?: "OTP initiation failed."
                Log.e(TAG, "OTP initiation failed from API. Message: $errorMsg, Success Flag: ${passengerDetailsResponseData?.success}")
                _bookingState.value = BookingProgress(isLoading = false, error = Event("Failed to initiate OTP: $errorMsg"))
            }
        }
    }
}
