package com.jiaoay.biometric

import android.content.Context
import android.util.Log
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import androidx.lifecycle.ViewModel
import com.jiaoay.biometric.authentication.AuthenticationCallbackProvider
import com.jiaoay.biometric.authentication.AuthenticationResult
import com.jiaoay.biometric.cancellation.CancellationSignalProvider
import com.jiaoay.biometric.crypto.CryptoObject
import com.jiaoay.biometric.crypto.CryptoObjectUtils
import com.jiaoay.biometric.manager.AuthenticatorTypes
import com.jiaoay.biometric.util.AuthenticatorUtils
import com.jiaoay.biometric.util.BiometricPrompt

class BiometricViewModel: ViewModel() {

    companion object {
        private const val TAG = "BiometricViewModel"
    }


}