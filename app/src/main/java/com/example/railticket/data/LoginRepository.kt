package com.example.railticket.data

import android.util.Log
import com.example.railticket.data.model.LoggedInUser
import com.example.railticket.data.model.OtpRequest
import com.example.railticket.data.model.OtpResponseData
import com.example.railticket.data.model.VerifyOtpRequest
import com.example.railticket.data.model.VerifyOtpResponse
import com.example.railticket.data.model.ErrorResponse
import com.google.gson.Gson

class LoginRepository private constructor(
    private val dataSource: LoginDataSource,
    private val gson: Gson
) {

    private val TAG = "LoginRepository"

    var user: LoggedInUser? = null
        private set

    val isLoggedIn: Boolean
        get() {
            val loggedIn = TokenManager.authToken != null
            Log.d(TAG, "isLoggedIn check: $loggedIn. TokenManager has token: ${TokenManager.authToken != null}")
            return loggedIn
        }

    fun logout() {
        Log.d(TAG, "logout() called. Clearing user and TokenManager token.")
        user = null
        TokenManager.authToken = null // Clear the global token
        dataSource.logout() 
    }

    suspend fun login(
        appVersion: String, 
        deviceId: String,   
        mobileNumber: String,
        password: String
    ): Result<LoggedInUser> {
        Log.d(TAG, "login() called in repository with mobile: $mobileNumber")
        val result = dataSource.login(mobileNumber, password) 
        if (result is Result.Success) {
            setLoggedInUser(result.data)
            TokenManager.authToken = result.data.token
        } else if (result is Result.Error) {
            Log.w(TAG, "Login failed in repository. Error: ${result.message}")
        }
        return result
    }

    private fun setLoggedInUser(loggedInUser: LoggedInUser) {
        this.user = loggedInUser
        Log.i(TAG, "setLoggedInUser: User set. DisplayName: ${loggedInUser.displayName}, Token: ${loggedInUser.token?.take(15)}...")
    }

    fun getAuthToken(): String? {
        val token = TokenManager.authToken
        Log.d(TAG, "getAuthToken() called. Returning token from TokenManager. Token is ${if (token == null) "null" else "NOT null"}.")
        return token
    }

    suspend fun requestOtp(
        appVersion: String,
        deviceId: String,
        authToken: String,
        otpRequest: OtpRequest
    ): Result<OtpResponseData> {
        Log.d(TAG, "requestOtp() called in repository. Token: ${authToken.take(15)}...")
        return dataSource.requestOtp(appVersion, deviceId, authToken, otpRequest)
    }

    suspend fun verifyOtp(
        appVersion: String,
        deviceId: String,
        authToken: String,
        verifyOtpRequest: VerifyOtpRequest
    ): Result<VerifyOtpResponse> {
        Log.d(TAG, "verifyOtp() called in repository. Token: ${authToken.take(15)}...")
        return dataSource.verifyOtp(appVersion, deviceId, authToken, verifyOtpRequest)
    }

    fun parseErrorBody(errorBody: String?): ErrorResponse? {
        return errorBody?.let {
            try {
                gson.fromJson(it, ErrorResponse::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing error body: $it", e)
                null
            }
        }
    }

    companion object {
        @Volatile private var instance: LoginRepository? = null

        fun getInstance(dataSource: LoginDataSource, gson: Gson): LoginRepository {
            return instance ?: synchronized(this) {
                instance ?: LoginRepository(dataSource, gson).also { 
                    instance = it
                    Log.d("LoginRepository", "getInstance: LoginRepository instance created.")
                }
            }
        }
    }
}
