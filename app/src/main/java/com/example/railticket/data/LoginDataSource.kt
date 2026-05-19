package com.example.railticket.data

import android.util.Log
import com.example.railticket.data.model.LoggedInUser
import com.example.railticket.data.model.LoginRequest
import com.example.railticket.data.model.LoginResponse
import com.example.railticket.data.model.OtpRequest
import com.example.railticket.data.model.OtpResponseData
import com.example.railticket.data.model.VerifyOtpRequest
import com.example.railticket.data.model.VerifyOtpResponse
import com.example.railticket.data.model.ErrorResponse
import com.example.railticket.data.model.getPrimaryErrorMessage
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Scanner

class LoginDataSource {

    private val gson = Gson()
    private val TAG = "LoginDataSource"

    private val WEB_AUTH_BASE_URL = "https://railspaapi.shohoz.com/v1.0/web/auth"
    private val APP_BOOKINGS_BASE_URL = "https://railspaapi.shohoz.com/v1.0/app/bookings"

    suspend fun login(mobileNumber: String, password: String): Result<LoggedInUser> =
        withContext(Dispatchers.IO) {
            try {
                val url = URL("$WEB_AUTH_BASE_URL/sign-in")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val loginRequest = LoginRequest(mobileNumber, password)
                val jsonRequestBody = gson.toJson(loginRequest)
                Log.d(TAG, "Login Request Body: $jsonRequestBody")

                OutputStreamWriter(connection.outputStream).use {
                    it.write(jsonRequestBody)
                    it.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseBody = Scanner(connection.inputStream).useDelimiter("\\A").next()
                    Log.d(TAG, "Login Successful Response Body: $responseBody")
                    val loginResponse = gson.fromJson(responseBody, LoginResponse::class.java)

                    if (loginResponse.data?.token != null) {
                        val token = loginResponse.data.token!!
                        val placeholderDisplayName = "User" 
                        Log.d(TAG, "Token received. Using placeholder display name: '$placeholderDisplayName'")
                        val loggedInUser = LoggedInUser(
                            userId = token,
                            displayName = placeholderDisplayName,
                            token = token
                        )
                        Result.Success(loggedInUser, loginResponse.data.message ?: "Login successful!")
                    } else {
                        val apiErrorMessage = loginResponse.data?.message ?: "Login successful but token missing."
                        Log.w(TAG, "Login API Warning (Token Missing): $apiErrorMessage - Response: $responseBody")
                        Result.Error(IOException("Login API Error: $apiErrorMessage (token missing)"), apiErrorMessage, responseBody)
                    }
                } else {
                    val errorStream = connection.errorStream
                    val errorResponseBody = errorStream?.let { Scanner(it).useDelimiter("\\A").next() } ?: ""
                    Log.d(TAG, "Login Error Response Body: $errorResponseBody")
                    val parsedError = try { gson.fromJson(errorResponseBody, ErrorResponse::class.java) } catch (e: Exception) { null }
                    val errorMessage = parsedError?.getPrimaryErrorMessage() ?: "Server error (code $responseCode)"
                    Log.w(TAG, "Login failed. HTTP Code: $responseCode - Message: $errorMessage")
                    Result.Error(IOException("Server error: $responseCode - $errorMessage"), errorMessage, errorResponseBody)
                }
            } catch (e: Exception) {
                val exceptionType = e.javaClass.simpleName
                val detailedErrorMessage = e.message ?: "An unexpected error occurred during login."
                Log.e(TAG, "Login Exception ($exceptionType): $detailedErrorMessage", e)
                Result.Error(IOException("Network Error ($exceptionType): $detailedErrorMessage", e), "Network Error: $detailedErrorMessage. Please check connection.")
            }
        }

    suspend fun requestOtp(appVersion: String, deviceId: String, authToken: String, otpRequest: OtpRequest): Result<OtpResponseData> =
        withContext(Dispatchers.IO) {
            Log.d(TAG, "requestOtp called with token: $authToken, request: $otpRequest, AppVersion: $appVersion, DeviceID: $deviceId")
            Result.Error(Exception(NotImplementedError("requestOtp not implemented in DataSource")), "Feature not implemented (requestOtp in DataSource).")
        }

    suspend fun verifyOtp(appVersion: String, deviceId: String, authToken: String, verifyOtpRequest: VerifyOtpRequest): Result<VerifyOtpResponse> =
        withContext(Dispatchers.IO) {
            val urlString = "$APP_BOOKINGS_BASE_URL/verify-otp"
            Log.d(TAG, "verifyOtp called. URL: $urlString, Token: Bearer ${authToken.take(15)}..., Request: $verifyOtpRequest")
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $authToken")
                DeviceKeyManager.deviceKey?.let { deviceKey ->
                    connection.setRequestProperty("X-Device-Key", deviceKey)
                }
                connection.setRequestProperty("X-App-Version", appVersion) 
                connection.setRequestProperty("X-Device-Id", deviceId)
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val jsonRequestBody = gson.toJson(verifyOtpRequest)
                Log.d(TAG, "VerifyOtp Request Body: $jsonRequestBody")

                OutputStreamWriter(connection.outputStream).use {
                    it.write(jsonRequestBody)
                    it.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseBody = Scanner(connection.inputStream).useDelimiter("\\A").next()
                    Log.d(TAG, "VerifyOtp Successful Response Body: $responseBody")
                    val verifyOtpResponse = gson.fromJson(responseBody, VerifyOtpResponse::class.java)
                    if (verifyOtpResponse.data?.success == true || verifyOtpResponse.data?.error == 0) {
                        Result.Success(verifyOtpResponse, verifyOtpResponse.data?.message ?: "OTP Verified Successfully")
                    } else {
                        val apiErrorMessage = verifyOtpResponse.data?.message ?: "OTP verification failed by API (within HTTP OK)."
                        Log.w(TAG, "VerifyOtp API Error (within HTTP OK): $apiErrorMessage - Response: $responseBody")
                        Result.Error(IOException("VerifyOtp API Error: $apiErrorMessage"), apiErrorMessage, responseBody)
                    }
                } else {
                    val errorStream = connection.errorStream
                    val errorResponseBody = errorStream?.let { Scanner(it).useDelimiter("\\A").next() } ?: ""
                    Log.d(TAG, "VerifyOtp Error Response Body: $errorResponseBody")
                    val parsedError = try { gson.fromJson(errorResponseBody, ErrorResponse::class.java) } catch (e: Exception) { null }
                    val errorMessage = parsedError?.getPrimaryErrorMessage() ?: "Server error (code $responseCode) for OTP verification."
                    Log.w(TAG, "VerifyOtp failed. HTTP Code: $responseCode - Message: $errorMessage")
                    Result.Error(IOException("Server error: $responseCode - $errorMessage"), errorMessage, errorResponseBody)
                }
            } catch (e: Exception) {
                val exceptionType = e.javaClass.simpleName
                val detailedErrorMessage = e.message ?: "An unexpected error occurred during OTP verification."
                Log.e(TAG, "VerifyOtp Exception ($exceptionType): $detailedErrorMessage", e)
                Result.Error(IOException("Network Error ($exceptionType): $detailedErrorMessage", e), "Network Error: $detailedErrorMessage. Please check connection.")
            }
        }

    fun logout() {
        Log.d(TAG, "Logout called")
    }
}
