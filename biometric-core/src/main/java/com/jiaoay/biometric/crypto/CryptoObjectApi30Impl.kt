package com.jiaoay.biometric.crypto

import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.security.identity.IdentityCredential
import androidx.annotation.RequiresApi

/**
 * Nested class to avoid verification errors for methods introduced in Android 9.0 (API 28).
 */
@RequiresApi(Build.VERSION_CODES.R)
object CryptoObjectApi30Impl {
    /**
     * Creates an instance of the framework class
     * [android.hardware.biometrics.BiometricPrompt.CryptoObject] from the given identity
     * credential.
     *
     * @param identityCredential The identity credential object to be wrapped.
     * @return An instance of [android.hardware.biometrics.BiometricPrompt.CryptoObject].
     */
    fun create(
        identityCredential: IdentityCredential
    ): BiometricPrompt.CryptoObject {
        return BiometricPrompt.CryptoObject(identityCredential)
    }

    /**
     * Gets the identity credential associated with the given crypto object, if any.
     *
     * @param crypto An instance of
     * [android.hardware.biometrics.BiometricPrompt.CryptoObject].
     * @return The wrapped identity credential object, or `null`.
     */
    fun getIdentityCredential(
        crypto: BiometricPrompt.CryptoObject
    ): IdentityCredential? {
        return crypto.identityCredential
    }
}