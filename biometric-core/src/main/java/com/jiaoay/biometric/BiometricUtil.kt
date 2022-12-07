package com.jiaoay.biometric

import android.content.Context
import android.os.Build
import com.jiaoay.biometric.DeviceUtils
import com.jiaoay.biometric.R

object BiometricUtil {

    /**
     * Checks if the current device should explicitly fall back to using
     * [FingerprintDialogFragment] when
     * [BiometricPrompt.authenticate] is called.
     *
     * @param context The application or activity context.
     * @param vendor Name of the device vendor/manufacturer.
     * @param model Model name of the current device.
     * @return Whether the current device should fall back to fingerprint for crypto-based
     * authentication.
     */
    @JvmStatic
    fun shouldUseFingerprintForCrypto(
        context: Context, vendor: String?, model: String?
    ): Boolean {
        return if (Build.VERSION.SDK_INT != Build.VERSION_CODES.P) {
            // This workaround is only needed for API 28.
            false
        } else (DeviceUtils.isVendorInList(context, vendor, R.array.crypto_fingerprint_fallback_vendors)
                || DeviceUtils.isModelInPrefixList(context, model, R.array.crypto_fingerprint_fallback_prefixes))
    }


    /**
     * Checks if the current device should hide [FingerprintDialogFragment] and ensure that
     * [FingerprintDialogFragment] is always dismissed immediately upon receiving an error or
     * cancel signal (e.g. if the dialog is shown behind an overlay).
     *
     * @param context The application or activity context.
     * @param model Model name of the current device.
     * @return Whether the [FingerprintDialogFragment] should be hidden.
     */
    @JvmStatic
    fun shouldHideFingerprintDialog(context: Context, model: String?): Boolean {
        return if (Build.VERSION.SDK_INT != Build.VERSION_CODES.P) {
            // This workaround is only needed for API 28.
            false
        } else DeviceUtils.isModelInPrefixList(context, model, R.array.hide_fingerprint_instantly_prefixes)
    }


    /**
     * Checks if the current device should delay showing a new biometric prompt when the previous
     * prompt was recently dismissed.
     *
     * @param context The application or activity context.
     * @param model   Model name of the current device.
     * @return Whether showing the prompt should be delayed after dismissal.
     */
    @JvmStatic
    fun shouldDelayShowingPrompt(context: Context, model: String?): Boolean {
        return if (Build.VERSION.SDK_INT != Build.VERSION_CODES.Q) {
            // This workaround is only needed for API 29.
            false
        } else DeviceUtils.isModelInList(context, model, R.array.delay_showing_prompt_models)
    }
}