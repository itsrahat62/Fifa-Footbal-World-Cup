package com.example.railticket.ui.login

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.railticket.data.LoginRepository
import com.example.railticket.data.Result
import com.example.railticket.data.model.LoggedInUser
import com.example.railticket.data.model.LoginForm
import com.example.railticket.data.model.OtpRequest
import com.example.railticket.data.model.OtpResponseData
import com.example.railticket.data.model.VerifyOtpRequest // Uses Long IDs now
import com.example.railticket.data.model.VerifyOtpResponse
import com.example.railticket.data.model.ErrorResponse
import com.example.railticket.data.model.getPrimaryErrorMessage
import com.example.railticket.util.Event
import kotlinx.coroutines.launch
import java.lang.NumberFormatException // Import for NumberFormatException

data class PassengerDetailsNavigationArgs(
    val tripId: String, // Keep as String for nav args, conversion happens in ViewModel
    val tripRouteId: String,
    val ticketIds: Array<String>,
    val boardingPointId: String,
    val sessionAuthToken: String,
    val userName: String,
    val userEmail: String,
    val userMobile: String,
    val submittedOtp: String
)

class LoginViewModel(
    private val loginRepository: LoginRepository,
    private val defaultAppVersion: String,
    private val defaultDeviceId: String
) : ViewModel() {

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginForm

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    private val _otpRequestResult = MutableLiveData<Event<OtpResult>>()
    val otpRequestResult: LiveData<Event<OtpResult>> = _otpRequestResult

    private val _verifyOtpResult = MutableLiveData<Event<VerifyOtpResult>>()
    val verifyOtpResult: LiveData<Event<VerifyOtpResult>> = _verifyOtpResult

    private val _navigateToPassengerDetails = MutableLiveData<Event<PassengerDetailsNavigationArgs>>()
    val navigateToPassengerDetails: LiveData<Event<PassengerDetailsNavigationArgs>> = _navigateToPassengerDetails

    private val TAG = "LoginViewModel"

    init {
        Log.d(TAG, "LoginViewModel instance created. LoginRepository hash: ${loginRepository.hashCode()}")
    }

    fun login(loginForm: LoginForm) {
        viewModelScope.launch {
            Log.d(TAG, "login called with mobile: ${loginForm.mobileNumber}")
            val result = loginRepository.login(
                defaultAppVersion,
                defaultDeviceId,
                loginForm.mobileNumber,
                loginForm.password
            )

            if (result is Result.Success<LoggedInUser>) {
                val successData = LoggedInUserView(
                    displayName = result.data.displayName,
                    token = result.data.token,
                    message = result.message ?: "Login successful!"
                )
                _loginResult.value = LoginResult(success = successData)
                Log.i(TAG, "Login success. _loginResult updated. Token: ${result.data.token?.take(15)}...")
            } else if (result is Result.Error) {
                var errorMessage = result.message ?: "Login Failed"
                if (result.errorBody != null) {
                    val errorResponse = loginRepository.parseErrorBody(result.errorBody)
                    errorMessage = errorResponse?.getPrimaryErrorMessage() ?: errorMessage
                }
                _loginResult.value = LoginResult(error = errorMessage)
                Log.w(TAG, "Login error. _loginResult updated with error: $errorMessage")
            }
        }
    }

    fun requestOtp(
        @Suppress("UNUSED_PARAMETER") authTokenFromArgs: String, 
        tripId: String,
        tripRouteId: String,
        ticketIds: List<String>,
        boardingPointId: String
    ) {
        viewModelScope.launch {
            val currentAuthToken = loginRepository.getAuthToken()
            Log.d(TAG, "requestOtp: Retrieved authToken from repository: ${currentAuthToken?.take(15)}...")
            if (currentAuthToken == null) {
                Log.w(TAG, "requestOtp: Auth token from repository is null. User not authenticated.")
                _otpRequestResult.value = Event(OtpResult(error = "User not authenticated. Please log in again."))
                return@launch
            }

            val otpRequest = OtpRequest(
                tripId = tripId,
                tripRouteId = tripRouteId,
                ticketIds = ticketIds,
                boardingPointId = boardingPointId
            )
            val result = loginRepository.requestOtp(
                defaultAppVersion,
                defaultDeviceId,
                currentAuthToken,
                otpRequest
            )

            if (result is Result.Success<OtpResponseData>) {
                _otpRequestResult.value = Event(OtpResult(success = true, message = result.data.innerData?.message ?: result.data.message ?: "OTP Sent"))
            } else if (result is Result.Error) {
                var errorMessage = result.message ?: "OTP Request Failed"
                if (result.errorBody != null) {
                    val errorResponse = loginRepository.parseErrorBody(result.errorBody)
                    errorMessage = errorResponse?.getPrimaryErrorMessage() ?: errorMessage
                }
                _otpRequestResult.value = Event(OtpResult(error = errorMessage))
            }
        }
    }

    fun verifyOtp(
        otp: String,
        tripIdStr: String, // Renamed to indicate it's a String from args
        tripRouteIdStr: String, // Renamed
        ticketIdsStr: List<String>, // Renamed
        boardingPointId: String 
    ) {
        viewModelScope.launch {
            val authToken = loginRepository.getAuthToken()
            Log.d(TAG, "verifyOtp: Retrieved authToken: ${authToken?.take(15)}...")

            if (authToken == null) {
                Log.w(TAG, "verifyOtp: Auth token null. User not authenticated.")
                _verifyOtpResult.value = Event(VerifyOtpResult(error = "User not authenticated. Please log in again."))
                return@launch
            }

            val tripIdLong: Long
            val tripRouteIdLong: Long
            val ticketIdsLong: List<Long>

            try {
                tripIdLong = tripIdStr.toLong()
                tripRouteIdLong = tripRouteIdStr.toLong()
                ticketIdsLong = ticketIdsStr.map { it.toLong() }
                Log.d(TAG, "verifyOtp: Converted IDs to Long. TripID: $tripIdLong, RouteID: $tripRouteIdLong, TicketIDs: $ticketIdsLong")
            } catch (e: NumberFormatException) {
                Log.e(TAG, "verifyOtp: Error converting string IDs to Long: ${e.message}", e)
                _verifyOtpResult.value = Event(VerifyOtpResult(error = "Invalid ID format. Please try again."))
                return@launch
            }

            Log.d(TAG, "verifyOtp: Proceeding with token. OTP: $otp, Converted TripID: $tripIdLong")
            val verifyOtpRequest = VerifyOtpRequest(
                tripId = tripIdLong,
                tripRouteId = tripRouteIdLong,
                ticketIds = ticketIdsLong,
                otp = otp
            )

            val result = loginRepository.verifyOtp(
                defaultAppVersion,
                defaultDeviceId,
                authToken,
                verifyOtpRequest
            )

            if (result is Result.Success<VerifyOtpResponse>) {
                val verifyOtpData = result.data.data
                if (verifyOtpData?.success == true || verifyOtpData?.error == 0) {
                    val user = verifyOtpData.user
                    if (user?.name != null && user.email != null && user.mobile != null) {
                        val navArgs = PassengerDetailsNavigationArgs(
                            tripId = tripIdStr, // Use original String args for navigation
                            tripRouteId = tripRouteIdStr,
                            ticketIds = ticketIdsStr.toTypedArray(),
                            boardingPointId = boardingPointId, 
                            sessionAuthToken = authToken, 
                            userName = user.name,
                            userEmail = user.email,
                            userMobile = user.mobile,
                            submittedOtp = otp
                        )
                        _navigateToPassengerDetails.value = Event(navArgs)
                        _verifyOtpResult.value = Event(VerifyOtpResult(success = true, message = verifyOtpData.message ?: "OTP Verified"))
                        Log.i(TAG, "verifyOtp success. Navigating. Token: ${authToken.take(15)}...")
                    } else {
                        _verifyOtpResult.value = Event(VerifyOtpResult(error = "User details missing in OTP response."))
                        Log.w(TAG, "verifyOtp: User details missing. Data: $verifyOtpData")
                    }
                } else {
                    val errMessage = verifyOtpData?.message ?: "OTP Verification Failed by API."
                    _verifyOtpResult.value = Event(VerifyOtpResult(error = errMessage))
                    Log.w(TAG, "verifyOtp: API indicated failure: $errMessage. Data: $verifyOtpData")
                }
            } else if (result is Result.Error) {
                var errorMessage = result.message ?: "OTP Verification Failed"
                if (result.errorBody != null) {
                    val errorResponse = loginRepository.parseErrorBody(result.errorBody)
                    errorMessage = errorResponse?.getPrimaryErrorMessage() ?: errorMessage
                }
                _verifyOtpResult.value = Event(VerifyOtpResult(error = errorMessage))
                Log.w(TAG, "verifyOtp: Error result from repository: $errorMessage")
            }
        }
    }

    fun loginDataChanged(loginForm: LoginForm) {
        var isValid = true
        var mobileError: String? = null
        var passwordError: String? = null

        if (!isMobileNumberValid(loginForm.mobileNumber)) {
            mobileError = "Invalid mobile number"
            isValid = false
        }
        if (!isPasswordValid(loginForm.password)) {
            passwordError = "Password must be >5 characters"
            isValid = false
        }
        _loginForm.value = LoginFormState(
            mobileNumberError = mobileError,
            passwordError = passwordError,
            isDataValid = isValid
        )
    }

    private fun isMobileNumberValid(mobileNumber: String): Boolean {
        return mobileNumber.isNotBlank() && mobileNumber.length >= 11
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length > 5
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "LoginViewModel instance cleared.")
    }
}
