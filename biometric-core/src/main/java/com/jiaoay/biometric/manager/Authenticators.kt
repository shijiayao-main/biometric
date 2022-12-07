package com.jiaoay.biometric.manager


/**
 * Types of authenticators, defined at a level of granularity supported by
 * [BiometricManager] and [BiometricPrompt].
 *
 *
 * Types may combined via bitwise OR into a single integer representing multiple
 * authenticators (e.g. `DEVICE_CREDENTIAL | BIOMETRIC_WEAK`).
 *
 * @see .canAuthenticate
 * @see PromptInfo.Builder.setAllowedAuthenticators
 */
interface Authenticators {
    companion object {
        /**
         * Any biometric (e.g. fingerprint, iris, or face) on the device that meets or exceeds the
         * requirements for **Class 3** (formerly **Strong**), as defined
         * by the Android CDD.
         */
        const val BIOMETRIC_STRONG = 0x000F

        /**
         * Any biometric (e.g. fingerprint, iris, or face) on the device that meets or exceeds the
         * requirements for **Class 2** (formerly **Weak**), as defined by
         * the Android CDD.
         *
         *
         * Note that this is a superset of [.BIOMETRIC_STRONG] and is defined such that
         * `BIOMETRIC_STRONG | BIOMETRIC_WEAK == BIOMETRIC_WEAK`.
         */
        const val BIOMETRIC_WEAK = 0x00FF

        /**
         * The non-biometric credential used to secure the device (i.e. PIN, pattern, or password).
         * This should typically only be used in combination with a biometric auth type, such as
         * [.BIOMETRIC_WEAK].
         */
        const val DEVICE_CREDENTIAL = 1 shl 15
    }
}
