package com.jiaoay.biometric.authentication

import com.jiaoay.biometric.util.BiometricPrompt.AuthenticationResultType
import com.jiaoay.biometric.crypto.CryptoObject

/**
 * A container for data passed to {@link AuthenticationCallback#onAuthenticationSucceeded(
 *AuthenticationResult)} when the user has successfully authenticated.
 */
class AuthenticationResult(
    crypto: CryptoObject?,
    @AuthenticationResultType authenticationType: Int
) {
    private var mCryptoObject: CryptoObject? = crypto

    @AuthenticationResultType
    private var mAuthenticationType = authenticationType

    /**
     * Gets the [CryptoObject] associated with this transaction.
     *
     * @return The [CryptoObject] provided to `authenticate()`.
     */
    fun getCryptoObject(): CryptoObject? {
        return mCryptoObject
    }

    /**
     * Gets the type of authentication (e.g. device credential or biometric) that was
     * requested from and successfully provided by the user.
     *
     * @return An integer representing the type of authentication that was used.
     * @see .AUTHENTICATION_RESULT_TYPE_UNKNOWN
     *
     * @see .AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL
     *
     * @see .AUTHENTICATION_RESULT_TYPE_BIOMETRIC
     */
    @AuthenticationResultType
    fun getAuthenticationType(): Int {
        return mAuthenticationType
    }
}