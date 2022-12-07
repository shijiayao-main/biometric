package com.jiaoay.biometric.ui

import android.content.DialogInterface
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jiaoay.biometric.AuthenticatorUtils.getConsolidatedAuthenticators
import com.jiaoay.biometric.AuthenticatorUtils.isDeviceCredentialAllowed
import com.jiaoay.biometric.AuthenticatorUtils.isSomeBiometricAllowed
import com.jiaoay.biometric.BiometricErrorData
import com.jiaoay.biometric.BiometricPrompt
import com.jiaoay.biometric.BiometricPrompt.AuthenticationResultType
import com.jiaoay.biometric.PromptInfo
import com.jiaoay.biometric.authentication.AuthenticationCallback
import com.jiaoay.biometric.authentication.AuthenticationCallbackProvider
import com.jiaoay.biometric.authentication.AuthenticationResult
import com.jiaoay.biometric.cancellation.CancellationSignalProvider
import com.jiaoay.biometric.crypto.CryptoObject
import com.jiaoay.biometric.manager.AuthenticatorTypes
import com.jiaoay.biometric.ui.fragment.BiometricFragment.CanceledFrom
import com.jiaoay.biometric.ui.fingerprint.FingerprintDialogFragment
import com.jiaoay.biometric.ui.fragment.BiometricFragment
import java.lang.ref.WeakReference
import java.util.concurrent.Executor

/**
 * A container for data associated with an ongoing authentication session, including intermediate
 * values needed to display the prompt UI.
 *
 *
 * This model and all of its data is persisted over the lifetime of the client activity that
 * hosts the [BiometricPrompt].
 */
class BiometricViewModel : ViewModel() {
    /**
     * The default executor provided when [.getClientExecutor] is called before
     * [.setClientExecutor].
     */
    private class DefaultExecutor internal constructor() : Executor {
        private val mHandler = Handler(Looper.getMainLooper())
        override fun execute(runnable: Runnable) {
            mHandler.post(runnable)
        }
    }

    /**
     * The authentication callback listener passed to [AuthenticationCallbackProvider] when
     * [.getAuthenticationCallbackProvider] is called.
     */
    private class CallbackListener internal constructor(viewModel: BiometricViewModel?) : AuthenticationCallbackProvider.Listener() {
        private val mViewModelRef: WeakReference<BiometricViewModel?>

        /**
         * Creates a callback listener with a weak reference to the given view model.
         *
         * @param viewModel The view model instance to hold a weak reference to.
         */
        init {
            mViewModelRef = WeakReference(viewModel)
        }

        override fun onSuccess(result: AuthenticationResult) {
            var result = result
            if (mViewModelRef.get() != null && mViewModelRef.get()!!.isAwaitingResult) {
                // Try to infer the authentication type if unknown.
                if (result.getAuthenticationType()
                    == BiometricPrompt.AUTHENTICATION_RESULT_TYPE_UNKNOWN
                ) {
                    result = AuthenticationResult(
                        result.getCryptoObject(),
                        mViewModelRef.get()!!.inferredAuthenticationResultType
                    )
                }
                mViewModelRef.get()!!.setAuthenticationResult(result)
            }
        }

        override fun onError(errorCode: Int, errorMessage: CharSequence?) {
            if (mViewModelRef.get() != null && !mViewModelRef.get()!!.isConfirmingDeviceCredential
                && mViewModelRef.get()!!.isAwaitingResult
            ) {
                mViewModelRef.get()!!.setAuthenticationError(
                    BiometricErrorData(errorCode, errorMessage)
                )
            }
        }

        override fun onHelp(helpMessage: CharSequence?) {
            if (mViewModelRef.get() != null) {
                mViewModelRef.get()!!.setAuthenticationHelpMessage(helpMessage)
            }
        }

        override fun onFailure() {
            if (mViewModelRef.get() != null && mViewModelRef.get()!!.isAwaitingResult) {
                mViewModelRef.get()!!.setAuthenticationFailurePending(true)
            }
        }
    }

    /**
     * The dialog listener that is returned by [.getNegativeButtonListener].
     */
    private class NegativeButtonListener internal constructor(viewModel: BiometricViewModel?) : DialogInterface.OnClickListener {
        private val mViewModelRef: WeakReference<BiometricViewModel?>

        /**
         * Creates a negative button listener with a weak reference to the given view model.
         *
         * @param viewModel The view model instance to hold a weak reference to.
         */
        init {
            mViewModelRef = WeakReference(viewModel)
        }

        override fun onClick(dialogInterface: DialogInterface, which: Int) {
            if (mViewModelRef.get() != null) {
                mViewModelRef.get()!!.setNegativeButtonPressPending(true)
            }
        }
    }

