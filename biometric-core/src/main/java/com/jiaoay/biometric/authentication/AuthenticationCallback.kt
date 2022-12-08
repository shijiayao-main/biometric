package com.jiaoay.biometric.authentication

import com.jiaoay.biometric.util.BiometricPrompt.AuthenticationError

/**
 * A collection of methods that may be invoked by {@link BiometricPrompt} during authentication.
 */
abstract class AuthenticationCallback {
    /**
     * Called when an unrecoverable error has been encountered and authentication has stopped.
     *
     *
     * After this method is called, no further events will be sent for the current
     * authentication session.
     *
     * @param errorCode An integer ID associated with the error.
     * @param errString A human-readable string that describes the error.
     */
    open fun onAuthenticationError(
        @AuthenticationError errorCode: Int, errString: CharSequence
    ) {
    }

    /**
     * Called when a biometric (e.g. fingerprint, face, etc.) is recognized, indicating that the
     * user has successfully authenticated.
     *
     *
     * After this method is called, no further events will be sent for the current
     * authentication session.
     *
     * @param result An object containing authentication-related data.
     */
    open fun onAuthenticationSucceeded(result: AuthenticationResult) {}

    /**
     * Called when a biometric (e.g. fingerprint, face, etc.) is presented but not recognized as
     * belonging to the user.
     */
    open fun onAuthenticationFailed() {}
}