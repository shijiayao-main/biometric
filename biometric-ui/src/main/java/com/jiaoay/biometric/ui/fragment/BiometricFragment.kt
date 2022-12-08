package com.jiaoay.biometric.ui.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.jiaoay.biometric.authentication.AuthenticationResult
import com.jiaoay.biometric.crypto.CryptoObject
import com.jiaoay.biometric.crypto.CryptoObjectUtils.createFakeCryptoObject
import com.jiaoay.biometric.crypto.CryptoObjectUtils.wrapForBiometricPrompt
import com.jiaoay.biometric.crypto.CryptoObjectUtils.wrapForFingerprintManager
import com.jiaoay.biometric.keyguard.KeyguardUtils.getKeyguardManager
import com.jiaoay.biometric.keyguard.KeyguardUtils.isDeviceSecuredWithCredential
import com.jiaoay.biometric.manager.AuthenticatorTypes
import com.jiaoay.biometric.manager.Authenticators
import com.jiaoay.biometric.manager.BiometricManager
import com.jiaoay.biometric.manager.BiometricManager.Companion.from
import com.jiaoay.biometric.pack.PackageUtils.hasSystemFeatureFingerprint
import com.jiaoay.biometric.ui.BiometricViewModel
import com.jiaoay.biometric.ui.ErrorUtils.getFingerprintErrorString
import com.jiaoay.biometric.ui.ErrorUtils.isKnownError
import com.jiaoay.biometric.ui.ErrorUtils.isLockoutError
import com.jiaoay.biometric.ui.fingerprint.FingerprintDialogFragment
import com.jiaoay.biometric.ui.fragment.BiometricFragmentApi21Impl.createConfirmDeviceCredentialIntent
import com.jiaoay.biometric.util.AuthenticatorUtils
import com.jiaoay.biometric.util.BiometricUtil
import com.jiaoay.biometric.util.PromptInfo
import com.jiaoay.biometric_ui.R
import java.lang.ref.WeakReference
import java.util.concurrent.Executor

