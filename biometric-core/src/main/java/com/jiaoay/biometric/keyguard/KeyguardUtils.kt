package com.jiaoay.biometric.keyguard

import android.app.KeyguardManager
import android.content.Context
import android.os.Build

/**
 * Utilities related to the [KeyguardManager] system service.
 */
object KeyguardUtils {
    /**
     * Gets an instance of the [KeyguardManager] system service.
     *
     * @param context The application or activity context.
     * @return An instance of [KeyguardManager].
     */
    @JvmStatic
    fun getKeyguardManager(context: Context): KeyguardManager? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return KeyguardApi23Impl.getKeyguardManager(context)
        }
        val service = context.getSystemService(Context.KEYGUARD_SERVICE)
        return if (service is KeyguardManager) service else null
    }

    /**
     * Checks if the user has set up a secure PIN, pattern, or password for the device.
     *
     * @param context The application or activity context.
     * @return Whether a PIN/pattern/password has been set, or `false` if unsure.
     */
    @JvmStatic
    fun isDeviceSecuredWithCredential(context: Context): Boolean {
        val keyguardManager = getKeyguardManager(context) ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return KeyguardApi23Impl.isDeviceSecure(keyguardManager)
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            KeyguardApi16Impl.isKeyguardSecure(keyguardManager)
        } else false
    }
}