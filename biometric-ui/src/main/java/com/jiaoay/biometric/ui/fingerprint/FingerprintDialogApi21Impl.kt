package com.jiaoay.biometric.ui.fingerprint

import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable

/**
 * Nested class to avoid verification errors for methods introduced in Android 5.0 (API 21).
 */
object FingerprintDialogApi21Impl {
    /**
     * Starts animating the given icon if it is an [AnimatedVectorDrawable].
     *
     * @param icon A [Drawable] icon asset.
     */
    fun startAnimation(icon: Drawable) {
        if (icon is AnimatedVectorDrawable) {
            icon.start()
        }
    }
}