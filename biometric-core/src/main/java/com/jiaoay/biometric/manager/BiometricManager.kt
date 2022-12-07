package com.jiaoay.biometric.manager

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import com.jiaoay.biometric.AuthenticatorUtils
import com.jiaoay.biometric.crypto.CryptoObjectUtils
import com.jiaoay.biometric.manager.BiometricManagerApi29Impl.canAuthenticate
import com.jiaoay.biometric.manager.BiometricManagerApi29Impl.getCanAuthenticateWithCryptoMethod
import com.jiaoay.biometric.manager.BiometricManagerApi30Impl.canAuthenticate
import java.lang.reflect.InvocationTargetException

/**
 * A class that provides system information related to biometrics (e.g. fingerprint, face, etc.).
 *
 *
 * On devices running Android 10 (API 29) and above, this will query the framework's version of
 * [android.hardware.biometrics.BiometricManager]. On Android 9.0 (API 28) and prior
 * versions, this will query [androidx.core.hardware.fingerprint.FingerprintManagerCompat].
 *
 * @see BiometricPrompt To prompt the user to authenticate with their biometric.
 */
class BiometricManager @VisibleForTesting internal constructor(
    /**
     * The injector for class and method dependencies used by this manager.
     */
    private val mBiometricManagerInjector: BiometricManagerInjector
) {

    /**
     * The framework biometric manager. Should be non-null on Android 10 (API 29) and above.
     */
    private val mBiometricManager: android.hardware.biometrics.BiometricManager?

    /**
     * The framework fingerprint manager. Should be non-null on Android 10 (API 29) and below.
     */
    private val mFingerprintManager: FingerprintManagerCompat?

    /**
     * Creates a [BiometricManager] instance with the given injector.
     *
     * @param injector An injector for class and method dependencies.
     */
    init {
        mBiometricManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) mBiometricManagerInjector.biometricManager else null
        mFingerprintManager = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) mBiometricManagerInjector.fingerprintManager else null
    }

    /**
     * Checks if the user can authenticate with biometrics. This requires at least one biometric
     * sensor to be present, enrolled, and available on the device.
     *
     * @return [.BIOMETRIC_SUCCESS] if the user can authenticate with biometrics. Otherwise,
     * returns an error code indicating why the user can't authenticate, or
     * [.BIOMETRIC_STATUS_UNKNOWN] if it is unknown whether the user can authenticate.
     *
     */
    @AuthenticationStatus
    @Deprecated("Use {@link #canAuthenticate(int)} instead.")
    fun canAuthenticate(): Int {
        return canAuthenticate(Authenticators.BIOMETRIC_WEAK)
    }

    /**
     * Checks if the user can authenticate with an authenticator that meets the given requirements.
     * This requires at least one of the specified authenticators to be present, enrolled, and
     * available on the device.
     *
     *
     * Note that not all combinations of authenticator types are supported prior to Android 11
     * (API 30). Specifically, `DEVICE_CREDENTIAL` alone is unsupported prior to API 30, and
     * `BIOMETRIC_STRONG | DEVICE_CREDENTIAL` is unsupported on API 28-29. Developers that
     * wish to check for the presence of a PIN, pattern, or password on these versions should
     * instead use [KeyguardManager.isDeviceSecure].
     *
     * @param authenticators A bit field representing the types of [Authenticators] that may
     * be used for authentication.
     * @return [.BIOMETRIC_SUCCESS] if the user can authenticate with an allowed
     * authenticator. Otherwise, returns [.BIOMETRIC_STATUS_UNKNOWN] or an error code
     * indicating why the user can't authenticate.
     */
    @AuthenticationStatus
    fun canAuthenticate(@AuthenticatorTypes authenticators: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (mBiometricManager == null) {
                Log.e(TAG, "Failure in canAuthenticate(). BiometricManager was null.")
                return BIOMETRIC_ERROR_HW_UNAVAILABLE
            }
            return canAuthenticate(mBiometricManager, authenticators)
        }
        return canAuthenticateCompat(authenticators)
    }

    /**
     * Checks if the user can authenticate with an authenticator that meets the given requirements.
     *
     *
     * This method attempts to emulate the behavior of [.canAuthenticate] on devices
     * running Android 10 (API 29) and below.
     *
     * @param authenticators A bit field representing the types of [Authenticators] that may
     * be used for authentication.
     * @return [.BIOMETRIC_SUCCESS] if the user can authenticate with the given set of allowed
     * authenticators. Otherwise, returns an error code indicating why the user can't authenticate,
     * or [.BIOMETRIC_STATUS_UNKNOWN] if it is unknown whether the user can authenticate.
     */
    @AuthenticationStatus
    private fun canAuthenticateCompat(@AuthenticatorTypes authenticators: Int): Int {
        if (!AuthenticatorUtils.isSupportedCombination(authenticators)) {
            return BIOMETRIC_ERROR_UNSUPPORTED
        }

        // Match the framework's behavior for an empty authenticator set on API 30.
        if (authenticators == 0) {
            return BIOMETRIC_ERROR_NO_HARDWARE
        }

        // Authentication is impossible if the device can't be secured.
        if (!mBiometricManagerInjector.isDeviceSecurable) {
            return BIOMETRIC_ERROR_NO_HARDWARE
        }

        // Credential authentication is always possible if the device is secured. Conversely, no
        // form of authentication is possible if the device is not secured.
        if (AuthenticatorUtils.isDeviceCredentialAllowed(authenticators)) {
            return if (mBiometricManagerInjector.isDeviceSecuredWithCredential) BIOMETRIC_SUCCESS else BIOMETRIC_ERROR_NONE_ENROLLED
        }

        // The class of some non-fingerprint biometrics can be checked on API 29.
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            return if (AuthenticatorUtils.isWeakBiometricAllowed(authenticators)) canAuthenticateWithWeakBiometricOnApi29() else canAuthenticateWithStrongBiometricOnApi29()
        }

        // Non-fingerprint biometrics may be invoked but can't be checked on API 28.
        return if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
            // Having fingerprint hardware is a prerequisite, since BiometricPrompt internally
            // calls FingerprintManager#getErrorString() on API 28 (b/151443237).
            if (mBiometricManagerInjector.isFingerprintHardwarePresent) canAuthenticateWithFingerprintOrUnknownBiometric() else BIOMETRIC_ERROR_NO_HARDWARE
        } else canAuthenticateWithFingerprint()

        // No non-fingerprint biometric APIs exist prior to API 28.
    }

    /**
     * Checks if the user can authenticate with a **Class 3** (formerly
     * **Strong**) or better biometric sensor on a device running Android 10 (API 29).
     *
     * @return [.BIOMETRIC_SUCCESS] if the user can authenticate with a
     * **Class 3** or better biometric sensor. Otherwise, returns an error code
     * indicating why the user can't authenticate, or [.BIOMETRIC_STATUS_UNKNOWN] if it is
     * unknown whether the user can authenticate.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    @AuthenticationStatus
    private fun canAuthenticateWithStrongBiometricOnApi29(): Int {
        // Use the hidden canAuthenticate(CryptoObject) method if it exists.
        val canAuthenticateWithCrypto = getCanAuthenticateWithCryptoMethod()
        if (canAuthenticateWithCrypto != null) {
            val crypto = CryptoObjectUtils.wrapForBiometricPrompt(
                CryptoObjectUtils.createFakeCryptoObject()
            )
            if (crypto != null) {
                try {
                    val result = canAuthenticateWithCrypto.invoke(mBiometricManager, crypto)
                    if (result is Int) {
                        return result
                    }
                    Log.w(TAG, "Invalid return type for canAuthenticate(CryptoObject).")
                } catch (e: IllegalAccessException) {
                    Log.w(TAG, "Failed to invoke canAuthenticate(CryptoObject).", e)
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "Failed to invoke canAuthenticate(CryptoObject).", e)
                } catch (e: InvocationTargetException) {
                    Log.w(TAG, "Failed to invoke canAuthenticate(CryptoObject).", e)
                }
            }
        }

        // Check if we can use canAuthenticate() as a proxy for canAuthenticate(BIOMETRIC_STRONG).
        @AuthenticationStatus val result = canAuthenticateWithWeakBiometricOnApi29()
        return if (mBiometricManagerInjector.isStrongBiometricGuaranteed || result != BIOMETRIC_SUCCESS) {
            result
        } else canAuthenticateWithFingerprintOrUnknownBiometric()

        // If all else fails, check if fingerprint authentication is available.
    }

    /**
     * Checks if the user can authenticate with a **Class 2** (formerly
     * **Weak**) or better biometric sensor on a device running Android 10 (API 29).
     *
     * @return [.BIOMETRIC_SUCCESS] if the user can authenticate with a
     * **Class 2** or better biometric sensor. Otherwise, returns an error code
     * indicating why the user can't authenticate.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    @AuthenticationStatus
    private fun canAuthenticateWithWeakBiometricOnApi29(): Int {
        if (mBiometricManager == null) {
            Log.e(TAG, "Failure in canAuthenticate(). BiometricManager was null.")
            return BIOMETRIC_ERROR_HW_UNAVAILABLE
        }
        return canAuthenticate(mBiometricManager)
    }

    /**
     * Checks if the user can authenticate with fingerprint or with a biometric sensor for which
     * there is no platform method to check availability.
     *
     * @return [.BIOMETRIC_SUCCESS] if the user can authenticate with fingerprint. Otherwise,
     * returns an error code indicating why the user can't authenticate, or
     * [.BIOMETRIC_STATUS_UNKNOWN] if it is unknown whether the user can authenticate.
     */
    @AuthenticationStatus
    private fun canAuthenticateWithFingerprintOrUnknownBiometric(): Int {
        // If the device is not secured, authentication is definitely not possible. Use
        // FingerprintManager to distinguish between the "no hardware" and "none enrolled" cases.
        if (!mBiometricManagerInjector.isDeviceSecuredWithCredential) {
            return canAuthenticateWithFingerprint()
        }

        // Check for definite availability of fingerprint. Otherwise, return "unknown" to allow for
        // non-fingerprint biometrics (e.g. iris) that may be available via BiometricPrompt.
        return if (canAuthenticateWithFingerprint() == BIOMETRIC_SUCCESS) BIOMETRIC_SUCCESS else BIOMETRIC_STATUS_UNKNOWN
    }

    /**
     * Checks if the user can authenticate with fingerprint.
     *
     * @return [.BIOMETRIC_SUCCESS] if the user can authenticate with fingerprint.
     * Otherwise, returns an error code indicating why the user can't authenticate.
     */
    @AuthenticationStatus
    private fun canAuthenticateWithFingerprint(): Int {
        if (mFingerprintManager == null) {
            Log.e(TAG, "Failure in canAuthenticate(). FingerprintManager was null.")
            return BIOMETRIC_ERROR_HW_UNAVAILABLE
        }
        if (!mFingerprintManager.isHardwareDetected) {
            return BIOMETRIC_ERROR_NO_HARDWARE
        }
        return if (!mFingerprintManager.hasEnrolledFingerprints()) {
            BIOMETRIC_ERROR_NONE_ENROLLED
        } else BIOMETRIC_SUCCESS
    }

    companion object {
        private const val TAG = "BiometricManager"

        /**
         * The user can successfully authenticate.
         */
        const val BIOMETRIC_SUCCESS = 0

        /**
         * Unable to determine whether the user can authenticate.
         *
         *
         * This status code may be returned on older Android versions due to partial incompatibility
         * with a newer API. Applications that wish to enable biometric authentication on affected
         * devices may still call `BiometricPrompt#authenticate()` after receiving this status
         * code but should be prepared to handle possible errors.
         */
        const val BIOMETRIC_STATUS_UNKNOWN = -1

        /**
         * The user can't authenticate because the specified options are incompatible with the current
         * Android version.
         */
        const val BIOMETRIC_ERROR_UNSUPPORTED = -2

        /**
         * The user can't authenticate because the hardware is unavailable. Try again later.
         */
        const val BIOMETRIC_ERROR_HW_UNAVAILABLE = 1

        /**
         * The user can't authenticate because no biometric or device credential is enrolled.
         */
        const val BIOMETRIC_ERROR_NONE_ENROLLED = 11

        /**
         * The user can't authenticate because there is no suitable hardware (e.g. no biometric sensor
         * or no keyguard).
         */
        const val BIOMETRIC_ERROR_NO_HARDWARE = 12

        /**
         * The user can't authenticate because a security vulnerability has been discovered with one or
         * more hardware sensors. The affected sensor(s) are unavailable until a security update has
         * addressed the issue.
         */
        const val BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED = 15

        /**
         * Creates a [BiometricManager] instance from the given context.
         *
         * @param context The application or activity context.
         * @return An instance of [BiometricManager].
         */
        @JvmStatic
        fun from(context: Context): BiometricManager {
            return BiometricManager(DefaultBiometricManagerInjector(context))
        }
    }
}