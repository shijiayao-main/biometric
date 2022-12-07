package com.jiaoay.biometric.authentication

import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import androidx.annotation.RequiresApi
import com.jiaoay.biometric.BiometricPrompt.AuthenticationResultType

/**
 * Nested class to avoid verification errors for methods introduced in Android 11 (API 30).
 */
@RequiresApi(Build.VERSION_CODES.R)
object AuthenticationCallbackApi30Impl {
    /**
     * Gets the authentication type from the given framework authentication result.
     *
     * @param result An instance of
     * [android.hardware.biometrics.BiometricPrompt.AuthenticationResult].
     * @return The value returned by calling [ ][android.hardware.biometrics.BiometricPrompt.AuthenticationResult.getAuthenticationType]
     * for the given result object.
     */
    @AuthenticationResultType
    fun getAuthenticationType(
        result: BiometricPrompt.AuthenticationResult
    ): Int {
        return result.authenticationType
    }
}