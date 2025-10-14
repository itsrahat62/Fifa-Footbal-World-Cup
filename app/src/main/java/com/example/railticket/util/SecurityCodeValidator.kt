package com.example.railticket.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SecurityCodeValidator {

    private const val SECRET_KEY = 112150L

    /**
     * Calculates the daily security code for a given mobile number.
     *
     * @param mobileNumber The full mobile number (e.g., "01767552562").
     * @return The calculated security code as a hexadecimal string (e.g., "D57A25").
     * Returns null if the mobile number is invalid.
     */
    fun generateCode(mobileNumber: String?): String? {
        if (mobileNumber == null || mobileNumber.length < 3) {
            return null
        }

        try {
            // 1. Get the current date in DDMMYYYY format
            val dateFormat = SimpleDateFormat("ddMMyyyy", Locale.getDefault())
            val dateString = dateFormat.format(Date())
            val dateAsNumber = dateString.toLong()

            // 2. Subtract the secret key
            val resultAfterSubtraction = dateAsNumber - SECRET_KEY

            // 3. Get the last three digits of the mobile number
            val mobileSuffix = mobileNumber.takeLast(3).toLong()

            // 4. Add the suffix to the result
            val finalNumber = resultAfterSubtraction + mobileSuffix

            // 5. Convert to hexadecimal and return as an uppercase string (corrected deprecation)
            return finalNumber.toString(16).uppercase(Locale.ROOT)
        } catch (e: Exception) {
            // In case of any parsing errors
            e.printStackTrace()
            return null
        }
    }
}
