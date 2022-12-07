package com.jiaoay.biometric;

import android.os.Build;
import android.os.CancellationSignal;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;

/**
 * Creates and caches cancellation signal objects that are compatible with
 * {@link android.hardware.biometrics.BiometricPrompt} or
 * {@link androidx.core.hardware.fingerprint.FingerprintManagerCompat}.
 */
@SuppressWarnings("deprecation")
class CancellationSignalProvider {
    private static final String TAG = "CancelSignalProvider";

    /**
     * An injector for various class dependencies. Used for testing.
     */
    @VisibleForTesting
    interface Injector {
        /**
         * Returns a cancellation signal object that is compatible with
         * {@link android.hardware.biometrics.BiometricPrompt}.
         *
         * @return An instance of {@link CancellationSignal}.
         */
        @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
        @NonNull
        CancellationSignal getBiometricCancellationSignal();

        /**
         * Returns a cancellation signal object that is compatible with
         * {@link androidx.core.hardware.fingerprint.FingerprintManagerCompat}.
         *
         * @return An instance of {@link androidx.core.os.CancellationSignal}.
         */
        @NonNull
        androidx.core.os.CancellationSignal getFingerprintCancellationSignal();
    }

    /**
     * The injector for class dependencies used by this provider.
     */
    private final Injector mInjector;

    /**
     * A cancellation signal object that is compatible with
     * {@link android.hardware.biometrics.BiometricPrompt}.
     */
    @Nullable private CancellationSignal mBiometricCancellationSignal;

    /**
     * A cancellation signal object that is compatible with
     * {@link androidx.core.hardware.fingerprint.FingerprintManagerCompat}.
     */
    @Nullable private androidx.core.os.CancellationSignal mFingerprintCancellationSignal;

    /**
     * Creates a new cancellation signal provider instance.
     */
    CancellationSignalProvider() {
        mInjector = new Injector() {
            @Override
            @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
            @NonNull
            public CancellationSignal getBiometricCancellationSignal() {
                return Api16Impl.create();
            }

            @Override
            @NonNull
            public androidx.core.os.CancellationSignal getFingerprintCancellationSignal() {
                return new androidx.core.os.CancellationSignal();
            }
        };
    }

    /**
     * Creates a new cancellation signal provider instance with the given injector.
     *
     * @param injector An injector for class and method dependencies.
     */
    @VisibleForTesting
    CancellationSignalProvider(Injector injector) {
        mInjector = injector;
    }

    /**
     * Provides a cancellation signal object that is compatible with
     * {@link android.hardware.biometrics.BiometricPrompt}.
     *
     * <p>Subsequent calls to this method for the same provider instance will return the same
     * cancellation signal, until {@link #cancel()} is invoked.
     *
     * @return A cancellation signal that can be passed to
     *  {@link android.hardware.biometrics.BiometricPrompt}.
     */
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    @NonNull
    CancellationSignal getBiometricCancellationSignal() {
        if (mBiometricCancellationSignal == null) {
            mBiometricCancellationSignal = mInjector.getBiometricCancellationSignal();
        }
        return mBiometricCancellationSignal;
    }

    /**
     * Provides a cancellation signal object that is compatible with
     * {@link androidx.core.hardware.fingerprint.FingerprintManagerCompat}.
     *
     * <p>Subsequent calls to this method for the same provider instance will return the same
     * cancellation signal, until {@link #cancel()} is invoked.
     *
     * @return A cancellation signal that can be passed to
     *  {@link androidx.core.hardware.fingerprint.FingerprintManagerCompat}.
     */
    @NonNull
    androidx.core.os.CancellationSignal getFingerprintCancellationSignal() {
        if (mFingerprintCancellationSignal == null) {
            mFingerprintCancellationSignal = mInjector.getFingerprintCancellationSignal();
        }
        return mFingerprintCancellationSignal;
    }

    /**
     * Invokes cancel for all cached cancellation signal objects and clears any references to them.
     */
    void cancel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && mBiometricCancellationSignal != null) {
            try {
                Api16Impl.cancel(mBiometricCancellationSignal);
            } catch (NullPointerException e) {
                // Catch and handle NPE if thrown by framework call to cancel() (b/151316421).
                Log.e(TAG, "Got NPE while canceling biometric authentication.", e);
            }
            mBiometricCancellationSignal = null;
        }
        if (mFingerprintCancellationSignal != null) {
            try {
                mFingerprintCancellationSignal.cancel();
            } catch (NullPointerException e) {
                // Catch and handle NPE if thrown by framework call to cancel() (b/151316421).
                Log.e(TAG, "Got NPE while canceling fingerprint authentication.", e);
            }
            mFingerprintCancellationSignal = null;
        }
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 4.1 (API 16).
     */
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    private static class Api16Impl {
        // Prevent instantiation.
        private Api16Impl() {}

        /**
         * Creates a new instance of the platform class {@link CancellationSignal}.
         *
         * @return An instance of {@link CancellationSignal}.
         */
        static CancellationSignal create() {
            return new CancellationSignal();
        }

        /**
         * Calls {@link CancellationSignal#cancel()} for the given cancellation signal.
         */
        static void cancel(CancellationSignal cancellationSignal) {
            cancellationSignal.cancel();
        }
    }
}
