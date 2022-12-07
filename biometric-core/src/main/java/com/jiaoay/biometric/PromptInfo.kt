package com.jiaoay.biometric

import android.os.Build
import android.text.TextUtils
import com.jiaoay.biometric.BiometricManager.AuthenticatorTypes

/**
 * A set of configurable options for how the {@link BiometricPrompt} should appear and behave.
 */
class PromptInfo(
    title: CharSequence,
    subtitle: CharSequence,
    description: CharSequence,
    negativeButtonText: CharSequence,
    confirmationRequired: Boolean,
    deviceCredentialAllowed: Boolean,
    @AuthenticatorTypes allowedAuthenticators: Int
) {
    /**
     * A builder used to set individual options for the [PromptInfo] class.
     */
    class Builder() {
        // Mutable options to be set on the builder.
        private var mTitle: CharSequence = ""
        private var mSubtitle: CharSequence = ""
        private var mDescription: CharSequence = ""
        private var mNegativeButtonText: CharSequence = ""
        private var mIsConfirmationRequired = true
        private var mIsDeviceCredentialAllowed = false

        @AuthenticatorTypes
        private var mAllowedAuthenticators = 0

        /**
         * Required: Sets the title for the prompt.
         *
         * @param title The title to be displayed on the prompt.
         * @return This builder.
         */
        fun setTitle(title: CharSequence): Builder {
            mTitle = title
            return this
        }

        /**
         * Optional: Sets the subtitle for the prompt.
         *
         * @param subtitle The subtitle to be displayed on the prompt.
         * @return This builder.
         */
        fun setSubtitle(subtitle: CharSequence): Builder {
            mSubtitle = subtitle
            return this
        }

        /**
         * Optional: Sets the description for the prompt.
         *
         * @param description The description to be displayed on the prompt.
         * @return This builder.
         */
        fun setDescription(description: CharSequence): Builder {
            mDescription = description
            return this
        }

        /**
         * Required: Sets the text for the negative button on the prompt.
         *
         *
         * Note that this option is incompatible with device credential authentication and
         * must NOT be set if the latter is enabled via [.setAllowedAuthenticators]
         * or [.setDeviceCredentialAllowed].
         *
         * @param negativeButtonText The label to be used for the negative button on the prompt.
         * @return This builder.
         */
        fun setNegativeButtonText(negativeButtonText: CharSequence): Builder {
            mNegativeButtonText = negativeButtonText
            return this
        }

        /**
         * Optional: Sets a system hint for whether to require explicit user confirmation after
         * a passive biometric (e.g. iris or face) has been recognized but before
         * [AuthenticationCallback.onAuthenticationSucceeded] is
         * called. Defaults to `true`.
         *
         *
         * Disabling this option is generally only appropriate for frequent, low-value
         * transactions, such as re-authenticating for a previously authorized application.
         *
         *
         * Also note that, as it is merely a hint, this option may be ignored by the system.
         * For example, the system may choose to instead always require confirmation if the user
         * has disabled passive authentication for their device in Settings. Additionally, this
         * option will be ignored on devices running OS versions prior to Android 10 (API 29).
         *
         * @param confirmationRequired Whether this option should be enabled.
         * @return This builder.
         */
        fun setConfirmationRequired(confirmationRequired: Boolean): Builder {
            mIsConfirmationRequired = confirmationRequired
            return this
        }

        /**
         * Optional: Sets whether the user should be given the option to authenticate with
         * their device PIN, pattern, or password instead of a biometric. Defaults to
         * `false`.
         *
         *
         * Note that this option is incompatible with
         * [Builder.setNegativeButtonText] and must NOT be
         * enabled if the latter is set.
         *
         *
         * Before enabling this option, developers should check whether the device is secure
         * by calling [android.app.KeyguardManager.isDeviceSecure]. If the device is not
         * secure, authentication will fail with [.ERROR_NO_DEVICE_CREDENTIAL].
         *
         *
         * On versions prior to Android 10 (API 29), calls to
         * [.cancelAuthentication] will not work as expected after the
         * user has chosen to authenticate with their device credential. This is because the
         * library internally launches a separate activity (by calling
         * [android.app.KeyguardManager.createConfirmDeviceCredentialIntent]) that does not have a public API for cancellation.
         *
         * @param deviceCredentialAllowed Whether this option should be enabled.
         * @return This builder.
         *
         */
        @Deprecated("Use {@link #setAllowedAuthenticators(int)} instead.")
        fun setDeviceCredentialAllowed(deviceCredentialAllowed: Boolean): Builder {
            mIsDeviceCredentialAllowed = deviceCredentialAllowed
            return this
        }

        /**
         * Optional: Specifies the type(s) of authenticators that may be invoked by
         * [BiometricPrompt] to authenticate the user. Available authenticator types are
         * defined in [Authenticators] and can be combined via bitwise OR. Defaults to:
         *
         *  * [Authenticators.BIOMETRIC_WEAK] for non-crypto authentication, or
         *  * [Authenticators.BIOMETRIC_STRONG] for crypto-based authentication.
         *
         *
         *
         * Note that not all combinations of authenticator types are supported prior to
         * Android 11 (API 30). Specifically, `DEVICE_CREDENTIAL` alone is unsupported
         * prior to API 30, and `BIOMETRIC_STRONG | DEVICE_CREDENTIAL` is unsupported on
         * API 28-29. Setting an unsupported value on an affected Android version will result in
         * an error when calling [.build].
         *
         *
         * This method should be preferred over [.setDeviceCredentialAllowed]
         * and overrides the latter if both are used. Using this method to enable device
         * credential authentication (with [Authenticators.DEVICE_CREDENTIAL]) will
         * replace the negative button on the prompt, making it an error to also call
         * [.setNegativeButtonText].
         *
         *
         * If this method is used and no authenticator of any of the specified types is
         * available at the time `authenticate()` is called,
         * [AuthenticationCallback.onAuthenticationError] will be
         * invoked with an appropriate error code.
         *
         * @param allowedAuthenticators A bit field representing all valid authenticator types
         * that may be invoked by the prompt.
         * @return This builder.
         */
        fun setAllowedAuthenticators(
            @AuthenticatorTypes allowedAuthenticators: Int
        ): Builder {
            mAllowedAuthenticators = allowedAuthenticators
            return this
        }

        /**
         * Creates a [PromptInfo] object with the specified options.
         *
         * @return The [PromptInfo] object.
         *
         * @throws IllegalArgumentException If any required option is not set, or if any
         * illegal combination of options is present.
         */
        fun build(): PromptInfo {
            if (TextUtils.isEmpty(mTitle)) {
                throw IllegalArgumentException("Title must be set and non-empty.")
            }
            if (!AuthenticatorUtils.isSupportedCombination(mAllowedAuthenticators)) {
                throw IllegalArgumentException(
                    "Authenticator combination is unsupported "
                            + "on API " + Build.VERSION.SDK_INT + ": "
                            + AuthenticatorUtils.convertToString(mAllowedAuthenticators)
                )
            }
            val isDeviceCredentialAllowed = if (mAllowedAuthenticators != 0) AuthenticatorUtils.isDeviceCredentialAllowed(mAllowedAuthenticators) else mIsDeviceCredentialAllowed
            if (TextUtils.isEmpty(mNegativeButtonText) && !isDeviceCredentialAllowed) {
                throw IllegalArgumentException("Negative text must be set and non-empty.")
            }
            if (!TextUtils.isEmpty(mNegativeButtonText) && isDeviceCredentialAllowed) {
                throw IllegalArgumentException(
                    ("Negative text must not be set if device "
                            + "credential authentication is allowed.")
                )
            }
            return PromptInfo(
                mTitle,
                mSubtitle,
                mDescription,
                mNegativeButtonText,
                mIsConfirmationRequired,
                mIsDeviceCredentialAllowed,
                mAllowedAuthenticators
            )
        }
    }

    // Immutable fields for the prompt info object.
    private var mTitle: CharSequence = title
    private var mSubtitle: CharSequence = subtitle
    private var mDescription: CharSequence = description
    private var mNegativeButtonText: CharSequence = negativeButtonText
    private var mIsConfirmationRequired = confirmationRequired
    private var mIsDeviceCredentialAllowed = deviceCredentialAllowed

    @AuthenticatorTypes
    private var mAllowedAuthenticators = allowedAuthenticators

    /**
     * Gets the title for the prompt.
     *
     * @return The title to be displayed on the prompt.
     *
     * @see Builder.setTitle
     */
    fun getTitle(): CharSequence {
        return mTitle
    }

    /**
     * Gets the subtitle for the prompt.
     *
     * @return The subtitle to be displayed on the prompt.
     *
     * @see Builder.setSubtitle
     */
    fun getSubtitle(): CharSequence {
        return mSubtitle
    }

    /**
     * Gets the description for the prompt.
     *
     * @return The description to be displayed on the prompt.
     *
     * @see Builder.setDescription
     */
    fun getDescription(): CharSequence {
        return mDescription
    }

    /**
     * Gets the text for the negative button on the prompt.
     *
     * @return The label to be used for the negative button on the prompt, or an empty string if
     * not set.
     *
     * @see Builder.setNegativeButtonText
     */
    fun getNegativeButtonText(): CharSequence {
        return mNegativeButtonText
    }

    /**
     * Checks if the confirmation required option is enabled for the prompt.
     *
     * @return Whether this option is enabled.
     *
     * @see Builder.setConfirmationRequired
     */
    fun isConfirmationRequired(): Boolean {
        return mIsConfirmationRequired
    }

    /**
     * Checks if the device credential allowed option is enabled for the prompt.
     *
     * @return Whether this option is enabled.
     *
     * @see Builder.setDeviceCredentialAllowed
     */
    @Deprecated("Will be removed with {@link Builder#setDeviceCredentialAllowed(boolean)}.")
    fun isDeviceCredentialAllowed(): Boolean {
        return mIsDeviceCredentialAllowed
    }

    /**
     * Gets the type(s) of authenticators that may be invoked by the prompt.
     *
     * @return A bit field representing all valid authenticator types that may be invoked by
     * the prompt, or 0 if not set.
     *
     * @see Builder.setAllowedAuthenticators
     */
    @AuthenticatorTypes
    fun getAllowedAuthenticators(): Int {
        return mAllowedAuthenticators
    }
}