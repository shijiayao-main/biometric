package com.jiaoay.biometric.ui.fragment

import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import androidx.annotation.RequiresApi


/**
 * Nested class to avoid verification errors for methods introduced in Android 10 (API 29).
 */
@RequiresApi(Build.VERSION_CODES.Q)
object BiometricFragmentApi29Impl {
    /**
     * Sets the "confirmation required" option for the given framework prompt builder.
     *
     * @param builder              An instance of
     * [android.hardware.biometrics.BiometricPrompt.Builder].
     * @param confirmationRequired The value for the "confirmation required" option.
     */
    @JvmStatic
    fun setConfirmationRequired(
        builder: BiometricPrompt.Builder,
        confirmationRequired: Boolean
    ) {
        builder.setConfirmationRequired(confirmationRequired)
    }

    /**
     * Sets the "device credential allowed" option for the given framework prompt builder.
     *
     * @param builder                 An instance of [                                ].
     * @param deviceCredentialAllowed The value for the "device credential allowed" option.
     */
    @JvmStatic
    fun setDeviceCredentialAllowed(
        builder: BiometricPrompt.Builder,
        deviceCredentialAllowed: Boolean
    ) {
        builder.setDeviceCredentialAllowed(deviceCredentialAllowed)
    }
}