package com.example.railticket.data.network

import com.example.railticket.data.DeviceKeyManager
import com.example.railticket.data.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class AuthInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        // Wait for the token to be available, with a timeout.
        for (i in 1..5) {
            if (TokenManager.authToken != null) {
                break
            }
            Thread.sleep(500)
        }

        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        TokenManager.authToken?.let {
            requestBuilder.addHeader("Authorization", "Bearer $it")
        }

        DeviceKeyManager.deviceKey?.let {
            requestBuilder.addHeader("X-Device-Key", it)
        }

        val request = requestBuilder.build()
        return chain.proceed(request)
    }
}
