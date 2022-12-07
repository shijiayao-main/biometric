package com.jiaoay.biometric.keyguard

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Nested class to avoid verification errors for methods introduced in Android 4.1 (API 16).
 */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
object KeyguardApi16Impl {
    /**
     * Calls [KeyguardManager.isKeyguardSecure] for the given keyguard manager.
     *
     * @param keyguardManager An instance of [KeyguardManager].
     * @return The result of [KeyguardManager.isKeyguardSecure].
     */
    fun isKeyguardSecure(keyguardManager: KeyguardManager): Boolean {
        return keyguardManager.isKeyguardSecure
    }
}