/**
 * A fragment that hosts the system-dependent UI for [BiometricPrompt] and coordinates logic
 * for the ongoing authentication session across device configuration changes.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class BiometricFragment : Fragment() {
    /**
     * Where authentication was canceled from.
     */
    @IntDef(CANCELED_FROM_INTERNAL, CANCELED_FROM_USER, CANCELED_FROM_NEGATIVE_BUTTON, CANCELED_FROM_CLIENT)
    @Retention(AnnotationRetention.SOURCE)
    internal annotation class CanceledFrom

    /**
     * An executor used by [android.hardware.biometrics.BiometricPrompt] to run framework
     * code.
     */
    private class PromptExecutor() : Executor {
        private val mPromptHandler = Handler(Looper.getMainLooper())
        override fun execute(runnable: Runnable) {
            mPromptHandler.post(runnable)
        }
    }

    /**
     * A runnable with a weak reference to this fragment that can be used to invoke
     * [.showPromptForAuthentication].
     */
    private class ShowPromptForAuthenticationRunnable(fragment: BiometricFragment?) : Runnable {
        private val mFragmentRef: WeakReference<BiometricFragment?>

        init {
            mFragmentRef = WeakReference(fragment)
        }

        override fun run() {
            if (mFragmentRef.get() != null) {
                mFragmentRef.get()!!.showPromptForAuthentication()
            }
        }
    }

    /**
     * A handler used to post delayed events.
     */
    @VisibleForTesting
    var mHandler = Handler(Looper.getMainLooper())

    /**
     * The view model for the ongoing authentication session.
     */
    @VisibleForTesting
    val mViewModel by activityViewModels<BiometricViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        connectViewModel()
    }

    override fun onStart() {
        super.onStart()

        // Some device credential implementations in API 29 cause the prompt to receive a cancel
        // signal immediately after it's shown (b/162022588).
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q
            && AuthenticatorUtils.isDeviceCredentialAllowed(
                mViewModel.allowedAuthenticators
            )
        ) {
            mViewModel.isIgnoringCancel = true
            mHandler.postDelayed(StopIgnoringCancelRunnable(mViewModel), 250L)
        }
    }

    override fun onStop() {
        super.onStop()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !mViewModel.isConfirmingDeviceCredential
            && !isChangingConfigurations
        ) {
            cancelAuthentication(CANCELED_FROM_INTERNAL)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CONFIRM_CREDENTIAL) {
            mViewModel.isConfirmingDeviceCredential = false
            handleConfirmCredentialResult(resultCode)
        }
    }

    /**
     * Connects the [BiometricViewModel] for the ongoing authentication session to this
     * fragment.
     */
    private fun connectViewModel() {
        activity ?: return
        mViewModel.authenticationResult.observe(
            this
        ) { authenticationResult: AuthenticationResult? ->
            if (authenticationResult != null) {
                onAuthenticationSucceeded(authenticationResult)
                mViewModel.setAuthenticationResult(null)
            }
        }
        mViewModel.authenticationError.observe(
            this
        ) { authenticationError ->
            if (authenticationError != null) {
                onAuthenticationError(
                    authenticationError.errorCode,
                    authenticationError.errorMessage
                )
                mViewModel.setAuthenticationError(null)
            }
        }

        mViewModel.authenticationHelpMessage.observe(
            this
        ) { authenticationHelpMessage ->
            if (authenticationHelpMessage != null) {
                onAuthenticationHelp(authenticationHelpMessage)
                mViewModel.setAuthenticationError(null)
            }
        }

        mViewModel.isAuthenticationFailurePending.observe(
            this
        ) { authenticationFailurePending ->
            if (authenticationFailurePending) {
                onAuthenticationFailed()
                mViewModel.setAuthenticationFailurePending(false)
            }
        }

        mViewModel.isNegativeButtonPressPending.observe(
            this
        ) { negativeButtonPressPending ->
            if (negativeButtonPressPending) {
                if (isManagingDeviceCredentialButton) {
                    onDeviceCredentialButtonPressed()
                } else {
                    onCancelButtonPressed()
                }
                mViewModel.setNegativeButtonPressPending(false)
            }
        }

        mViewModel.isFingerprintDialogCancelPending.observe(
            this
        ) { fingerprintDialogCancelPending ->
            if (fingerprintDialogCancelPending) {
                cancelAuthentication(CANCELED_FROM_USER)
                dismiss()
                mViewModel.setFingerprintDialogCancelPending(false)
            }
        }
    }

    /**
     * Shows the prompt UI to the user and begins an authentication session.
     *
     * @param info   An object describing the appearance and behavior of the prompt.
     * @param crypto A crypto object to be associated with this authentication.
     */
    fun authenticate(
        info: PromptInfo,
        crypto: CryptoObject?
    ) {
        val activity = activity
        if (activity == null) {
            Log.e(TAG, "Not launching prompt. Client activity was null.")
            return
        }
        mViewModel.setPromptInfo(info)

        // Use a fake crypto object to force Strong biometric auth prior to Android 11 (API 30).
        @AuthenticatorTypes val authenticators = AuthenticatorUtils.getConsolidatedAuthenticators(info, crypto)
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.R &&
            authenticators == Authenticators.BIOMETRIC_STRONG &&
            crypto == null
        ) {
            mViewModel.cryptoObject = createFakeCryptoObject()
        } else {
            mViewModel.cryptoObject = crypto
        }
        if (isManagingDeviceCredentialButton) {
            mViewModel.setNegativeButtonTextOverride(
                getString(R.string.confirm_device_credential_password)
            )
        } else {
            // Don't override the negative button text from the client.
            mViewModel.setNegativeButtonTextOverride(null)
        }

        // Fall back to device credential immediately if no known biometrics are available.
        if (isManagingDeviceCredentialButton && from(activity).canAuthenticate(Authenticators.BIOMETRIC_WEAK) != BiometricManager.BIOMETRIC_SUCCESS) {
            mViewModel.isAwaitingResult = true
            launchConfirmCredentialActivity()
            return
        }

        // Check if we should delay showing the authentication prompt.
        if (mViewModel.isDelayingPrompt) {
            mHandler.postDelayed(
                ShowPromptForAuthenticationRunnable(this), SHOW_PROMPT_DELAY_MS.toLong()
            )
        } else {
            showPromptForAuthentication()
        }
    }

    /**
     * Shows either the framework biometric prompt or fingerprint UI dialog to the user and begins
     * authentication.
     */
    fun showPromptForAuthentication() {
        if (!mViewModel.isPromptShowing) {
            if (context == null) {
                Log.w(TAG, "Not showing biometric prompt. Context is null.")
                return
            }
            mViewModel.isPromptShowing = true
            mViewModel.isAwaitingResult = true
            if (isUsingFingerprintDialog) {
                showFingerprintDialogForAuthentication()
            } else {
                showBiometricPromptForAuthentication()
            }
        }
    }

    /**
     * Shows the fingerprint dialog UI to the user and begins authentication.
     */
    private fun showFingerprintDialogForAuthentication() {
        val context = requireContext().applicationContext
        val fingerprintManagerCompat = FingerprintManagerCompat.from(context)
        val errorCode = checkForFingerprintPreAuthenticationErrors(fingerprintManagerCompat)
        if (errorCode != com.jiaoay.biometric.util.BiometricPrompt.BIOMETRIC_SUCCESS) {
            sendErrorAndDismiss(
                errorCode, getFingerprintErrorString(context, errorCode)
            )
            return
        }
        if (isAdded) {
            mViewModel.isFingerprintDialogDismissedInstantly = true
            if (!BiometricUtil.shouldHideFingerprintDialog(context, Build.MODEL)) {
                mHandler.postDelayed(
                    {
                        mViewModel.isFingerprintDialogDismissedInstantly = false
                    },
                    DISMISS_INSTANTLY_DELAY_MS.toLong()
                )
                val dialog = FingerprintDialogFragment.newInstance()
                dialog.show(parentFragmentManager, FINGERPRINT_DIALOG_FRAGMENT_TAG)
            }
            mViewModel.canceledFrom = CANCELED_FROM_INTERNAL
            authenticateWithFingerprint(fingerprintManagerCompat, context)
        }
    }

    /**
     * Shows the framework [android.hardware.biometrics.BiometricPrompt] UI to the user and
     * begins authentication.
     */
    @RequiresApi(Build.VERSION_CODES.P)
    private fun showBiometricPromptForAuthentication() {
        val builder = BiometricFragmentApi28Impl.createPromptBuilder(requireContext().applicationContext)
        val title = mViewModel.title
        val subtitle = mViewModel.subtitle
        val description = mViewModel.description
        if (title != null) {
            BiometricFragmentApi28Impl.setTitle(builder, title)
        }
        if (subtitle != null) {
            BiometricFragmentApi28Impl.setSubtitle(builder, subtitle)
        }
        if (description != null) {
            BiometricFragmentApi28Impl.setDescription(builder, description)
        }
        val negativeButtonText = mViewModel.negativeButtonText
        if (!TextUtils.isEmpty(negativeButtonText)) {
            BiometricFragmentApi28Impl.setNegativeButton(
                builder,
                negativeButtonText!!,
                mViewModel.clientExecutor,
                mViewModel.negativeButtonListener
            )
        }

        // Set the confirmation required option introduced in Android 10 (API 29).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            BiometricFragmentApi29Impl.setConfirmationRequired(builder, mViewModel.isConfirmationRequired)
        }

        // Set or emulate the allowed authenticators option introduced in Android 11 (API 30).
        @AuthenticatorTypes
        val authenticators = mViewModel.allowedAuthenticators
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricFragmentApi30Impl.setAllowedAuthenticators(builder, authenticators)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            BiometricFragmentApi29Impl.setDeviceCredentialAllowed(
                builder,
                AuthenticatorUtils.isDeviceCredentialAllowed(authenticators)
            )
        }
        authenticateWithBiometricPrompt(BiometricFragmentApi28Impl.buildPrompt(builder), context)
    }

    /**
     * Requests user authentication with the given fingerprint manager.
     *
     * @param fingerprintManager The fingerprint manager that will be used for authentication.
     * @param context            The application context.
     */
    @VisibleForTesting
    fun authenticateWithFingerprint(
        fingerprintManager: FingerprintManagerCompat,
        context: Context
    ) {
        val crypto = wrapForFingerprintManager(mViewModel.cryptoObject)
        val cancellationSignal = mViewModel.cancellationSignalProvider.fingerprintCancellationSignal
        val callback = mViewModel.authenticationCallbackProvider
            .fingerprintCallback
        try {
            fingerprintManager.authenticate(
                crypto, 0, cancellationSignal, callback, null /* handler */
            )
        } catch (e: NullPointerException) {
            // Catch and handle NPE if thrown by framework call to authenticate() (b/151316421).
            Log.e(TAG, "Got NPE while authenticating with fingerprint.", e)
            val errorCode = com.jiaoay.biometric.util.BiometricPrompt.ERROR_HW_UNAVAILABLE
            sendErrorAndDismiss(
                errorCode, getFingerprintErrorString(context, errorCode)
            )
        }
    }

    /**
     * Requests user authentication with the given framework biometric prompt.
     *
     * @param biometricPrompt The biometric prompt that will be used for authentication.
     * @param context         An application or activity context.
     */
    @RequiresApi(Build.VERSION_CODES.P)
    @VisibleForTesting
    fun authenticateWithBiometricPrompt(
        biometricPrompt: BiometricPrompt,
        context: Context?
    ) {
        val cryptoObject = wrapForBiometricPrompt(mViewModel.cryptoObject)
        val cancellationSignal = mViewModel.cancellationSignalProvider.biometricCancellationSignal
        val executor: Executor = PromptExecutor()
        val callback = mViewModel.authenticationCallbackProvider.biometricCallback
        try {
            if (cryptoObject == null) {
                BiometricFragmentApi28Impl.authenticate(biometricPrompt, cancellationSignal, executor, callback)
            } else {
                BiometricFragmentApi28Impl.authenticate(
                    biometricPrompt, cryptoObject, cancellationSignal, executor, callback
                )
            }
        } catch (e: NullPointerException) {
            // Catch and handle NPE if thrown by framework call to authenticate() (b/151316421).
            Log.e(TAG, "Got NPE while authenticating with biometric prompt.", e)
            val errorCode = com.jiaoay.biometric.util.BiometricPrompt.ERROR_HW_UNAVAILABLE
            val errorString = context?.getString(R.string.default_error_msg) ?: ""
            sendErrorAndDismiss(errorCode, errorString)
        }
    }

    /**
     * Cancels the ongoing authentication session and sends an error to the client callback.
     *
     * @param canceledFrom Where authentication was canceled from.
     */
    fun cancelAuthentication(@CanceledFrom canceledFrom: Int) {
        if (canceledFrom != CANCELED_FROM_CLIENT && mViewModel.isIgnoringCancel) {
            return
        }
        if (isUsingFingerprintDialog) {
            mViewModel.canceledFrom = canceledFrom
            if (canceledFrom == CANCELED_FROM_USER) {
                val errorCode = com.jiaoay.biometric.util.BiometricPrompt.ERROR_USER_CANCELED
                sendErrorToClient(
                    errorCode, getFingerprintErrorString(context, errorCode)
                )
            }
        }
        mViewModel.cancellationSignalProvider.cancel()
    }

    /**
     * Removes this fragment and any associated UI from the client activity/fragment.
     */
    fun dismiss() {
        mViewModel.isPromptShowing = false
        dismissFingerprintDialog()
        if (!mViewModel.isConfirmingDeviceCredential && isAdded) {
            parentFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
        }

        // Wait before showing again to work around a dismissal logic issue on API 29 (b/157783075).
        val context = context
        if (context != null && BiometricUtil.shouldDelayShowingPrompt(context, Build.MODEL)) {
            mViewModel.isDelayingPrompt = true
            mHandler.postDelayed(StopDelayingPromptRunnable(mViewModel), SHOW_PROMPT_DELAY_MS.toLong())
        }
    }

    /**
     * Removes the fingerprint dialog UI from the client activity/fragment.
     */
    private fun dismissFingerprintDialog() {
        mViewModel.isPromptShowing = false
        if (isAdded) {
            val fragmentManager = parentFragmentManager
            val fingerprintDialog = fragmentManager.findFragmentByTag(
                FINGERPRINT_DIALOG_FRAGMENT_TAG
            ) as FingerprintDialogFragment?
            if (fingerprintDialog != null) {
                if (fingerprintDialog.isAdded) {
                    fingerprintDialog.dismissAllowingStateLoss()
                } else {
                    fragmentManager.beginTransaction().remove(fingerprintDialog)
                        .commitAllowingStateLoss()
                }
            }
        }
    }

    /**
     * Callback that is run when the view model receives a successful authentication result.
     *
     * @param result An object containing authentication-related data.
     */
    @VisibleForTesting
    fun onAuthenticationSucceeded(result: AuthenticationResult) {
        sendSuccessAndDismiss(result)
    }

    /**
     * Callback that is run when the view model receives an unrecoverable error result.
     *
     * @param errorCode    An integer ID associated with the error.
     * @param errorMessage A human-readable string that describes the error.
     */
    @VisibleForTesting
    fun onAuthenticationError(errorCode: Int, errorMessage: CharSequence?) {
        // Ensure we're only sending publicly defined errors.
        val knownErrorCode = if (isKnownError(errorCode)) errorCode else com.jiaoay.biometric.util.BiometricPrompt.ERROR_VENDOR
        val context = context
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && isLockoutError(knownErrorCode) && context != null && isDeviceSecuredWithCredential(context) && AuthenticatorUtils.isDeviceCredentialAllowed(
                mViewModel.allowedAuthenticators
            )
        ) {
            launchConfirmCredentialActivity()
            return
        }
        if (isUsingFingerprintDialog) {
            // Avoid passing a null error string to the client callback.
            val errorString = errorMessage ?: getFingerprintErrorString(getContext(), knownErrorCode)
            if (knownErrorCode == com.jiaoay.biometric.util.BiometricPrompt.ERROR_CANCELED) {
                // User-initiated cancellation errors should already be handled.
                @CanceledFrom val canceledFrom = mViewModel.canceledFrom
                if (canceledFrom == CANCELED_FROM_INTERNAL
                    || canceledFrom == CANCELED_FROM_CLIENT
                ) {
                    sendErrorToClient(knownErrorCode, errorString)
                }
                dismiss()
            } else {
                if (mViewModel.isFingerprintDialogDismissedInstantly) {
                    sendErrorAndDismiss(knownErrorCode, errorString)
                } else {
                    showFingerprintErrorMessage(errorString)
                    mHandler.postDelayed(
                        { sendErrorAndDismiss(knownErrorCode, errorString) },
                        dismissDialogDelay.toLong()
                    )
                }

                // Always set this to true. In case the user tries to authenticate again
                // the UI will not be shown.
                mViewModel.isFingerprintDialogDismissedInstantly = true
            }
        } else {
            val errorString = errorMessage ?: (getString(R.string.default_error_msg) + " " + knownErrorCode)
            sendErrorAndDismiss(knownErrorCode, errorString)
        }
    }

    /**
     * Callback that is run when the view model receives a recoverable error or help message.
     *
     * @param helpMessage A human-readable error/help message.
     */
    fun onAuthenticationHelp(helpMessage: CharSequence) {
        if (isUsingFingerprintDialog) {
            showFingerprintErrorMessage(helpMessage)
        }
    }

    /**
     * Callback that is run when the view model reports a failed authentication attempt.
     */
    fun onAuthenticationFailed() {
        if (isUsingFingerprintDialog) {
            showFingerprintErrorMessage(getString(R.string.fingerprint_not_recognized))
        }
        sendFailureToClient()
    }

    /**
     * Callback that is run when the view model reports that the device credential fallback
     * button has been pressed on the prompt.
     */
    fun onDeviceCredentialButtonPressed() {
        launchConfirmCredentialActivity()
    }

    /**
     * Callback that is run when the view model reports that the cancel button has been pressed on
     * the prompt.
     */
    fun onCancelButtonPressed() {
        val negativeButtonText = mViewModel.negativeButtonText
        sendErrorAndDismiss(
            com.jiaoay.biometric.util.BiometricPrompt.ERROR_NEGATIVE_BUTTON,
            negativeButtonText ?: getString(R.string.default_error_msg)
        )
        cancelAuthentication(CANCELED_FROM_NEGATIVE_BUTTON)
    }

    /**
     * Launches the confirm device credential Settings activity, where the user can authenticate
     * using their PIN, pattern, or password.
     */
    private fun launchConfirmCredentialActivity() {
        val activity = activity
        if (activity == null) {
            Log.e(TAG, "Failed to check device credential. Client FragmentActivity not found.")
            return
        }

        // Get the KeyguardManager service in whichever way the platform supports.
        val keyguardManager = getKeyguardManager(activity)
        if (keyguardManager == null) {
            sendErrorAndDismiss(
                com.jiaoay.biometric.util.BiometricPrompt.ERROR_HW_NOT_PRESENT,
                getString(R.string.generic_error_no_keyguard)
            )
            return
        }

        // Pass along the title and subtitle/description from the biometric prompt.
        val title = mViewModel.title
        val subtitle = mViewModel.subtitle
        val description = mViewModel.description
        val credentialDescription = subtitle ?: description!!
        val intent = createConfirmDeviceCredentialIntent(
            keyguardManager, title, credentialDescription
        )

        // A null intent from KeyguardManager means that the device is not secure.
        if (intent == null) {
            sendErrorAndDismiss(
                com.jiaoay.biometric.util.BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
                getString(R.string.generic_error_no_device_credential)
            )
            return
        }
        mViewModel.isConfirmingDeviceCredential = true

        // Dismiss the fingerprint dialog before launching the activity.
        if (isUsingFingerprintDialog) {
            dismissFingerprintDialog()
        }

        // Launch a new instance of the confirm device credential Settings activity.
        intent.flags = Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
        startActivityForResult(intent, REQUEST_CONFIRM_CREDENTIAL)
    }

    /**
     * Processes the result returned by the confirm device credential Settings activity.
     *
     * @param resultCode The result code from the Settings activity.
     */
    private fun handleConfirmCredentialResult(resultCode: Int) {
        if (resultCode == Activity.RESULT_OK) {
            // Device credential auth succeeded. This is incompatible with crypto for API <30.
            sendSuccessAndDismiss(
                AuthenticationResult(
                    null /* crypto */,
                    com.jiaoay.biometric.util.BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL
                )
            )
        } else {
            // Device credential auth failed. Assume this is due to the user canceling.
            sendErrorAndDismiss(
                com.jiaoay.biometric.util.BiometricPrompt.ERROR_USER_CANCELED,
                getString(R.string.generic_error_user_canceled)
            )
        }
    }

    /**
     * Updates the fingerprint dialog to show an error message to the user.
     *
     * @param errorMessage The error message to show on the dialog.
     */
    private fun showFingerprintErrorMessage(errorMessage: CharSequence?) {
        val helpMessage = errorMessage ?: getString(R.string.default_error_msg)
        mViewModel.setFingerprintDialogState(FingerprintDialogFragment.STATE_FINGERPRINT_ERROR)
        mViewModel.setFingerprintDialogHelpMessage(helpMessage)
    }

    /**
     * Sends a successful authentication result to the client and dismisses the prompt.
     *
     * @param result An object containing authentication-related data.
     * @see .sendSuccessToClient
     */
    private fun sendSuccessAndDismiss(result: AuthenticationResult) {
        sendSuccessToClient(result)
        dismiss()
    }

    /**
     * Sends an unrecoverable error result to the client and dismisses the prompt.
     *
     * @param errorCode   An integer ID associated with the error.
     * @param errorString A human-readable string that describes the error.
     * @see .sendErrorToClient
     */
    fun sendErrorAndDismiss(errorCode: Int, errorString: CharSequence) {
        sendErrorToClient(errorCode, errorString)
        dismiss()
    }

    /**
     * Sends a successful authentication result to the client callback.
     *
     * @param result An object containing authentication-related data.
     * @see .sendSuccessAndDismiss
     * @see AuthenticationCallback.onAuthenticationSucceeded
     */
    private fun sendSuccessToClient(result: AuthenticationResult) {
        if (!mViewModel.isAwaitingResult) {
            Log.w(TAG, "Success not sent to client. Client is not awaiting a result.")
            return
        }
        mViewModel.isAwaitingResult = false
        mViewModel.clientExecutor.execute {
            mViewModel.clientCallback.onAuthenticationSucceeded(result)
        }
    }

    /**
     * Sends an unrecoverable error result to the client callback.
     *
     * @param errorCode   An integer ID associated with the error.
     * @param errorString A human-readable string that describes the error.
     * @see .sendErrorAndDismiss
     * @see AuthenticationCallback.onAuthenticationError
     */
    private fun sendErrorToClient(errorCode: Int, errorString: CharSequence) {
        if (mViewModel.isConfirmingDeviceCredential) {
            Log.v(TAG, "Error not sent to client. User is confirming their device credential.")
            return
        }
        if (!mViewModel.isAwaitingResult) {
            Log.w(TAG, "Error not sent to client. Client is not awaiting a result.")
            return
        }
        mViewModel.isAwaitingResult = false
        mViewModel.clientExecutor.execute {
            mViewModel.clientCallback.onAuthenticationError(errorCode, errorString)
        }
    }

    /**
     * Sends an authentication failure event to the client callback.
     *
     * @see AuthenticationCallback.onAuthenticationFailed
     */
    private fun sendFailureToClient() {
        if (!mViewModel.isAwaitingResult) {
            Log.w(TAG, "Failure not sent to client. Client is not awaiting a result.")
            return
        }
        mViewModel.clientExecutor.execute {
            mViewModel.clientCallback.onAuthenticationFailed()
        }
    }

    /**
     * Checks if this fragment is responsible for drawing and handling the result of a device
     * credential fallback button on the prompt.
     *
     * @return Whether this fragment is managing a device credential button for the prompt.
     */  /* synthetic access */
    val isManagingDeviceCredentialButton: Boolean
        get() = (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
                && AuthenticatorUtils.isDeviceCredentialAllowed(
            mViewModel.allowedAuthenticators
        ))

    /**
     * Checks if this fragment should display the fingerprint dialog authentication UI to the user,
     * rather than delegate to the framework [android.hardware.biometrics.BiometricPrompt].
     *
     * @return Whether this fragment should display the fingerprint dialog UI.
     */
    private val isUsingFingerprintDialog: Boolean
        get() = (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || isFingerprintDialogNeededForCrypto
                || isFingerprintDialogNeededForErrorHandling)

    /**
     * Checks if this fragment should display the fingerprint dialog authentication UI for an
     * ongoing crypto-based authentication attempt.
     *
     * @return Whether this fragment should display the fingerprint dialog UI.
     * @see BiometricUtil.shouldUseFingerprintForCrypto
     */
    private val isFingerprintDialogNeededForCrypto: Boolean
        get() {
            val activity = activity
            return activity != null && mViewModel.cryptoObject != null && BiometricUtil.shouldUseFingerprintForCrypto(
                activity, Build.MANUFACTURER, Build.MODEL
            )
        }// On API 28, BiometricPrompt internally calls FingerprintManager#getErrorString(), which
    // requires fingerprint hardware to be present (b/151443237).
    /**
     * Checks if this fragment should invoke the fingerprint dialog, rather than the framework
     * biometric prompt, to handle an authentication error.
     *
     * @return Whether this fragment should invoke the fingerprint dialog.
     * @see BiometricUtil.shouldUseFingerprintForCrypto
     */
    private val isFingerprintDialogNeededForErrorHandling: Boolean
        get() =// On API 28, BiometricPrompt internally calls FingerprintManager#getErrorString(), which
            // requires fingerprint hardware to be present (b/151443237).
            Build.VERSION.SDK_INT == Build.VERSION_CODES.P && !hasSystemFeatureFingerprint(context)

    /**
     * Checks if the client activity is currently changing configurations (e.g. rotating screen
     * orientation).
     *
     * @return Whether the client activity is changing configurations.
     */
    private val isChangingConfigurations: Boolean
        get() {
            val activity = activity
            return activity != null && activity.isChangingConfigurations
        }

    /**
     * Gets the amount of time to wait after receiving an unrecoverable error before dismissing the
     * fingerprint dialog and forwarding the error to the client.
     *
     *
     * This method respects the result of
     * [BiometricUtil.shouldHideFingerprintDialog] and returns 0 if the latter
     * is `true`.
     *
     * @return The delay (in milliseconds) to apply before hiding the fingerprint dialog.
     */
    private val dismissDialogDelay: Int
        get() {
            val context = context
            return if (context != null && BiometricUtil.shouldHideFingerprintDialog(context, Build.MODEL)) 0 else HIDE_DIALOG_DELAY_MS
        }

    companion object {
        private const val TAG = "BiometricFragment"

        /**
         * Authentication was canceled by the library or framework.
         */
        const val CANCELED_FROM_INTERNAL = 0

        /**
         * Authentication was canceled by the user (e.g. by pressing the system back button).
         */
        const val CANCELED_FROM_USER = 1

        /**
         * Authentication was canceled by the user by pressing the negative button on the prompt.
         */
        const val CANCELED_FROM_NEGATIVE_BUTTON = 2

        /**
         * Authentication was canceled by the client application via
         * [BiometricDialogManager.cancelAuthentication].
         */
        const val CANCELED_FROM_CLIENT = 3

        /**
         * Tag used to identify the [FingerprintDialogFragment] attached to the client
         * activity/fragment.
         */
        private const val FINGERPRINT_DIALOG_FRAGMENT_TAG = "com.jiaoay.biometric.ui.fingerprint.FingerprintDialogFragment"

        /**
         * The amount of time (in milliseconds) before the flag indicating whether to dismiss the
         * fingerprint dialog instantly can be changed.
         */
        private const val DISMISS_INSTANTLY_DELAY_MS = 500

        /**
         * The amount of time (in milliseconds) to wait before dismissing the fingerprint dialog after
         * encountering an error. Ignored if
         * [BiometricUtil.shouldHideFingerprintDialog] is `true`.
         */
        private const val HIDE_DIALOG_DELAY_MS = 2000

        /**
         * The amount of time (in milliseconds) to wait before showing the authentication UI if
         * [BiometricViewModel.isDelayingPrompt] is `true`.
         */
        private const val SHOW_PROMPT_DELAY_MS = 600

        /**
         * Request code used when launching the confirm device credential Settings activity.
         */
        private const val REQUEST_CONFIRM_CREDENTIAL = 1

        /**
         * Creates a new instance of [BiometricFragment].
         *
         * @return A [BiometricFragment].
         */
        fun newInstance(): BiometricFragment {
            return BiometricFragment()
        }

        /**
         * Checks for possible error conditions prior to starting fingerprint authentication.
         *
         * @return 0 if there is no error, or a nonzero integer identifying the specific error.
         */
        private fun checkForFingerprintPreAuthenticationErrors(
            fingerprintManager: FingerprintManagerCompat
        ): Int {
            if (!fingerprintManager.isHardwareDetected) {
                return com.jiaoay.biometric.util.BiometricPrompt.ERROR_HW_NOT_PRESENT
            } else if (!fingerprintManager.hasEnrolledFingerprints()) {
                return com.jiaoay.biometric.util.BiometricPrompt.ERROR_NO_BIOMETRICS
            }
            return com.jiaoay.biometric.util.BiometricPrompt.BIOMETRIC_SUCCESS
        }
    }
}