    /**
     * The executor that will run authentication callback methods.
     *
     *
     * If unset, callbacks are invoked on the main thread with [Looper.getMainLooper].
     */
    private var mClientExecutor: Executor? = null

    /**
     * The callback object that will receive authentication events.
     */
    private var mClientCallback: AuthenticationCallback? = null

    /**
     * Info about the appearance and behavior of the prompt provided by the client application.
     */
    private var mPromptInfo: PromptInfo? = null

    /**
     * The crypto object associated with the current authentication session.
     */
    var cryptoObject: CryptoObject? = null

    /**
     * A provider for cross-platform compatible authentication callbacks.
     */
    private var mAuthenticationCallbackProvider: AuthenticationCallbackProvider? = null

    /**
     * A provider for cross-platform compatible cancellation signal objects.
     */
    private var mCancellationSignalProvider: CancellationSignalProvider? = null

    /**
     * A dialog listener for the negative button shown on the prompt.
     */
    private var mNegativeButtonListener: DialogInterface.OnClickListener? = null

    /**
     * A label for the negative button shown on the prompt.
     *
     *
     * If set, this value overrides the one returned by
     * [PromptInfo.getNegativeButtonText].
     */
    private var mNegativeButtonTextOverride: CharSequence? = null

    /**
     * An integer indicating where the dialog was last canceled from.
     */
    @CanceledFrom
    var canceledFrom = BiometricFragment.CANCELED_FROM_INTERNAL

    /**
     * Whether the prompt is currently showing.
     */
    var isPromptShowing = false

    /**
     * Whether the client callback is awaiting an authentication result.
     */
    var isAwaitingResult = false

    /**
     * Whether the user is currently authenticating with their PIN, pattern, or password.
     */
    var isConfirmingDeviceCredential = false

    /**
     * Whether the prompt should delay showing the authentication UI.
     */
    var isDelayingPrompt = false

    /**
     * Whether the prompt should ignore cancel requests not initiated by the client.
     */
    var isIgnoringCancel = false

    /**
     * Information associated with a successful authentication attempt.
     */
    private var mAuthenticationResult: MutableLiveData<AuthenticationResult?>? = null

    /**
     * Information associated with an unrecoverable authentication error.
     */
    private var mAuthenticationError: MutableLiveData<BiometricErrorData?>? = null

    /**
     * A human-readable message describing a recoverable authentication error or event.
     */
    private var mAuthenticationHelpMessage: MutableLiveData<CharSequence?>? = null

    /**
     * Whether an unrecognized biometric has been presented.
     */
    private var mIsAuthenticationFailurePending: MutableLiveData<Boolean>? = null

    /**
     * Whether the user has pressed the negative button on the prompt.
     */
    private var mIsNegativeButtonPressPending: MutableLiveData<Boolean>? = null

    /**
     * Whether the fingerprint dialog should always be dismissed instantly.
     */
    var isFingerprintDialogDismissedInstantly = true

    /**
     * Whether the user has manually canceled out of the fingerprint dialog.
     */
    private var mIsFingerprintDialogCancelPending: MutableLiveData<Boolean>? = null

    /**
     * The previous state of the fingerprint dialog UI.
     */
    @get:FingerprintDialogFragment.State
    @FingerprintDialogFragment.State
    var fingerprintDialogPreviousState = FingerprintDialogFragment.STATE_NONE

    /**
     * The current state of the fingerprint dialog UI.
     */
    private var mFingerprintDialogState: MutableLiveData<Int>? = null

    /**
     * A human-readable message to be displayed below the icon on the fingerprint dialog.
     */
    private var mFingerprintDialogHelpMessage: MutableLiveData<CharSequence>? = null
    var clientExecutor: Executor
        get() = if (mClientExecutor != null) mClientExecutor!! else DefaultExecutor()
        set(clientExecutor) {
            mClientExecutor = clientExecutor
        }
    var clientCallback: AuthenticationCallback
        get() {
            if (mClientCallback == null) {
                mClientCallback = object : AuthenticationCallback() {}
            }
            return mClientCallback!!
        }
        set(clientCallback) {
            mClientCallback = clientCallback
        }

    /**
     * Clears the client callback reference held by this view model.
     */
    fun resetClientCallback() {
        mClientCallback = null
    }

    fun setPromptInfo(promptInfo: PromptInfo?) {
        mPromptInfo = promptInfo
    }

