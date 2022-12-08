package com.jiaoay.biometric.authentication

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import com.jiaoay.biometric.util.BiometricPrompt
import com.jiaoay.biometric.crypto.CryptoObjectUtils

/**
 * Uses a common listener interface provided by the client to create and cache authentication
 * callback objects that are compatible with [android.hardware.biometrics.BiometricPrompt] or
 * [androidx.core.hardware.fingerprint.FingerprintManagerCompat].
 */
class AuthenticationCallbackProvider
/**
 * Constructs a callback provider that delegates events to the given listener.
 *
 * @param listener A listener that will receive authentication events.
 */(
    /**
     * A common listener object that will receive all authentication events.
     */
    val mListener: Listener
) {
    /**
     * A listener object that can receive events from either
     * [android.hardware.biometrics.BiometricPrompt.AuthenticationCallback] or
     * [androidx.core.hardware.fingerprint.FingerprintManagerCompat.AuthenticationCallback].
     */
    open class Listener {
        /**
         * See [AuthenticationCallback.onAuthenticationSucceeded].
         *
         * @param result An object containing authentication-related data.
         */
        open fun onSuccess(result: AuthenticationResult) {}

        /**
         * See [AuthenticationCallback.onAuthenticationError].
         *
         * @param errorCode    An integer ID associated with the error.
         * @param errorMessage A human-readable message that describes the error.
         */
        open fun onError(errorCode: Int, errorMessage: CharSequence?) {}

        /**
         * Called when a recoverable error/event has been encountered during authentication.
         *
         * @param helpMessage A human-readable message that describes the event.
         */
        open fun onHelp(helpMessage: CharSequence?) {}

        /**
         * See [AuthenticationCallback.onAuthenticationFailed].
         */
        open fun onFailure() {}
    }

    /**
     * An authentication callback object that is compatible with
     * [android.hardware.biometrics.BiometricPrompt].
     */
    private var mBiometricCallback: android.hardware.biometrics.BiometricPrompt.AuthenticationCallback? = null

    /**
     * An authentication callback object that is compatible with
     * [androidx.core.hardware.fingerprint.FingerprintManagerCompat].
     */
    private var mFingerprintCallback: FingerprintManagerCompat.AuthenticationCallback? = null

    /**
     * Provides a callback object that wraps the given listener and is compatible with
     * [android.hardware.biometrics.BiometricPrompt].
     *
     *
     * Subsequent calls to this method for the same provider instance will return the same
     * callback object.
     *
     * @return A callback object that can be passed to
     * [android.hardware.biometrics.BiometricPrompt].
     */
    @get:RequiresApi(Build.VERSION_CODES.P)
    val biometricCallback: android.hardware.biometrics.BiometricPrompt.AuthenticationCallback
        get() {
            if (mBiometricCallback == null) {
                mBiometricCallback = AuthenticationCallbackApi28Impl.createCallback(mListener)
            }
            return mBiometricCallback!!
        }

    /**
     * Provides a callback object that wraps the given listener and is compatible with
     * [androidx.core.hardware.fingerprint.FingerprintManagerCompat].
     *
     *
     * Subsequent calls to this method for the same provider instance will return the same
     * callback object.
     *
     * @return A callback object that can be passed to
     * [androidx.core.hardware.fingerprint.FingerprintManagerCompat].
     */
    val fingerprintCallback: FingerprintManagerCompat.AuthenticationCallback
        get() {
            if (mFingerprintCallback == null) {
                mFingerprintCallback = object : FingerprintManagerCompat.AuthenticationCallback() {
                    override fun onAuthenticationError(errMsgId: Int, errString: CharSequence) {
                        mListener.onError(errMsgId, errString)
                    }

                    override fun onAuthenticationHelp(helpMsgId: Int, helpString: CharSequence) {
                        mListener.onHelp(helpString)
                    }

                    override fun onAuthenticationSucceeded(result: FingerprintManagerCompat.AuthenticationResult) {
                        val crypto = CryptoObjectUtils.unwrapFromFingerprintManager(
                            result.cryptoObject
                        )
                        val resultCompat = AuthenticationResult(
                            crypto, BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC
                        )
                        mListener.onSuccess(resultCompat)
                    }

                    override fun onAuthenticationFailed() {
                        mListener.onFailure()
                    }
                }
            }
            return mFingerprintCallback!!
        }
}