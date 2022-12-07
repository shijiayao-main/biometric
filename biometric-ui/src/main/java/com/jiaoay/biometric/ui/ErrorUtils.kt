package com.jiaoay.biometric.ui

import android.content.Context
import android.util.Log
import com.jiaoay.biometric.BiometricPrompt
import com.jiaoay.biometric_ui.R

/**
 * Utilities related to biometric authentication errors.
 */
object ErrorUtils {
    /**
     * Checks if the given error code matches any known (i.e. publicly defined) error.
     *
     * @param errorCode An integer ID associated with the error.
     * @return Whether the error code matches a known error.
     */
    @JvmStatic
    fun isKnownError(errorCode: Int): Boolean {
        return when (errorCode) {
            BiometricPrompt.ERROR_HW_UNAVAILABLE, BiometricPrompt.ERROR_UNABLE_TO_PROCESS, BiometricPrompt.ERROR_TIMEOUT, BiometricPrompt.ERROR_NO_SPACE, BiometricPrompt.ERROR_CANCELED, BiometricPrompt.ERROR_LOCKOUT, BiometricPrompt.ERROR_VENDOR, BiometricPrompt.ERROR_LOCKOUT_PERMANENT, BiometricPrompt.ERROR_USER_CANCELED, BiometricPrompt.ERROR_NO_BIOMETRICS, BiometricPrompt.ERROR_HW_NOT_PRESENT, BiometricPrompt.ERROR_NEGATIVE_BUTTON, BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL, BiometricPrompt.ERROR_SECURITY_UPDATE_REQUIRED -> true
            else -> false
        }
    }

    /**
     * Checks if the given error code indicates that the user has been (temporarily or permanently)
     * locked out from using biometric authentication, likely due to too many attempts.
     *
     * @param errorCode An integer ID associated with the error.
     * @return Whether the error code indicates that the user has been locked out.
     */
    @JvmStatic
    fun isLockoutError(errorCode: Int): Boolean {
        return (errorCode == BiometricPrompt.ERROR_LOCKOUT
                || errorCode == BiometricPrompt.ERROR_LOCKOUT_PERMANENT)
    }

    /**
     * Only needs to provide a subset of the fingerprint error strings since the rest are translated
     * in FingerprintManager
     */
    @JvmStatic
    fun getFingerprintErrorString(context: Context?, errorCode: Int): String {
        return if (context == null) {
            ""
        } else when (errorCode) {
            BiometricPrompt.ERROR_HW_NOT_PRESENT -> context.getString(R.string.fingerprint_error_hw_not_present)
            BiometricPrompt.ERROR_HW_UNAVAILABLE -> context.getString(R.string.fingerprint_error_hw_not_available)
            BiometricPrompt.ERROR_NO_BIOMETRICS -> context.getString(R.string.fingerprint_error_no_fingerprints)
            BiometricPrompt.ERROR_USER_CANCELED -> context.getString(R.string.fingerprint_error_user_canceled)
            BiometricPrompt.ERROR_LOCKOUT, BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> context.getString(R.string.fingerprint_error_lockout)
            else -> {
                Log.e("BiometricUtils", "Unknown error code: $errorCode")
                context.getString(R.string.default_error_msg)
            }
        }
    }
}