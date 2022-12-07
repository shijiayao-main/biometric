package com.jiaoay.biometric.keyguard

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Nested class to avoid verification errors for methods introduced in Android 6.0 (API 23).
 */
@RequiresApi(Build.VERSION_CODES.M)
object KeyguardApi23Impl {
    /**
     * Gets an instance of the [KeyguardManager] system service.
     *
     * @param context The application or activity context.
     * @return An instance of [KeyguardManager].
     */
    fun getKeyguardManager(context: Context): KeyguardManager? {
        return context.getSystemService(KeyguardManager::class.java)
    }

    /**
     * Calls [KeyguardManager.isDeviceSecure] for the given keyguard manager.
     *
     * @param keyguardManager An instance of [KeyguardManager].
     * @return The result of [KeyguardManager.isDeviceSecure].
     */
    fun isDeviceSecure(keyguardManager: KeyguardManager): Boolean {
        return keyguardManager.isDeviceSecure
    }
}