package com.jiaoay.biometric.ui.fragment

import android.content.Context
import android.content.DialogInterface
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.annotation.RequiresApi
import java.util.concurrent.Executor


/**
 * Nested class to avoid verification errors for methods introduced in Android 9.0 (API 28).
 */
@RequiresApi(Build.VERSION_CODES.P)
object BiometricFragmentApi28Impl {


    /**
     * Creates an instance of the framework class
     * [android.hardware.biometrics.BiometricPrompt.Builder].
     *
     * @param context The application or activity context.
     * @return An instance of [android.hardware.biometrics.BiometricPrompt.Builder].
     */
    @JvmStatic
    fun createPromptBuilder(
        context: Context
    ): BiometricPrompt.Builder {
        return BiometricPrompt.Builder(context)
    }

    /**
     * Sets the title for the given framework prompt builder.
     *
     * @param builder An instance of
     * [android.hardware.biometrics.BiometricPrompt.Builder].
     * @param title   The title for the prompt.
     */
    @JvmStatic
    fun setTitle(
        builder: BiometricPrompt.Builder,
        title: CharSequence
    ) {
        builder.setTitle(title)
    }

    /**
     * Sets the subtitle for the given framework prompt builder.
     *
     * @param builder  An instance of
     * [android.hardware.biometrics.BiometricPrompt.Builder].
     * @param subtitle The subtitle for the prompt.
     */
    @JvmStatic
    fun setSubtitle(
        builder: BiometricPrompt.Builder,
        subtitle: CharSequence
    ) {
        builder.setSubtitle(subtitle)
    }

    /**
     * Sets the description for the given framework prompt builder.
     *
     * @param builder     An instance of
     * [android.hardware.biometrics.BiometricPrompt.Builder].
     * @param description The description for the prompt.
     */
    @JvmStatic
    fun setDescription(
        builder: BiometricPrompt.Builder,
        description: CharSequence
    ) {
        builder.setDescription(description)
    }

    /**
     * Sets the negative button text and behavior for the given framework prompt builder.
     *
     * @param builder  An instance of
     * [android.hardware.biometrics.BiometricPrompt.Builder].
     * @param text     The text for the negative button.
     * @param executor An executor for the negative button callback.
     * @param listener A listener for the negative button press event.
     */
    @JvmStatic
    fun setNegativeButton(
        builder: BiometricPrompt.Builder,
        text: CharSequence,
        executor: Executor,
        listener: DialogInterface.OnClickListener
    ) {
        builder.setNegativeButton(text, executor, listener)
    }

    /**
     * Creates an instance of the framework class
     * [android.hardware.biometrics.BiometricPrompt] from the given builder.
     *
     * @param builder The builder for the prompt.
     * @return An instance of [android.hardware.biometrics.BiometricPrompt].
     */
    @JvmStatic
    fun buildPrompt(
        builder: BiometricPrompt.Builder
    ): BiometricPrompt {
        return builder.build()
    }

    /**
     * Starts (non-crypto) authentication for the given framework biometric prompt.
     *
     * @param biometricPrompt    An instance of
     * [android.hardware.biometrics.BiometricPrompt].
     * @param cancellationSignal A cancellation signal object for the prompt.
     * @param executor           An executor for authentication callbacks.
     * @param callback           An object that will receive authentication events.
     */
    @JvmStatic
    fun authenticate(
        biometricPrompt: BiometricPrompt,
        cancellationSignal: CancellationSignal,
        executor: Executor,
        callback: BiometricPrompt.AuthenticationCallback
    ) {
        biometricPrompt.authenticate(cancellationSignal, executor, callback)
    }

    /**
     * Starts (crypto-based) authentication for the given framework biometric prompt.
     *
     * @param biometricPrompt    An instance of
     * [android.hardware.biometrics.BiometricPrompt].
     * @param crypto             A crypto object associated with the given authentication.
     * @param cancellationSignal A cancellation signal object for the prompt.
     * @param executor           An executor for authentication callbacks.
     * @param callback           An object that will receive authentication events.
     */
    @JvmStatic
    fun authenticate(
        biometricPrompt: BiometricPrompt,
        crypto: BiometricPrompt.CryptoObject,
        cancellationSignal: CancellationSignal,
        executor: Executor,
        callback: BiometricPrompt.AuthenticationCallback
    ) {
        biometricPrompt.authenticate(crypto, cancellationSignal, executor, callback)
    }
}