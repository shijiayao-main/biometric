package com.jiaoay.biometric

import android.os.Build
import com.jiaoay.biometric.crypto.CryptoObject
import com.jiaoay.biometric.manager.AuthenticatorTypes
import com.jiaoay.biometric.manager.Authenticators

/**
 * Utilities related to [Authenticators] constants.
 */
object AuthenticatorUtils {
    /**
     * A bitmask for the portion of an [AuthenticatorTypes] value related to
     * biometric sensor class.
     */
    private const val BIOMETRIC_CLASS_MASK = 0x7FFF

    /**
     * Converts the given set of allowed authenticator types to a unique, developer-readable string.
     *
     * @param authenticators A bit field representing a set of allowed authenticator types.
     * @return A string that uniquely identifies the set of authenticators and can be used in
     * developer-facing contexts (e.g. error messages).
     */
    fun convertToString(@AuthenticatorTypes authenticators: Int): String {
        return when (authenticators) {
            Authenticators.BIOMETRIC_STRONG -> "BIOMETRIC_STRONG"
            Authenticators.BIOMETRIC_WEAK -> "BIOMETRIC_WEAK"
            Authenticators.DEVICE_CREDENTIAL -> "DEVICE_CREDENTIAL"
            Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL -> "BIOMETRIC_STRONG | DEVICE_CREDENTIAL"
            Authenticators.BIOMETRIC_WEAK or Authenticators.DEVICE_CREDENTIAL -> "BIOMETRIC_WEAK | DEVICE_CREDENTIAL"
            else -> authenticators.toString()
        }
    }

    /**
     * Combines relevant information from the given [PromptInfo] and
     * [CryptoObject] to determine which type(s) of authenticators should be
     * allowed for a given authentication session.
     *
     * @param info   The [PromptInfo] for a given authentication session.
     * @param crypto The [CryptoObject] for a given crypto-based
     * authentication session, or `null` for non-crypto authentication.
     * @return A bit field representing all valid authenticator types that may be invoked.
     */
    @JvmStatic
    @AuthenticatorTypes
    fun getConsolidatedAuthenticators(
        info: PromptInfo,
        crypto: CryptoObject?
    ): Int {
        @AuthenticatorTypes var authenticators: Int
        if (info.getAllowedAuthenticators() != 0) {
            // Use explicitly allowed authenticators if set.
            authenticators = info.getAllowedAuthenticators()
        } else {
            // Crypto auth requires a Class 3 (Strong) biometric.
            authenticators = if (crypto != null) Authenticators.BIOMETRIC_STRONG else Authenticators.BIOMETRIC_WEAK
            if (info.isDeviceCredentialAllowed()) {
                authenticators = authenticators or Authenticators.DEVICE_CREDENTIAL
            }
        }
        return authenticators
    }

    /**
     * Checks if the given set of allowed authenticator types is supported on this Android version.
     *
     * @param authenticators A bit field representing a set of allowed authenticator types.
     * @return Whether user authentication with the given set of allowed authenticator types is
     * supported on the current Android version.
     */
    fun isSupportedCombination(@AuthenticatorTypes authenticators: Int): Boolean {
        return when (authenticators) {
            Authenticators.BIOMETRIC_STRONG, Authenticators.BIOMETRIC_WEAK, Authenticators.BIOMETRIC_WEAK or Authenticators.DEVICE_CREDENTIAL -> true
            Authenticators.DEVICE_CREDENTIAL -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
            Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL -> (Build.VERSION.SDK_INT < Build.VERSION_CODES.P
                    || Build.VERSION.SDK_INT > Build.VERSION_CODES.Q)

            else ->                 // 0 means "no authenticator types" and is supported. Other values are not.
                authenticators == 0
        }
    }

    /**
     * Checks if a device credential is included in the given set of allowed authenticator types.
     *
     * @param authenticators A bit field representing a set of allowed authenticator types.
     * @return Whether [Authenticators.DEVICE_CREDENTIAL] is an allowed authenticator type.
     */
    @JvmStatic
    fun isDeviceCredentialAllowed(
        @AuthenticatorTypes authenticators: Int
    ): Boolean {
        return authenticators and Authenticators.DEVICE_CREDENTIAL != 0
    }

    /**
     * Checks if any biometric class is included in the given set of allowed authenticator types.
     *
     * @param authenticators A bit field representing a set of allowed authenticator types.
     * @return Whether the allowed authenticator types include one or more biometric classes.
     */
    @JvmStatic
    fun isSomeBiometricAllowed(@AuthenticatorTypes authenticators: Int): Boolean {
        return authenticators and BIOMETRIC_CLASS_MASK != 0
    }

    /**
     * Checks if a **Class 2** (formerly **Weak**) biometric is included
     * in the given set of allowed authenticator types.
     *
     * @param authenticators A bit field representing a set of allowed authenticator types.
     * @return Whether [Authenticators.BIOMETRIC_WEAK] is an allowed authenticator type.
     */
    fun isWeakBiometricAllowed(@AuthenticatorTypes authenticators: Int): Boolean {
        return authenticators and Authenticators.BIOMETRIC_WEAK == Authenticators.BIOMETRIC_WEAK
    }
}