    /**
     * Gets the title to be shown on the biometric prompt.
     *
     *
     * This method relies on the [PromptInfo] set by
     * [.setPromptInfo].
     *
     * @return The title for the prompt, or `null` if not set.
     */
    val title: CharSequence?
        get() = if (mPromptInfo != null) mPromptInfo!!.getTitle() else null

    /**
     * Gets the subtitle to be shown on the biometric prompt.
     *
     *
     * This method relies on the [PromptInfo] set by
     * [.setPromptInfo].
     *
     * @return The subtitle for the prompt, or `null` if not set.
     */
    val subtitle: CharSequence?
        get() = if (mPromptInfo != null) mPromptInfo!!.getSubtitle() else null

    /**
     * Gets the description to be shown on the biometric prompt.
     *
     *
     * This method relies on the [PromptInfo] set by
     * [.setPromptInfo].
     *
     * @return The description for the prompt, or `null` if not set.
     */
    val description: CharSequence?
        get() = if (mPromptInfo != null) mPromptInfo!!.getDescription() else null

    /**
     * Gets the text that should be shown for the negative button on the biometric prompt.
     *
     *
     * If non-null, the value set by [.setNegativeButtonTextOverride] is
     * used. Otherwise, falls back to the value returned by
     * [PromptInfo.getNegativeButtonText], or `null` if a non-null
     * [PromptInfo] has not been set by
     * [.setPromptInfo].
     *
     * @return The negative button text for the prompt, or `null` if not set.
     */
    val negativeButtonText: CharSequence?
        get() = if (mNegativeButtonTextOverride != null) {
            mNegativeButtonTextOverride
        } else if (mPromptInfo != null) {
            mPromptInfo!!.getNegativeButtonText()
        } else {
            null
        }

    /**
     * Checks if the confirmation required option is enabled for the biometric prompt.
     *
     *
     * This method relies on the [PromptInfo] set by
     * [.setPromptInfo].
     *
     * @return Whether the confirmation required option is enabled.
     */
    val isConfirmationRequired: Boolean
        get() = mPromptInfo == null || mPromptInfo!!.isConfirmationRequired()

    /**
     * Gets the type(s) of authenticators that may be invoked by the biometric prompt.
     *
     *
     * If a non-null [PromptInfo] has been set by
     * [.setPromptInfo], this is the single consolidated set of
     * authenticators allowed by the prompt, taking into account the values of
     * [PromptInfo.getAllowedAuthenticators],
     * [PromptInfo.isDeviceCredentialAllowed], and
     * [.getCryptoObject].
     *
     * @return A bit field representing all valid authenticator types that may be invoked by
     * the prompt, or 0 if not set.
     */
    @get:AuthenticatorTypes
    val allowedAuthenticators: Int
        get() = if (mPromptInfo != null) getConsolidatedAuthenticators(mPromptInfo!!, cryptoObject) else 0
    val authenticationCallbackProvider: AuthenticationCallbackProvider
        get() {
            if (mAuthenticationCallbackProvider == null) {
                mAuthenticationCallbackProvider = AuthenticationCallbackProvider(CallbackListener(this))
            }
            return mAuthenticationCallbackProvider!!
        }
    val cancellationSignalProvider: CancellationSignalProvider
        get() {
            if (mCancellationSignalProvider == null) {
                mCancellationSignalProvider = CancellationSignalProvider()
            }
            return mCancellationSignalProvider!!
        }
    val negativeButtonListener: DialogInterface.OnClickListener
        get() {
            if (mNegativeButtonListener == null) {
                mNegativeButtonListener = NegativeButtonListener(this)
            }
            return mNegativeButtonListener!!
        }

    fun setNegativeButtonTextOverride(negativeButtonTextOverride: CharSequence?) {
        mNegativeButtonTextOverride = negativeButtonTextOverride
    }

    val authenticationResult: LiveData<AuthenticationResult?>
        get() {
            if (mAuthenticationResult == null) {
                mAuthenticationResult = MutableLiveData()
            }
            return mAuthenticationResult!!
        }

    fun setAuthenticationResult(
        authenticationResult: AuthenticationResult?
    ) {
        if (mAuthenticationResult == null) {
            mAuthenticationResult = MutableLiveData()
        }
        updateValue(mAuthenticationResult!!, authenticationResult)
    }

    val authenticationError: MutableLiveData<BiometricErrorData?>
        get() {
            if (mAuthenticationError == null) {
                mAuthenticationError = MutableLiveData()
            }
            return mAuthenticationError!!
        }

    fun setAuthenticationError(authenticationError: BiometricErrorData?) {
        if (mAuthenticationError == null) {
            mAuthenticationError = MutableLiveData()
        }
        updateValue(mAuthenticationError!!, authenticationError)
    }

