package com.jiaoay.biometric.crypto

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import java.security.InvalidAlgorithmParameterException
import javax.crypto.KeyGenerator

/**
 * Nested class to avoid verification errors for methods introduced in Android 6.0 (API 23).
 */
@RequiresApi(Build.VERSION_CODES.M)
object CryptoObjectApi23Impl {
    /**
     * Creates a new instance of [KeyGenParameterSpec.Builder].
     *
     * @param keystoreAlias The keystore alias for the resulting key.
     * @param purposes      The purposes for which the resulting key will be used.
     * @return An instance of [KeyGenParameterSpec.Builder].
     */
    fun createKeyGenParameterSpecBuilder(
        keystoreAlias: String, purposes: Int
    ): KeyGenParameterSpec.Builder {
        return KeyGenParameterSpec.Builder(keystoreAlias, purposes)
    }

    /**
     * Sets CBC block mode for the given key spec builder.
     *
     * @param keySpecBuilder An instance of [KeyGenParameterSpec.Builder].
     */
    fun setBlockModeCBC(keySpecBuilder: KeyGenParameterSpec.Builder) {
        keySpecBuilder.setBlockModes(KeyProperties.BLOCK_MODE_CBC)
    }

    /**
     * Sets PKCS7 encryption padding for the given key spec builder.
     *
     * @param keySpecBuilder An instance of [KeyGenParameterSpec.Builder].
     */
    fun setEncryptionPaddingPKCS7(keySpecBuilder: KeyGenParameterSpec.Builder) {
        keySpecBuilder.setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
    }

    /**
     * Builds a key spec from the given builder.
     *
     * @param keySpecBuilder An instance of [KeyGenParameterSpec.Builder].
     * @return A [KeyGenParameterSpec] created from the given builder.
     */
    fun buildKeyGenParameterSpec(
        keySpecBuilder: KeyGenParameterSpec.Builder
    ): KeyGenParameterSpec {
        return keySpecBuilder.build()
    }

    /**
     * Calls [KeyGenerator.init] for the given key generator and
     * spec.
     *
     * @param keyGenerator An instance of [KeyGenerator].
     * @param keySpec      The key spec with which to initialize the generator.
     *
     * @throws InvalidAlgorithmParameterException If the key spec is invalid.
     */
    @Throws(InvalidAlgorithmParameterException::class)
    fun initKeyGenerator(
        keyGenerator: KeyGenerator, keySpec: KeyGenParameterSpec
    ) {
        keyGenerator.init(keySpec)
    }
}