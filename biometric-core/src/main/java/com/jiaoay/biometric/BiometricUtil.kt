package com.jiaoay.biometric

import android.content.Context
import android.os.Build

object BiometricUtil {

    /**
     * Checks if all biometric sensors for the current device can be assumed to meet the
     * **Class 3** (formerly **Strong**) security threshold.
     *
     * @param context The application or activity context.
     * @param model   Model name of the current device.
     * @return Whether the device can be assumed to have only **Class 3** biometrics.
     */
    @JvmStatic
    fun canAssumeStrongBiometrics(context: Context, model: String?): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 (API 30) and above may downgrade a sensor's security class at runtime.
            false
        } else isModelInList(context, model, R.array.assume_strong_biometrics_models)
    }


    /**
     * Checks if the name of the current device vendor matches one in the given string array
     * resource.
     *
     * @param context The application or activity context.
     * @param vendor  Case-insensitive name of the device vendor.
     * @param resId   Resource ID for the string array of vendor names to check against.
     * @return Whether the vendor name matches one in the string array.
     */
    fun isVendorInList(context: Context, vendor: String?, resId: Int): Boolean {
        if (vendor == null) {
            return false
        }
        val vendorNames = context.resources.getStringArray(resId)
        for (vendorName in vendorNames) {
            if (vendor.equals(vendorName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }


    /**
     * Checks if the current device model matches a prefix in the given string array resource.
     *
     * @param context The application or activity context.
     * @param model   Model name of the current device.
     * @param resId   Resource ID for the string array of device model prefixes to check against.
     * @return Whether the model matches a prefix in the string array.
     */
    fun isModelInPrefixList(context: Context, model: String?, resId: Int): Boolean {
        if (model == null) {
            return false
        }
        val modelPrefixes = context.resources.getStringArray(resId)
        for (modelPrefix in modelPrefixes) {
            if (model.startsWith(modelPrefix!!)) {
                return true
            }
        }
        return false
    }


    /**
     * Checks if the current device model matches one in the given string array resource.
     *
     * @param context The application or activity context.
     * @param model   Model name of the current device.
     * @param resId   Resource ID for the string array of device model prefixes to check against.
     * @return Whether the model matches one in the string array.
     */
    fun isModelInList(context: Context, model: String?, resId: Int): Boolean {
        if (model == null) {
            return false
        }
        val modelNames = context.resources.getStringArray(resId)
        for (modelName in modelNames) {
            if (model == modelName) {
                return true
            }
        }
        return false
    }

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
        } else (isVendorInList(context, vendor, R.array.crypto_fingerprint_fallback_vendors)
                || isModelInPrefixList(context, model, R.array.crypto_fingerprint_fallback_prefixes))
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
        } else isModelInPrefixList(context, model, R.array.hide_fingerprint_instantly_prefixes)
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
        } else isModelInList(context, model, R.array.delay_showing_prompt_models)
    }
}