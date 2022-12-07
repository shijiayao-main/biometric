package com.jiaoay.biometric.crypto

import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import java.io.IOException
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.UnrecoverableKeyException
import java.security.cert.CertificateException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey

/**
 * Utility class for creating and converting between different types of crypto objects that may be
 * used internally by [BiometricPrompt] and [BiometricManager].
 */
object CryptoObjectUtils {
    private const val TAG = "CryptoObjectUtils"

    /**
     * The key name used when creating a fake crypto object.
     */
    private const val FAKE_KEY_NAME = "androidxBiometric"

    /**
     * The name of the Android keystore instance.
     */
    private const val KEYSTORE_INSTANCE = "AndroidKeyStore"

    /**
     * Unwraps a crypto object returned by [android.hardware.biometrics.BiometricPrompt].
     *
     * @param cryptoObject A crypto object from [android.hardware.biometrics.BiometricPrompt].
     * @return An equivalent [CryptoObject] instance.
     */
    @RequiresApi(Build.VERSION_CODES.P)
    fun unwrapFromBiometricPrompt(
        cryptoObject: BiometricPrompt.CryptoObject?
    ): CryptoObject? {
        if (cryptoObject == null) {
            return null
        }
        val cipher = CryptoObjectApi28Impl.getCipher(cryptoObject)
        if (cipher != null) {
            return CryptoObject(cipher)
        }
        val signature = CryptoObjectApi28Impl.getSignature(cryptoObject)
        if (signature != null) {
            return CryptoObject(signature)
        }
        val mac = CryptoObjectApi28Impl.getMac(cryptoObject)
        if (mac != null) {
            return CryptoObject(mac)
        }

        // Identity credential is only supported on API 30 and above.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val identityCredential = CryptoObjectApi30Impl.getIdentityCredential(cryptoObject)
            if (identityCredential != null) {
                return CryptoObject(identityCredential)
            }
        }
        return null
    }

    /**
     * Wraps a crypto object to be passed to [android.hardware.biometrics.BiometricPrompt].
     *
     * @param cryptoObject An instance of [CryptoObject].
     * @return An equivalent crypto object that is compatible with
     * [android.hardware.biometrics.BiometricPrompt].
     */
    @JvmStatic
    @RequiresApi(Build.VERSION_CODES.P)
    fun wrapForBiometricPrompt(cryptoObject: CryptoObject?): BiometricPrompt.CryptoObject? {
        if (cryptoObject == null) {
            return null
        }
        val cipher = cryptoObject.getCipher()
        if (cipher != null) {
            return CryptoObjectApi28Impl.create(cipher)
        }
        val signature = cryptoObject.getSignature()
        if (signature != null) {
            return CryptoObjectApi28Impl.create(signature)
        }
        val mac = cryptoObject.getMac()
        if (mac != null) {
            return CryptoObjectApi28Impl.create(mac)
        }

        // Identity credential is only supported on API 30 and above.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val identityCredential = cryptoObject.getIdentityCredential()
            if (identityCredential != null) {
                return CryptoObjectApi30Impl.create(identityCredential)
            }
        }
        return null
    }

    /**
     * Unwraps a crypto object returned by
     * [androidx.core.hardware.fingerprint.FingerprintManagerCompat].
     *
     * @param cryptoObject A crypto object from
     * [androidx.core.hardware.fingerprint.FingerprintManagerCompat].
     * @return An equivalent [CryptoObject] instance.
     */
    fun unwrapFromFingerprintManager(
        cryptoObject: FingerprintManagerCompat.CryptoObject?
    ): CryptoObject? {
        if (cryptoObject == null) {
            return null
        }
        val cipher = cryptoObject.cipher
        if (cipher != null) {
            return CryptoObject(cipher)
        }
        val signature = cryptoObject.signature
        if (signature != null) {
            return CryptoObject(signature)
        }
        val mac = cryptoObject.mac
        return if (mac != null) {
            CryptoObject(mac)
        } else null
    }

    /**
     * Wraps a crypto object to be passed to
     * [androidx.core.hardware.fingerprint.FingerprintManagerCompat].
     *
     * @param cryptoObject An instance of [CryptoObject].
     * @return An equivalent crypto object that is compatible with
     * [androidx.core.hardware.fingerprint.FingerprintManagerCompat].
     */
    @JvmStatic
    fun wrapForFingerprintManager(cryptoObject: CryptoObject?): FingerprintManagerCompat.CryptoObject? {
        if (cryptoObject == null) {
            return null
        }
        val cipher = cryptoObject.getCipher()
        if (cipher != null) {
            return FingerprintManagerCompat.CryptoObject(
                cipher
            )
        }
        val signature = cryptoObject.getSignature()
        if (signature != null) {
            return FingerprintManagerCompat.CryptoObject(
                signature
            )
        }
        val mac = cryptoObject.getMac()
        if (mac != null) {
            return FingerprintManagerCompat.CryptoObject(
                mac
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
            && cryptoObject.getIdentityCredential() != null
        ) {
            Log.e(TAG, "Identity credential is not supported by FingerprintManager.")
            return null
        }
        return null
    }

    /**
     * Creates a [CryptoObject] instance that can be passed
     * to [BiometricManager] and [BiometricPrompt] in order to force crypto-based
     * authentication behavior.
     *
     * @return An internal-only instance of [CryptoObject].
     */
    @JvmStatic
    @RequiresApi(Build.VERSION_CODES.M)
    fun createFakeCryptoObject(): CryptoObject? {
        return try {
            val keystore = KeyStore.getInstance(KEYSTORE_INSTANCE)
            keystore.load(null)
            val keySpecBuilder = CryptoObjectApi23Impl.createKeyGenParameterSpecBuilder(
                FAKE_KEY_NAME,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
            CryptoObjectApi23Impl.setBlockModeCBC(keySpecBuilder)
            CryptoObjectApi23Impl.setEncryptionPaddingPKCS7(keySpecBuilder)
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_INSTANCE)
            val keySpec = CryptoObjectApi23Impl.buildKeyGenParameterSpec(keySpecBuilder)
            CryptoObjectApi23Impl.initKeyGenerator(keyGenerator, keySpec)
            keyGenerator.generateKey()
            val secretKey = keystore.getKey(FAKE_KEY_NAME, null /* password */) as SecretKey
            val cipher = Cipher.getInstance(
                KeyProperties.KEY_ALGORITHM_AES + "/"
                        + KeyProperties.BLOCK_MODE_CBC + "/"
                        + KeyProperties.ENCRYPTION_PADDING_PKCS7
            )
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            CryptoObject(cipher)
        } catch (e: NoSuchPaddingException) {
            Log.w(TAG, "Failed to create fake crypto object.", e)
            null
        } catch (e: NoSuchAlgorithmException) {
            Log.w(TAG, "Failed to create fake crypto object.", e)
            null
        } catch (e: CertificateException) {
            Log.w(TAG, "Failed to create fake crypto object.", e)
            null
        } catch (e: KeyStoreException) {
            Log.w(TAG, "Failed to create fake crypto object.", e)
            null
        } catch (e: InvalidKeyException) {
            Log.w(TAG, "Failed to create fake crypto object.", e)
            null
        } catch (e: InvalidAlgorithmParameterException) {
            Log.w(TAG, "Failed to create fake crypto object.", e)
            null
        } catch (e: UnrecoverableKeyException) {
            Log.w(TAG, "Failed to create fake crypto object.", e)
            null
        } catch (e: IOException) {
            Log.w(TAG, "Failed to create fake crypto object.", e)
            null
        } catch (e: NoSuchProviderException) {
            Log.w(TAG, "Failed to create fake crypto object.", e)
            null
        }
    }
}