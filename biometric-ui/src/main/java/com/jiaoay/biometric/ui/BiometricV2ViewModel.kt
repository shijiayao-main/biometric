package com.jiaoay.biometric.ui

import android.content.Context
import android.util.Log
import androidx.annotation.IntDef
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import androidx.lifecycle.ViewModel
import com.jiaoay.biometric.AuthenticatorUtils
import com.jiaoay.biometric.BiometricPrompt
import com.jiaoay.biometric.authentication.AuthenticationCallbackProvider
import com.jiaoay.biometric.authentication.AuthenticationResult
import com.jiaoay.biometric.cancellation.CancellationSignalProvider
import com.jiaoay.biometric.crypto.CryptoObject
import com.jiaoay.biometric.crypto.CryptoObjectUtils
import com.jiaoay.biometric.manager.AuthenticatorTypes
import com.jiaoay.biometric.ui.fragment.BiometricFragment

class BiometricV2ViewModel : ViewModel() {

    /**
     * Where authentication was canceled from.
     */
    @IntDef(BiometricFragment.CANCELED_FROM_INTERNAL, BiometricFragment.CANCELED_FROM_USER, BiometricFragment.CANCELED_FROM_NEGATIVE_BUTTON, BiometricFragment.CANCELED_FROM_CLIENT)
    @Retention(AnnotationRetention.SOURCE)
    internal annotation class CanceledFrom

    companion object {
        private const val TAG = "BiometricV2ViewModel"
    }

    private val cancellationSignalProvider by lazy {
        CancellationSignalProvider()
    }

    @AuthenticatorTypes
    private fun isAllowedAuthenticators(): Int {
        return 0
//        return if (mPromptInfo != null) {
//            AuthenticatorUtils.getConsolidatedAuthenticators(mPromptInfo!!, cryptoObject)
//        } else {
//            0
//        }
    }

    @BiometricPrompt.AuthenticationResultType
    private fun getInferredAuthenticationResultType(): Int {
        @AuthenticatorTypes
        val authenticators = isAllowedAuthenticators()
        return if (AuthenticatorUtils.isSomeBiometricAllowed(authenticators) && !AuthenticatorUtils.isDeviceCredentialAllowed(authenticators)) {
            BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC
        } else {
            BiometricPrompt.AUTHENTICATION_RESULT_TYPE_UNKNOWN
        }
    }

    private val authenticationCallbackProvider by lazy {
        AuthenticationCallbackProvider(object : AuthenticationCallbackProvider.Listener() {
            override fun onSuccess(result: AuthenticationResult) {
                var result = result
                // Try to infer the authentication type if unknown.
                if (result.getAuthenticationType() == BiometricPrompt.AUTHENTICATION_RESULT_TYPE_UNKNOWN) {
                    result = AuthenticationResult(
                        result.getCryptoObject(),
                        getInferredAuthenticationResultType()
                    )
                }
                // TODO:
                // setAuthenticationResult(result)
            }

            override fun onError(errorCode: Int, errorMessage: CharSequence?) {
                super.onError(errorCode, errorMessage)
            }

            override fun onHelp(helpMessage: CharSequence?) {
                super.onHelp(helpMessage)
            }

            override fun onFailure() {
                super.onFailure()
            }
        })
    }

    /**
     * Shows the fingerprint dialog UI to the user and begins authentication.
     */
    fun showFingerprintDialogForAuthentication(context: Context) {
        val fingerprintManagerCompat = FingerprintManagerCompat.from(context)
        val errorCode = checkForFingerprintPreAuthenticationErrors(fingerprintManagerCompat)
        if (errorCode != BiometricPrompt.BIOMETRIC_SUCCESS) {
//            sendErrorAndDismiss(
//                errorCode, ErrorUtils.getFingerprintErrorString(context, errorCode)
//            )
            return
        }
        authenticateWithFingerprint(
            fingerprintManager = fingerprintManagerCompat,
            context = context,
            cryptoObject = null
        )
    }


    /**
     * Checks for possible error conditions prior to starting fingerprint authentication.
     *
     * @return 0 if there is no error, or a nonzero integer identifying the specific error.
     */
    private fun checkForFingerprintPreAuthenticationErrors(
        fingerprintManager: FingerprintManagerCompat
    ): Int {
        if (!fingerprintManager.isHardwareDetected) {
            return BiometricPrompt.ERROR_HW_NOT_PRESENT
        } else if (!fingerprintManager.hasEnrolledFingerprints()) {
            return BiometricPrompt.ERROR_NO_BIOMETRICS
        }
        return BiometricPrompt.BIOMETRIC_SUCCESS
    }

    fun authenticateWithFingerprint(
        fingerprintManager: FingerprintManagerCompat,
        context: Context,
        cryptoObject: CryptoObject?
    ) {
        val crypto = CryptoObjectUtils.wrapForFingerprintManager(cryptoObject)
        val cancellationSignal = cancellationSignalProvider.fingerprintCancellationSignal
        val callback = authenticationCallbackProvider.fingerprintCallback
        try {
            fingerprintManager.authenticate(
                crypto, 0, cancellationSignal, callback, null /* handler */
            )
        } catch (e: NullPointerException) {
            // Catch and handle NPE if thrown by framework call to authenticate() (b/151316421).
            Log.e(TAG, "Got NPE while authenticating with fingerprint.", e)
            val errorCode = BiometricPrompt.ERROR_HW_UNAVAILABLE
            // TODO:
            //  sendErrorAndDismiss(
            //      errorCode, getFingerprintErrorString(context, errorCode)
            //  )
        }
    }

    /**
     * Cancels the ongoing authentication session and sends an error to the client callback.
     *
     * @param canceledFrom Where authentication was canceled from.
     */
    fun cancelAuthentication(@CanceledFrom canceledFrom: Int) {
//        if (canceledFrom != BiometricFragment.CANCELED_FROM_CLIENT && mViewModel.isIgnoringCancel) {
        if (canceledFrom != BiometricFragment.CANCELED_FROM_CLIENT) {
            return
        }
//        if (isUsingFingerprintDialog) {
//            mViewModel.canceledFrom = canceledFrom
//            if (canceledFrom == BiometricFragment.CANCELED_FROM_USER) {
//                val errorCode = com.jiaoay.biometric.BiometricPrompt.ERROR_USER_CANCELED
//                sendErrorToClient(
//                    errorCode, ErrorUtils.getFingerprintErrorString(context, errorCode)
//                )
//            }
//        }
        cancellationSignalProvider.cancel()
    }

}