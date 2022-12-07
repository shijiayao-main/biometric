package com.jiaoay.biometric;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;

/**
 * Utility class for specifying custom behavior based on the vendor and model of the device.
 */
public class DeviceUtils {
    // Prevent instantiation.
    private DeviceUtils() {
    }

    /**
     * Checks if all biometric sensors for the current device can be assumed to meet the
     * <strong>Class 3</strong> (formerly <strong>Strong</strong>) security threshold.
     *
     * @param context The application or activity context.
     * @param model   Model name of the current device.
     * @return Whether the device can be assumed to have only <strong>Class 3</strong> biometrics.
     */
    static boolean canAssumeStrongBiometrics(@NonNull Context context, String model) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 (API 30) and above may downgrade a sensor's security class at runtime.
            return false;
        }
        return isModelInList(context, model, com.jiaoay.biometric.R.array.assume_strong_biometrics_models);
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
    public static boolean isVendorInList(@NonNull Context context, String vendor, int resId) {
        if (vendor == null) {
            return false;
        }

        final String[] vendorNames = context.getResources().getStringArray(resId);
        for (final String vendorName : vendorNames) {
            if (vendor.equalsIgnoreCase(vendorName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the current device model matches a prefix in the given string array resource.
     *
     * @param context The application or activity context.
     * @param model   Model name of the current device.
     * @param resId   Resource ID for the string array of device model prefixes to check against.
     * @return Whether the model matches a prefix in the string array.
     */
    public static boolean isModelInPrefixList(@NonNull Context context, String model, int resId) {
        if (model == null) {
            return false;
        }

        final String[] modelPrefixes = context.getResources().getStringArray(resId);
        for (final String modelPrefix : modelPrefixes) {
            if (model.startsWith(modelPrefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the current device model matches one in the given string array resource.
     *
     * @param context The application or activity context.
     * @param model   Model name of the current device.
     * @param resId   Resource ID for the string array of device model prefixes to check against.
     * @return Whether the model matches one in the string array.
     */
    public static boolean isModelInList(@NonNull Context context, String model, int resId) {
        if (model == null) {
            return false;
        }

        final String[] modelNames = context.getResources().getStringArray(resId);
        for (final String modelName : modelNames) {
            if (model.equals(modelName)) {
                return true;
            }
        }
        return false;
    }
}
