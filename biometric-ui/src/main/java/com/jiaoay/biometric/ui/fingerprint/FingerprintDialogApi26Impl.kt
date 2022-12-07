package com.jiaoay.biometric.ui.fingerprint

import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Nested class to avoid verification errors for methods introduced in Android 8.0 (API 26).
 */
@RequiresApi(Build.VERSION_CODES.O)
object FingerprintDialogApi26Impl {
    /**
     * Gets the resource ID of the `colorError` style attribute.
     */
    val colorErrorAttr: Int
        get() = androidx.appcompat.R.attr.colorError
}