    val authenticationHelpMessage: LiveData<CharSequence?>
        get() {
            if (mAuthenticationHelpMessage == null) {
                mAuthenticationHelpMessage = MutableLiveData()
            }
            return mAuthenticationHelpMessage!!
        }

    fun setAuthenticationHelpMessage(
        authenticationHelpMessage: CharSequence?
    ) {
        if (mAuthenticationHelpMessage == null) {
            mAuthenticationHelpMessage = MutableLiveData()
        }
        updateValue(mAuthenticationHelpMessage!!, authenticationHelpMessage)
    }

    val isAuthenticationFailurePending: LiveData<Boolean>
        get() {
            if (mIsAuthenticationFailurePending == null) {
                mIsAuthenticationFailurePending = MutableLiveData()
            }
            return mIsAuthenticationFailurePending!!
        }

    fun setAuthenticationFailurePending(authenticationFailurePending: Boolean) {
        if (mIsAuthenticationFailurePending == null) {
            mIsAuthenticationFailurePending = MutableLiveData()
        }
        updateValue(mIsAuthenticationFailurePending!!, authenticationFailurePending)
    }

    val isNegativeButtonPressPending: LiveData<Boolean>
        get() {
            if (mIsNegativeButtonPressPending == null) {
                mIsNegativeButtonPressPending = MutableLiveData()
            }
            return mIsNegativeButtonPressPending!!
        }

    fun setNegativeButtonPressPending(negativeButtonPressPending: Boolean) {
        if (mIsNegativeButtonPressPending == null) {
            mIsNegativeButtonPressPending = MutableLiveData()
        }
        updateValue(mIsNegativeButtonPressPending!!, negativeButtonPressPending)
    }

    val isFingerprintDialogCancelPending: LiveData<Boolean>
        get() {
            if (mIsFingerprintDialogCancelPending == null) {
                mIsFingerprintDialogCancelPending = MutableLiveData()
            }
            return mIsFingerprintDialogCancelPending!!
        }

    fun setFingerprintDialogCancelPending(fingerprintDialogCancelPending: Boolean) {
        if (mIsFingerprintDialogCancelPending == null) {
            mIsFingerprintDialogCancelPending = MutableLiveData()
        }
        updateValue(mIsFingerprintDialogCancelPending!!, fingerprintDialogCancelPending)
    }

    val fingerprintDialogState: LiveData<Int>
        get() {
            if (mFingerprintDialogState == null) {
                mFingerprintDialogState = MutableLiveData()
            }
            return mFingerprintDialogState!!
        }

    fun setFingerprintDialogState(
        @FingerprintDialogFragment.State fingerprintDialogState: Int
    ) {
        if (mFingerprintDialogState == null) {
            mFingerprintDialogState = MutableLiveData()
        }
        updateValue(mFingerprintDialogState!!, fingerprintDialogState)
    }

    val fingerprintDialogHelpMessage: LiveData<CharSequence>
        get() {
            if (mFingerprintDialogHelpMessage == null) {
                mFingerprintDialogHelpMessage = MutableLiveData()
            }
            return mFingerprintDialogHelpMessage!!
        }

    fun setFingerprintDialogHelpMessage(
        fingerprintDialogHelpMessage: CharSequence
    ) {
        if (mFingerprintDialogHelpMessage == null) {
            mFingerprintDialogHelpMessage = MutableLiveData()
        }
        updateValue(mFingerprintDialogHelpMessage!!, fingerprintDialogHelpMessage)
    }

    /**
     * Attempts to infer the type of authenticator that was used to authenticate the user.
     *
     * @return The inferred authentication type, or
     * [BiometricPrompt.AUTHENTICATION_RESULT_TYPE_UNKNOWN] if unknown.
     */
    /* synthetic access */
    @get:AuthenticationResultType
    val inferredAuthenticationResultType: Int
        get() {
            @AuthenticatorTypes val authenticators = allowedAuthenticators
            return if (isSomeBiometricAllowed(authenticators)
                && !isDeviceCredentialAllowed(authenticators)
            ) {
                BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC
            } else BiometricPrompt.AUTHENTICATION_RESULT_TYPE_UNKNOWN
        }

    companion object {
        /**
         * Ensures the value of a given mutable live data object is updated on the main thread.
         *
         * @param liveData The mutable live data object whose value should be updated.
         * @param value    The new value to be set for the mutable live data object.
         */
        private fun <T> updateValue(liveData: MutableLiveData<T>, value: T) {
            if (Thread.currentThread() === Looper.getMainLooper().thread) {
                liveData.setValue(value)
            } else {
                liveData.postValue(value)
            }
        }
    }
}