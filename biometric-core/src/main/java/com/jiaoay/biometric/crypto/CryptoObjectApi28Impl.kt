package com.jiaoay.biometric.crypto

import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import androidx.annotation.RequiresApi
import java.security.Signature
import javax.crypto.Cipher
import javax.crypto.Mac

/**
 * Nested class to avoid verification errors for methods introduced in Android 9.0 (API 28).
 */
@RequiresApi(Build.VERSION_CODES.P)
object CryptoObjectApi28Impl {
    /**
     * Creates an instance of the framework class
     * [android.hardware.biometrics.BiometricPrompt.CryptoObject] from the given cipher.
     *
     * @param cipher The cipher object to be wrapped.
     * @return An instance of [android.hardware.biometrics.BiometricPrompt.CryptoObject].
     */
    fun create(
        cipher: Cipher
    ): BiometricPrompt.CryptoObject {
        return BiometricPrompt.CryptoObject(cipher)
    }

    /**
     * Creates an instance of the framework class
     * [android.hardware.biometrics.BiometricPrompt.CryptoObject] from the given
     * signature.
     *
     * @param signature The signature object to be wrapped.
     * @return An instance of [android.hardware.biometrics.BiometricPrompt.CryptoObject].
     */
    fun create(
        signature: Signature
    ): BiometricPrompt.CryptoObject {
        return BiometricPrompt.CryptoObject(signature)
    }

    /**
     * Creates an instance of the framework class
     * [android.hardware.biometrics.BiometricPrompt.CryptoObject] from the given MAC.
     *
     * @param mac The MAC object to be wrapped.
     * @return An instance of [android.hardware.biometrics.BiometricPrompt.CryptoObject].
     */
    fun create(mac: Mac): BiometricPrompt.CryptoObject {
        return BiometricPrompt.CryptoObject(mac)
    }

    /**
     * Gets the cipher associated with the given crypto object, if any.
     *
     * @param crypto An instance of
     * [android.hardware.biometrics.BiometricPrompt.CryptoObject].
     * @return The wrapped cipher object, or `null`.
     */
    fun getCipher(
        crypto: BiometricPrompt.CryptoObject
    ): Cipher? {
        return crypto.cipher
    }

    /**
     * Gets the signature associated with the given crypto object, if any.
     *
     * @param crypto An instance of
     * [android.hardware.biometrics.BiometricPrompt.CryptoObject].
     * @return The wrapped signature object, or `null`.
     */
    fun getSignature(
        crypto: BiometricPrompt.CryptoObject
    ): Signature? {
        return crypto.signature
    }

    /**
     * Gets the MAC associated with the given crypto object, if any.
     *
     * @param crypto An instance of
     * [android.hardware.biometrics.BiometricPrompt.CryptoObject].
     * @return The wrapped MAC object, or `null`.
     */
    fun getMac(
        crypto: BiometricPrompt.CryptoObject
    ): Mac? {
        return crypto.mac
    }
}