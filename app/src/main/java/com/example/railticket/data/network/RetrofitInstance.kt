package com.example.railticket.data.network

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    private const val BASE_URL = "https://railspaapi.shohoz.com/" // Your base API URL

    // Create a logging interceptor (optional, but very useful for debugging)
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // Log request and response bodies
    }

    // Create an Auth interceptor
    private val authInterceptor = AuthInterceptor()

    // Create an OkHttpClient and add the interceptors
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS) // Optional: Set timeouts
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Configure Gson to serialize nulls and be lenient
    private val gson = GsonBuilder()
        .setLenient() // Example: if your API sometimes returns slightly malformed JSON
        .serializeNulls() // Add this to include null fields in the JSON output
        .create()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // Use the OkHttpClient with the interceptors
            .addConverterFactory(GsonConverterFactory.create(gson)) // Use the modified Gson instance
            .build()
    }

    val bookingService: BookingService by lazy {
        retrofit.create(BookingService::class.java)
    }
}
