package com.jiaoay.biometric.crypto

import android.os.Build
import android.security.identity.IdentityCredential
import androidx.annotation.RequiresApi
import java.security.Signature
import javax.crypto.Cipher
import javax.crypto.Mac

/**
 * A wrapper class for the crypto objects supported by {@link BiometricPrompt}.
 */
class CryptoObject {
    private var mSignature: Signature? = null
    private var mCipher: Cipher? = null
    private var mMac: Mac? = null
    private var mIdentityCredential: IdentityCredential? = null

    /**
     * Creates a crypto object that wraps the given signature object.
     *
     * @param signature The signature to be associated with this crypto object.
     */
    constructor(signature: Signature) {
        mSignature = signature
        mCipher = null
        mMac = null
        mIdentityCredential = null
    }

    /**
     * Creates a crypto object that wraps the given cipher object.
     *
     * @param cipher The cipher to be associated with this crypto object.
     */
    constructor(cipher: Cipher) {
        mSignature = null
        mCipher = cipher
        mMac = null
        mIdentityCredential = null
    }

    /**
     * Creates a crypto object that wraps the given MAC object.
     *
     * @param mac The MAC to be associated with this crypto object.
     */
    constructor(mac: Mac) {
        mSignature = null
        mCipher = null
        mMac = mac
        mIdentityCredential = null
    }

    /**
     * Creates a crypto object that wraps the given identity credential object.
     *
     * @param identityCredential The identity credential to be associated with this crypto
     * object.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    constructor(
        identityCredential: IdentityCredential
    ) {
        mSignature = null
        mCipher = null
        mMac = null
        mIdentityCredential = identityCredential
    }

    /**
     * Gets the signature object associated with this crypto object.
     *
     * @return The signature, or `null` if none is associated with this object.
     */
    fun getSignature(): Signature? {
        return mSignature
    }

    /**
     * Gets the cipher object associated with this crypto object.
     *
     * @return The cipher, or `null` if none is associated with this object.
     */
    fun getCipher(): Cipher? {
        return mCipher
    }

    /**
     * Gets the MAC object associated with this crypto object.
     *
     * @return The MAC, or `null` if none is associated with this object.
     */
    fun getMac(): Mac? {
        return mMac
    }

    /**
     * Gets the identity credential object associated with this crypto object.
     *
     * @return The identity credential, or `null` if none is associated with this object.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun getIdentityCredential(): IdentityCredential? {
        return mIdentityCredential
    }
}