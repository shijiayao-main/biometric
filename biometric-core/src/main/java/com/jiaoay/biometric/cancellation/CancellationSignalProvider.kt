package com.jiaoay.biometric.cancellation

import android.os.Build
import android.os.CancellationSignal
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting

/**
 * Creates and caches cancellation signal objects that are compatible with
 * [android.hardware.biometrics.BiometricPrompt] or
 * [androidx.core.hardware.fingerprint.FingerprintManagerCompat].
 */
class CancellationSignalProvider {

    /**
     * The injector for class dependencies used by this provider.
     */
    private val mCancellationInjector: CancellationInjector

    /**
     * A cancellation signal object that is compatible with
     * [android.hardware.biometrics.BiometricPrompt].
     */
    private var mBiometricCancellationSignal: CancellationSignal? = null

    /**
     * A cancellation signal object that is compatible with
     * [androidx.core.hardware.fingerprint.FingerprintManagerCompat].
     */
    private var mFingerprintCancellationSignal: androidx.core.os.CancellationSignal? = null

    /**
     * Creates a new cancellation signal provider instance.
     */
    constructor() {
        mCancellationInjector = object : CancellationInjector {
            @get:RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
            override val biometricCancellationSignal: CancellationSignal
                get() = CancellationApi16Impl.create()
            override val fingerprintCancellationSignal: androidx.core.os.CancellationSignal
                get() = androidx.core.os.CancellationSignal()
        }
    }

    /**
     * Creates a new cancellation signal provider instance with the given injector.
     *
     * @param cancellationInjector An injector for class and method dependencies.
     */
    @VisibleForTesting
    internal constructor(cancellationInjector: CancellationInjector) {
        mCancellationInjector = cancellationInjector
    }

    /**
     * Provides a cancellation signal object that is compatible with
     * [android.hardware.biometrics.BiometricPrompt].
     *
     *
     * Subsequent calls to this method for the same provider instance will return the same
     * cancellation signal, until [.cancel] is invoked.
     *
     * @return A cancellation signal that can be passed to
     * [android.hardware.biometrics.BiometricPrompt].
     */
    @get:RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    val biometricCancellationSignal: CancellationSignal
        get() {
            if (mBiometricCancellationSignal == null) {
                mBiometricCancellationSignal = mCancellationInjector.biometricCancellationSignal
            }
            return mBiometricCancellationSignal!!
        }

    /**
     * Provides a cancellation signal object that is compatible with
     * [androidx.core.hardware.fingerprint.FingerprintManagerCompat].
     *
     *
     * Subsequent calls to this method for the same provider instance will return the same
     * cancellation signal, until [.cancel] is invoked.
     *
     * @return A cancellation signal that can be passed to
     * [androidx.core.hardware.fingerprint.FingerprintManagerCompat].
     */
    val fingerprintCancellationSignal: androidx.core.os.CancellationSignal
        get() {
            if (mFingerprintCancellationSignal == null) {
                mFingerprintCancellationSignal = mCancellationInjector.fingerprintCancellationSignal
            }
            return mFingerprintCancellationSignal!!
        }

    /**
     * Invokes cancel for all cached cancellation signal objects and clears any references to them.
     */
    fun cancel() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN &&
            mBiometricCancellationSignal != null
        ) {
            try {
                CancellationApi16Impl.cancel(mBiometricCancellationSignal!!)
            } catch (e: NullPointerException) {
                // Catch and handle NPE if thrown by framework call to cancel() (b/151316421).
                Log.e(TAG, "Got NPE while canceling biometric authentication.", e)
            }
            mBiometricCancellationSignal = null
        }
        if (mFingerprintCancellationSignal != null) {
            try {
                mFingerprintCancellationSignal!!.cancel()
            } catch (e: NullPointerException) {
                // Catch and handle NPE if thrown by framework call to cancel() (b/151316421).
                Log.e(TAG, "Got NPE while canceling fingerprint authentication.", e)
            }
            mFingerprintCancellationSignal = null
        }
    }

    companion object {
        private const val TAG = "CancelSignalProvider"
    }
}