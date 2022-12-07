package com.jiaoay.biometric

import com.jiaoay.biometric.BiometricPrompt.AuthenticationError
import java.util.*

/**
 * A container for data associated with a biometric authentication error, which may be handled by
 * the client application's callback.
 */
class BiometricErrorData(
    /**
     * An integer ID associated with this error.
     */
    @field:AuthenticationError @get:AuthenticationError val errorCode: Int,
    /**
     * A human-readable message that describes the error.
     */
    val errorMessage: CharSequence?
) {

    override fun hashCode(): Int {
        return Arrays.hashCode(arrayOf<Any?>(errorCode, convertToString(errorMessage)))
    }

    override fun equals(obj: Any?): Boolean {
        if (obj is BiometricErrorData) {
            val other = obj
            return errorCode == other.errorCode && isErrorMessageEqualTo(other.errorMessage)
        }
        return false
    }

    /**
     * Checks if a given error message is equivalent to the one for this object.
     *
     * @param otherMessage A message to compare to the error message for this object.
     * @return Whether the error message for this object and `otherMessage` are equivalent.
     */
    private fun isErrorMessageEqualTo(otherMessage: CharSequence?): Boolean {
        val errorString = convertToString(errorMessage)
        val otherString = convertToString(otherMessage)
        return errorString == null && otherString == null || errorString != null && errorString == otherString
    }

    companion object {
        /**
         * Converts a nullable [CharSequence] message to an equivalent [String].
         *
         * @param message The message to be converted.
         * @return A string matching the given message, or `null` if message is `null`.
         */
        private fun convertToString(message: CharSequence?): String? {
            return message?.toString()
        }
    }
}