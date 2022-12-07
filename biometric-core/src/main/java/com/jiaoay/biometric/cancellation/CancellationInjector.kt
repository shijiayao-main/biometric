package com.jiaoay.biometric.cancellation

import android.os.Build
import android.os.CancellationSignal
import androidx.annotation.RequiresApi

/**
 * An injector for various class dependencies. Used for testing.
 */
interface CancellationInjector {
    /**
     * Returns a cancellation signal object that is compatible with
     * [android.hardware.biometrics.BiometricPrompt].
     *
     * @return An instance of [CancellationSignal].
     */
    @get:RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    val biometricCancellationSignal: CancellationSignal

    /**
     * Returns a cancellation signal object that is compatible with
     * [androidx.core.hardware.fingerprint.FingerprintManagerCompat].
     *
     * @return An instance of [androidx.core.os.CancellationSignal].
     */
    val fingerprintCancellationSignal: androidx.core.os.CancellationSignal
}