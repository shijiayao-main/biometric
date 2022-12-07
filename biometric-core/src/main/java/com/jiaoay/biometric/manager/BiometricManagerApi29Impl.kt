package com.jiaoay.biometric.manager

import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import androidx.annotation.RequiresApi
import com.jiaoay.biometric.manager.AuthenticationStatus
import java.lang.reflect.Method

/**
 * Nested class to avoid verification errors for methods introduced in Android 10 (API 29).
 */
@RequiresApi(Build.VERSION_CODES.Q)
object BiometricManagerApi29Impl {
    /**
     * Gets an instance of the framework
     * [android.hardware.biometrics.BiometricManager] class.
     *
     * @param context The application or activity context.
     * @return An instance of [android.hardware.biometrics.BiometricManager].
     */
    @JvmStatic
    fun create(context: Context): BiometricManager? {
        return context.getSystemService(BiometricManager::class.java)
    }

    /**
     * Calls [android.hardware.biometrics.BiometricManager.canAuthenticate] for the
     * given biometric manager.
     *
     * @param biometricManager An instance of
     * [android.hardware.biometrics.BiometricManager].
     * @return The result of
     * [android.hardware.biometrics.BiometricManager.canAuthenticate].
     */
    @JvmStatic
    @AuthenticationStatus
    fun canAuthenticate(
        biometricManager: BiometricManager
    ): Int {
        return biometricManager.canAuthenticate()
    }

    /**
     * Checks for and returns the hidden [android.hardware.biometrics.BiometricManager]
     * method `canAuthenticate(CryptoObject)` via reflection.
     *
     * @return The method `BiometricManager#canAuthenticate(CryptoObject)`, if present.
     */
    @JvmStatic
    fun getCanAuthenticateWithCryptoMethod(): Method? {
        return try {
            BiometricManager::class.java.getMethod(
                "canAuthenticate",
                BiometricPrompt.CryptoObject::class.java
            )
        } catch (e: NoSuchMethodException) {
            null
        }
    }
}