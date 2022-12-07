package com.jiaoay.biometric.ui.fragment

import android.app.KeyguardManager
import android.content.Intent

/**
 * Nested class to avoid verification errors for methods introduced in Android 5.0 (API 21).
 */
object BiometricFragmentApi21Impl {

    /**
     * Calls
     * [KeyguardManager.createConfirmDeviceCredentialIntent]
     * for the given keyguard manager.
     *
     * @param keyguardManager An instance of [KeyguardManager].
     * @param title           The title for the confirm device credential activity.
     * @param description     The description for the confirm device credential activity.
     * @return An intent that can be used to launch the confirm device credential activity.
     */
    @JvmStatic
    fun createConfirmDeviceCredentialIntent(
        keyguardManager: KeyguardManager,
        title: CharSequence?,
        description: CharSequence?
    ): Intent? {
        return keyguardManager.createConfirmDeviceCredentialIntent(title, description)
    }
}