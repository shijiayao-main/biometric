package com.jiaoay.biometric

import android.content.Context
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.util.Log
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import com.jiaoay.biometric.cancellation.CancellationSignalProvider
import com.jiaoay.biometric.crypto.CryptoObject
import com.jiaoay.biometric.crypto.CryptoObjectUtils

abstract class FingerprintCompat(
    internal val context: Context
) {

    companion object {
        private const val TAG = "FingerprintCompat"
    }


    abstract fun authWithFingerprint(
        cryptoObject: CryptoObject?
    )

    abstract fun recycle()

    /**
     * 有检测到指纹识别设备
     */
    abstract fun isHardWareDetected(): Boolean

    /**
     * 用户设置了指纹
     */
    abstract fun hasEnrolledFingerprints(): Boolean

//    @AuthenticatorTypes
//    private fun isAllowedAuthenticators(): Int {
//        return if (mPromptInfo != null) {
//            AuthenticatorUtils.getConsolidatedAuthenticators(mPromptInfo!!, cryptoObject)
//        } else {
//            0
//        }
//    }

//    @BiometricPrompt.AuthenticationResultType
//    private fun getInferredAuthenticationResultType(): Int {
//        @AuthenticatorTypes
//        val authenticators = isAllowedAuthenticators()
//        return if (AuthenticatorUtils.isSomeBiometricAllowed(authenticators) && !AuthenticatorUtils.isDeviceCredentialAllowed(authenticators)) {
//            BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC
//        } else {
//            BiometricPrompt.AUTHENTICATION_RESULT_TYPE_UNKNOWN
//        }
//    }
}