package com.jiaoay.biometric.authentication

import android.os.Build
import androidx.annotation.RequiresApi
import com.jiaoay.biometric.BiometricPrompt
import com.jiaoay.biometric.BiometricPrompt.AuthenticationResultType
import com.jiaoay.biometric.crypto.CryptoObjectUtils

/**
 * Nested class to avoid verification errors for methods introduced in Android 9.0 (API 28).
 */
@RequiresApi(Build.VERSION_CODES.P)
object AuthenticationCallbackApi28Impl {
    /**
     * Creates a [android.hardware.biometrics.BiometricPrompt.AuthenticationCallback] that
     * delegates events to the given listener.
     *
     * @param listener A listener object that will receive authentication events.
     * @return A new instance of
     * [android.hardware.biometrics.BiometricPrompt.AuthenticationCallback].
     */
    @JvmStatic
    fun createCallback(
        listener: AuthenticationCallbackProvider.Listener
    ): android.hardware.biometrics.BiometricPrompt.AuthenticationCallback {
        return object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                listener.onError(errorCode, errString)
            }

            override fun onAuthenticationHelp(
                helpCode: Int, helpString: CharSequence
            ) {
                // Don't forward the result to the client, since the dialog takes care of it.
            }

            override fun onAuthenticationSucceeded(
                result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult
            ) {
                val crypto = if (result != null) CryptoObjectUtils.unwrapFromBiometricPrompt(result.cryptoObject) else null
                @AuthenticationResultType val authenticationType: Int
                authenticationType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (result != null) AuthenticationCallbackApi30Impl.getAuthenticationType(result) else BiometricPrompt.AUTHENTICATION_RESULT_TYPE_UNKNOWN
                } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                    BiometricPrompt.AUTHENTICATION_RESULT_TYPE_UNKNOWN
                } else {
                    BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC
                }
                val resultCompat = AuthenticationResult(crypto, authenticationType)
                listener.onSuccess(resultCompat)
            }

            override fun onAuthenticationFailed() {
                listener.onFailure()
            }
        }
    }
}