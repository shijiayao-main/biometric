package com.jiaoay.biometric.manager

import android.hardware.biometrics.BiometricManager
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Nested class to avoid verification errors for methods introduced in Android 11 (API 30).
 */
@RequiresApi(Build.VERSION_CODES.R)
object BiometricManagerApi30Impl {
    /**
     * Calls [android.hardware.biometrics.BiometricManager.canAuthenticate] for the
     * given biometric manager and set of allowed authenticators.
     *
     * @param biometricManager An instance of
     * [android.hardware.biometrics.BiometricManager].
     * @param authenticators   A bit field representing the types of [Authenticators] that
     * may be used for authentication.
     * @return The result of
     * [android.hardware.biometrics.BiometricManager.canAuthenticate].
     */
    @JvmStatic
    @AuthenticationStatus
    fun canAuthenticate(
        biometricManager: BiometricManager,
        @AuthenticatorTypes authenticators: Int
    ): Int {
        return biometricManager.canAuthenticate(authenticators)
    }
}