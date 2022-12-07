package com.jiaoay.biometric.manager

import androidx.annotation.IntDef

/**
 * A status code that may be returned when checking for biometric authentication.
 */
@IntDef(
    BiometricManager.BIOMETRIC_SUCCESS,
    BiometricManager.BIOMETRIC_STATUS_UNKNOWN,
    BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED,
    BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
    BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
    BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
    BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED
)
@Retention(AnnotationRetention.SOURCE)
annotation class AuthenticationStatus
