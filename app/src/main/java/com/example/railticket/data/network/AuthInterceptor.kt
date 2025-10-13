package com.example.railticket.data.network

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

        requestBuilder.addHeader("X-Device-Key", "114e6a31e406bf79f2efa4ea722293f7a477112801cf30f7422790a7733d4871e4ddbe7aaabb40565b05f9351eb158cfbc812ab918bec911e21874da8742bf23a27ee880db53705e34b09440b0dcb4e6")

        val request = requestBuilder.build()
        return chain.proceed(request)
    }
}
