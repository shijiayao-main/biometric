package com.jiaoay.biometric

import android.content.Context
import android.content.DialogInterface
import android.hardware.biometrics.BiometricPrompt
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import com.jiaoay.biometric.cancellation.CancellationSignalProvider
import com.jiaoay.biometric.crypto.CryptoObject
import com.jiaoay.biometric.crypto.CryptoObjectUtils
import com.jiaoay.biometric.manager.Authenticators
import com.jiaoay.biometric.util.AuthenticatorUtils
import java.util.concurrent.Executor

@RequiresApi(Build.VERSION_CODES.P)
class FingerprintApi28Compat(context: Context) : FingerprintCompat(context) {

    companion object {
        private const val TAG = "FingerprintApi28Compat"
    }

    private val cancellationSignalProvider by lazy {
        CancellationSignalProvider()
    }

    val executor: Executor by lazy {
        object : Executor {
            private val handler = Handler(Looper.getMainLooper())
            override fun execute(command: Runnable?) {
                if (command != null) {
                    handler.post(command)
                }
            }
        }
    }

    override fun authWithFingerprint(cryptoObject: CryptoObject?) {
        // TODO:
        val authenticators = 0

        val biometricCryptoObject = if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.R &&
            authenticators == Authenticators.BIOMETRIC_STRONG &&
            cryptoObject == null
        ) {
            CryptoObjectUtils.createFakeCryptoObject()
        } else {
            cryptoObject
        }

        val biometricPromptBuilder = BiometricPrompt.Builder(
            context
        )
            .setTitle("我也不想设置title, 这实在是太阴间了")
            .setNegativeButton("阴间中的阴间", executor, object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {

                }
            })
        // TODO:
        val isConfirmationRequired = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            biometricPromptBuilder.setConfirmationRequired(isConfirmationRequired)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            biometricPromptBuilder.setAllowedAuthenticators(authenticators)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val deviceCredentialAllowed = AuthenticatorUtils.isDeviceCredentialAllowed(authenticators)
            biometricPromptBuilder.setDeviceCredentialAllowed(
                deviceCredentialAllowed
            )
        }

        val biometricPrompt = biometricPromptBuilder.build()

        val cryptoObjectWrapper = CryptoObjectUtils.wrapForBiometricPrompt(biometricCryptoObject)
        val cancellationSignal = cancellationSignalProvider.biometricCancellationSignal

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                super.onAuthenticationError(errorCode, errString)
            }

            override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence?) {
                super.onAuthenticationHelp(helpCode, helpString)
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                super.onAuthenticationSucceeded(result)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
            }
        }

        try {
            if (cryptoObjectWrapper == null) {
                biometricPrompt.authenticate(
                    cancellationSignal,
                    executor,
                    callback
                )
            } else {
                biometricPrompt.authenticate(
                    cryptoObjectWrapper,
                    cancellationSignal,
                    executor,
                    callback
                )
            }
        } catch (e: NullPointerException) {
            // Catch and handle NPE if thrown by framework call to authenticate() (b/151316421).
            Log.e(TAG, "Got NPE while authenticating with biometric prompt.", e)
//             TODO:
//              val errorCode = com.jiaoay.biometric.util.BiometricPrompt.ERROR_HW_UNAVAILABLE
//              val errorString = context?.getString(R.string.default_error_msg) ?: ""
//              sendErrorAndDismiss(errorCode, errorString)
        }
    }

    override fun recycle() {

    }

    override fun isHardWareDetected(): Boolean {
        return true
    }

    override fun hasEnrolledFingerprints(): Boolean {
        return true
    }
}