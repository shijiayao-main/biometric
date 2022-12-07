package com.jiaoay.biometric.manager

import androidx.annotation.IntDef

/**
 * A bitwise combination of authenticator types defined in [Authenticators].
 */
@IntDef(
    flag = true,
    value = [
        Authenticators.BIOMETRIC_STRONG,
        Authenticators.BIOMETRIC_WEAK,
        Authenticators.DEVICE_CREDENTIAL
    ]
)
@Retention(AnnotationRetention.SOURCE)
annotation class AuthenticatorTypes
