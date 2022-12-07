package com.jiaoay.biometric.manager

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.hardware.fingerprint.FingerprintManagerCompat


/**
 * An injector for various class and method dependencies. Used for testing.
 */
interface BiometricManagerInjector {
    /**
     * Provides the framework biometric manager that may be used on Android 10 (API 29) and
     * above.
     *
     * @return An instance of [android.hardware.biometrics.BiometricManager].
     */
    @get:RequiresApi(Build.VERSION_CODES.Q)
    val biometricManager: android.hardware.biometrics.BiometricManager?

    /**
     * Provides the fingerprint manager that may be used on Android 9.0 (API 28) and below.
     *
     * @return An instance of
     * [androidx.core.hardware.fingerprint.FingerprintManagerCompat].
     */
    val fingerprintManager: FingerprintManagerCompat?

    /**
     * Checks if the current device is capable of being secured with a lock screen credential
     * (i.e. PIN, pattern, or password).
     */
    val isDeviceSecurable: Boolean

    /**
     * Checks if the current device is secured with a lock screen credential (i.e. PIN, pattern,
     * or password).
     *
     * @return Whether the device is secured with a lock screen credential.
     */
    val isDeviceSecuredWithCredential: Boolean

    /**
     * Checks if the current device has a hardware sensor that may be used for fingerprint
     * authentication.
     *
     * @return Whether the device has a fingerprint sensor.
     */
    val isFingerprintHardwarePresent: Boolean

    /**
     * Checks if all biometric sensors on the device are known to meet or exceed the security
     * requirements for **Class 3** (formerly **Strong**).
     *
     * @return Whether all biometrics are known to be **Class 3** or stronger.
     */
    val isStrongBiometricGuaranteed: Boolean
}