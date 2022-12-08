package com.jiaoay.biometric

import android.content.Context
import android.util.Log
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import com.jiaoay.biometric.cancellation.CancellationSignalProvider
import com.jiaoay.biometric.crypto.CryptoObject
import com.jiaoay.biometric.crypto.CryptoObjectUtils

class FingerprintApi21Compat(context: Context) : FingerprintCompat(context) {

    companion object {
        private const val TAG = "FingerprintApi21Compat"
    }

    private val cancellationSignalProvider by lazy {
        CancellationSignalProvider()
    }

    private val fingerprintManager by lazy {
        FingerprintManagerCompat.from(context)
    }

    override fun authWithFingerprint(cryptoObject: CryptoObject?) {

        if (isHardWareDetected().not()) {
            // 没有硬件
            return
        }

        if (hasEnrolledFingerprints().not()) {
            // 未注册指纹
            return
        }

        val crypto = CryptoObjectUtils.wrapForFingerprintManager(cryptoObject)
        val cancellationSignal = cancellationSignalProvider.fingerprintCancellationSignal
        try {
            fingerprintManager.authenticate(
                crypto,
                0,
                cancellationSignal,
                object : FingerprintManagerCompat.AuthenticationCallback() {
                    override fun onAuthenticationError(errMsgId: Int, errString: CharSequence?) {
                        super.onAuthenticationError(errMsgId, errString)
                    }

                    override fun onAuthenticationHelp(helpMsgId: Int, helpString: CharSequence?) {
                        super.onAuthenticationHelp(helpMsgId, helpString)
                    }

                    override fun onAuthenticationSucceeded(result: FingerprintManagerCompat.AuthenticationResult?) {
                        super.onAuthenticationSucceeded(result)
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                    }
                },
                null
            )
        } catch (e: NullPointerException) {
            // Catch and handle NPE if thrown by framework call to authenticate() (b/151316421).
            Log.e(TAG, "Got NPE while authenticating with fingerprint.", e)
            // TODO:
            //  sendErrorAndDismiss(
            //      errorCode, getFingerprintErrorString(context, errorCode)
            //  )
        }
    }

    override fun isHardWareDetected(): Boolean {
        return fingerprintManager.isHardwareDetected
    }

    override fun hasEnrolledFingerprints(): Boolean {
        return fingerprintManager.hasEnrolledFingerprints()
    }
}