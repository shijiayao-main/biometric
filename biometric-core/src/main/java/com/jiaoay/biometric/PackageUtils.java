package com.jiaoay.biometric;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * Utilities related to the system {@link PackageManager}.
 */
public class PackageUtils {
    // Prevent instantiation.
    private PackageUtils() {}

    /**
     * Checks if the current device supports fingerprint authentication.
     *
     * @param context The application or activity context.
     * @return Whether fingerprint is supported.
     */
    public static boolean hasSystemFeatureFingerprint(@Nullable Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && context != null
                && context.getPackageManager() != null
                && Api23Impl.hasSystemFeatureFingerprint(context.getPackageManager());
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 6.0 (API 23).
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private static class Api23Impl {
        // Prevent instantiation.
        private Api23Impl() {}

        /**
         * Checks if the given package manager has support for the fingerprint system feature.
         *
         * @param packageManager The system package manager.
         * @return Whether fingerprint is supported.
         */
        static boolean hasSystemFeatureFingerprint(@NonNull PackageManager packageManager) {
            return packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT);
        }
    }
}
