package com.example.railticket.data

/**
 * A generic class that holds a value with its loading status.
 * @param <T>
 */
sealed class Result<out T : Any> {

    data class Success<out T : Any>(val data: T, val message: String? = null) : Result<T>()
    data class Error(
        val exception: Exception,
        val message: String? = null,
        val errorBody: String? = null // For holding raw error response
    ) : Result<Nothing>()

    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data=$data, message=$message]"
            is Error -> "Error[exception=$exception, message=$message, errorBody=$errorBody]"
        }
    }
}
