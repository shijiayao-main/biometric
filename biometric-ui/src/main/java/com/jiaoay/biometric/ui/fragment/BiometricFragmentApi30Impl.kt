package com.jiaoay.biometric.ui.fragment

import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import androidx.annotation.RequiresApi
import com.jiaoay.biometric.manager.AuthenticatorTypes


/**
 * Nested class to avoid verification errors for methods introduced in Android 11 (API 30).
 */
@RequiresApi(Build.VERSION_CODES.R)
object BiometricFragmentApi30Impl {

    /**
     * Sets the allowed authenticator type(s) for the given framework prompt builder.
     *
     * @param builder               An instance of
     * [android.hardware.biometrics.BiometricPrompt.Builder].
     * @param allowedAuthenticators A bit field representing allowed authenticator types.
     */
    @JvmStatic
    fun setAllowedAuthenticators(
        builder: BiometricPrompt.Builder,
        @AuthenticatorTypes allowedAuthenticators: Int
    ) {
        builder.setAllowedAuthenticators(allowedAuthenticators)
    }
}