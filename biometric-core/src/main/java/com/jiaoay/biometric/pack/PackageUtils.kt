package com.jiaoay.biometric.pack

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Utilities related to the system [PackageManager].
 */
object PackageUtils {
    /**
     * Checks if the current device supports fingerprint authentication.
     *
     * @param context The application or activity context.
     * @return Whether fingerprint is supported.
     */
    @JvmStatic
    fun hasSystemFeatureFingerprint(context: Context?): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                context != null &&
                context.packageManager != null &&
                PackageApi23Impl.hasSystemFeatureFingerprint(context.packageManager)
    }
}