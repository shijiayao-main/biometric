package com.jiaoay.biometric.pack

import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Nested class to avoid verification errors for methods introduced in Android 6.0 (API 23).
 */
@RequiresApi(Build.VERSION_CODES.M)
object PackageApi23Impl {
    /**
     * Checks if the given package manager has support for the fingerprint system feature.
     *
     * @param packageManager The system package manager.
     * @return Whether fingerprint is supported.
     */
    fun hasSystemFeatureFingerprint(packageManager: PackageManager): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
    }
}