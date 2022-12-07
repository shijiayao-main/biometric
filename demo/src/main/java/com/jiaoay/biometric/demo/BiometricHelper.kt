package com.jiaoay.biometric.demo

import android.os.Build
import androidx.fragment.app.FragmentActivity
import com.jiaoay.biometric.authentication.AuthenticationCallback
import com.jiaoay.biometric.authentication.AuthenticationResult
import com.jiaoay.biometric.manager.BiometricManager
import com.jiaoay.biometric.crypto.CryptoObject
import com.jiaoay.biometric.PromptInfo
import com.jiaoay.biometric.manager.Authenticators
import com.jiaoay.biometric.ui.BiometricDialogManager

class BiometricHelper(private val activity: FragmentActivity) {

    private val biometricManager: BiometricManager = BiometricManager.from(activity)
    private val cryptoHelper by lazy { CryptoHelper() }

    private val availableFeatures: List<BiometricType> =
        listOf(
            "android.hardware.fingerprint" to BiometricType.FINGERPRINT,
            "android.hardware.biometrics.face" to BiometricType.FACE,
            "android.hardware.biometrics.iris" to BiometricType.IRIS
        ).filter {
            activity.packageManager.hasSystemFeature(it.first)
        }.map {
            it.second
        }

    fun getBiometricType(): BiometricType =
        if (checkMinVersion() && cryptoHelper.checkOneBiometricMustBeEnrolled()) {
            when (biometricManager.canAuthenticate(Authenticators.BIOMETRIC_WEAK)) {
                BiometricManager.BIOMETRIC_SUCCESS,
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> when {
                    availableFeatures.isEmpty() -> BiometricType.NONE
                    availableFeatures.size == 1 -> availableFeatures[0]
                    else -> BiometricType.MULTIPLE
                }

                else -> BiometricType.NONE
            }
        } else BiometricType.NONE

    fun biometricEnable(): Boolean = checkMinVersion() && getBiometricType() != BiometricType.NONE

    fun showBiometricPrompt(
        title: String,
        negativeButtonText: String,
        subtitle: String = "",
        description: String = "",
        confirmationRequired: Boolean = true,
        crypto: CryptoObject? = null,
        onError: ((Int, CharSequence) -> Unit)? = null,
        onFailed: (() -> Unit)? = null,
        onSuccess: (AuthenticationResult) -> Unit
    ) {
        showBiometricPrompt(
            promptInfo = PromptInfo.Builder()
                .setTitle(title)
                .setNegativeButtonText(negativeButtonText)
                .setSubtitle(subtitle)
                .setDescription(description)
                .setConfirmationRequired(confirmationRequired)
                .build(),
            authenticationCallback = getAuthenticationCallback(onSuccess, onError, onFailed),
            crypto = crypto
        )
    }

    fun showBiometricPrompt(
        title: String,
        negativeButtonText: String,
        subtitle: String = "",
        description: String = "",
        confirmationRequired: Boolean = true,
        authenticationCallback: AuthenticationCallback,
        crypto: CryptoObject? = null
    ) {
        showBiometricPrompt(
            PromptInfo.Builder()
                .setTitle(title)
                .setNegativeButtonText(negativeButtonText)
                .setSubtitle(subtitle)
                .setDescription(description)
                .setConfirmationRequired(confirmationRequired)
                .build(),
            authenticationCallback,
            crypto
        )
    }

    fun showBiometricPrompt(
        promptInfo: PromptInfo,
        crypto: CryptoObject,
        onError: ((Int, CharSequence) -> Unit)? = null,
        onFailed: (() -> Unit)? = null,
        onSuccess: (AuthenticationResult) -> Unit
    ) {
        showBiometricPrompt(
            promptInfo,
            getAuthenticationCallback(onSuccess, onError, onFailed),
            crypto
        )
    }

    fun showBiometricPrompt(
        promptInfo: PromptInfo,
        authenticationCallback: AuthenticationCallback,
        crypto: CryptoObject? = null
    ) {
        if (biometricEnable()) {
            BiometricDialogManager(
                activity = activity,
                fragmentManager = activity.supportFragmentManager,
                callback = authenticationCallback
            ).apply {
                crypto?.also {
                    authenticate(promptInfo, it)
                } ?: authenticate(promptInfo)
            }
//            BiometricPrompt(
//                activity,
//                ContextCompat.getMainExecutor(activity),
//                authenticationCallback
//            ).apply {
//                crypto?.also {
//                    authenticate(promptInfo, it)
//                } ?: authenticate(promptInfo)
//            }
        }
    }

    private fun checkMinVersion(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

    private fun getAuthenticationCallback(
        onSuccess: (AuthenticationResult) -> Unit,
        onError: ((Int, CharSequence) -> Unit)? = null,
        onFailed: (() -> Unit)? = null
    ): AuthenticationCallback = object : AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            onSuccess(result)
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            onError?.invoke(errorCode, errString)
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            onFailed?.invoke()
        }
    }
}