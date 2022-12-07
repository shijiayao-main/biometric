package com.jiaoay.biometric;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * Utilities related to the {@link KeyguardManager} system service.
 */
class KeyguardUtils {
    // Prevent instantiation.
    private KeyguardUtils() {}

    /**
     * Gets an instance of the {@link KeyguardManager} system service.
     *
     * @param context The application or activity context.
     * @return An instance of {@link KeyguardManager}.
     */
    @Nullable
    static KeyguardManager getKeyguardManager(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Api23Impl.getKeyguardManager(context);
        }
        final Object service = context.getSystemService(Context.KEYGUARD_SERVICE);
        return service instanceof KeyguardManager ? (KeyguardManager) service : null;
    }

    /**
     * Checks if the user has set up a secure PIN, pattern, or password for the device.
     *
     * @param context The application or activity context.
     * @return Whether a PIN/pattern/password has been set, or {@code false} if unsure.
     */
    static boolean isDeviceSecuredWithCredential(@NonNull Context context) {
        final KeyguardManager keyguardManager = getKeyguardManager(context);
        if (keyguardManager == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Api23Impl.isDeviceSecure(keyguardManager);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return Api16Impl.isKeyguardSecure(keyguardManager);
        }
        return false;
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 6.0 (API 23).
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private static class Api23Impl {
        // Prevent instantiation.
        private Api23Impl() {}

        /**
         * Gets an instance of the {@link KeyguardManager} system service.
         *
         * @param context The application or activity context.
         * @return An instance of {@link KeyguardManager}.
         */
        @Nullable
        static KeyguardManager getKeyguardManager(@NonNull Context context) {
            return context.getSystemService(KeyguardManager.class);
        }

        /**
         * Calls {@link KeyguardManager#isDeviceSecure()} for the given keyguard manager.
         *
         * @param keyguardManager An instance of {@link KeyguardManager}.
         * @return The result of {@link KeyguardManager#isDeviceSecure()}.
         */
        static boolean isDeviceSecure(@NonNull KeyguardManager keyguardManager) {
            return keyguardManager.isDeviceSecure();
        }
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 4.1 (API 16).
     */
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    private static class Api16Impl {
        // Prevent instantiation.
        private Api16Impl() {}

        /**
         * Calls {@link KeyguardManager#isKeyguardSecure()} for the given keyguard manager.
         *
         * @param keyguardManager An instance of {@link KeyguardManager}.
         * @return The result of {@link KeyguardManager#isKeyguardSecure()}.
         */
        static boolean isKeyguardSecure(@NonNull KeyguardManager keyguardManager) {
            return keyguardManager.isKeyguardSecure();
        }
    }
}
