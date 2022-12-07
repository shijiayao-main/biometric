package com.jiaoay.biometric.manager

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import com.jiaoay.biometric.BiometricUtil
import com.jiaoay.biometric.keyguard.KeyguardUtils
import com.jiaoay.biometric.pack.PackageUtils

/**
 * Provides the default class and method dependencies that will be used in production.
 */
class DefaultBiometricManagerInjector internal constructor(context: Context) : BiometricManagerInjector {

    private val mContext: Context

    /**
     * Creates a default injector from the given context.
     *
     * @param context The application or activity context.
     */
    init {
        mContext = context.applicationContext
    }

    @get:RequiresApi(Build.VERSION_CODES.Q)
    override val biometricManager: android.hardware.biometrics.BiometricManager?
        get() = BiometricManagerApi29Impl.create(mContext)
    override val fingerprintManager: FingerprintManagerCompat?
        get() = FingerprintManagerCompat.from(mContext)
    override val isDeviceSecurable: Boolean
        get() = KeyguardUtils.getKeyguardManager(mContext) != null
    override val isDeviceSecuredWithCredential: Boolean
        get() = KeyguardUtils.isDeviceSecuredWithCredential(mContext)
    override val isFingerprintHardwarePresent: Boolean
        get() = PackageUtils.hasSystemFeatureFingerprint(mContext)
    override val isStrongBiometricGuaranteed: Boolean
        get() = BiometricUtil.canAssumeStrongBiometrics(mContext, Build.MODEL)
}
