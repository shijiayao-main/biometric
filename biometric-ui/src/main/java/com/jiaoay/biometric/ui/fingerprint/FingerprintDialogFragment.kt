package com.jiaoay.biometric.ui.fingerprint

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.View
import androidx.annotation.IntDef
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.jiaoay.biometric.AuthenticatorUtils.isDeviceCredentialAllowed
import com.jiaoay.biometric.ui.BiometricViewModel
import com.jiaoay.biometric_ui.R
import com.jiaoay.biometric_ui.databinding.FingerprintDialogLayoutBinding

/**
 * A fragment that provides a standard prompt UI for fingerprint authentication on versions prior
 * to Android 9.0 (API 28).
 */
class FingerprintDialogFragment : DialogFragment() {
    /**
     * A possible state for the fingerprint dialog.
     */
    @IntDef(STATE_NONE, STATE_FINGERPRINT, STATE_FINGERPRINT_ERROR, STATE_FINGERPRINT_AUTHENTICATED)
    @Retention(AnnotationRetention.SOURCE)
    internal annotation class State

    /**
     * A handler used to post delayed events.
     */
    /* synthetic access */ val mHandler = Handler(Looper.getMainLooper())

    /**
     * A runnable that resets the dialog to its default state and appearance.
     */
    /* synthetic access */ val mResetDialogRunnable = Runnable { resetDialog() }

    /**
     * The view model for the ongoing authentication session.
     */
    val mViewModel by activityViewModels<BiometricViewModel>()

    /**
     * The text color used for displaying error messages.
     */
    private var mErrorTextColor = 0

    /**
     * The text color used for displaying help messages.
     */
    private var mNormalTextColor = 0

    val binding by lazy {
        FingerprintDialogLayoutBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        connectViewModel()
        mErrorTextColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getThemedColorFor(FingerprintDialogApi26Impl.colorErrorAttr)
        } else {
            val context = context
            if (context != null) ContextCompat.getColor(context, R.color.biometric_error_color) else 0
        }
        mNormalTextColor = getThemedColorFor(android.R.attr.textColorSecondary)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(mViewModel.title)

        // We have to use builder.getContext() instead of the usual getContext() in order to get
        // the appropriately themed context for this dialog.

        val subtitle = mViewModel.subtitle
        if (TextUtils.isEmpty(subtitle)) {
            binding.fingerprintSubtitle.visibility = View.GONE
        } else {
            binding.fingerprintSubtitle.visibility = View.VISIBLE
            binding.fingerprintSubtitle.text = subtitle
        }

        val description = mViewModel.description
        if (TextUtils.isEmpty(description)) {
            binding.fingerprintDescription.visibility = View.GONE
        } else {
            binding.fingerprintDescription.visibility = View.VISIBLE
            binding.fingerprintDescription.text = description
        }

        val negativeButtonText = if (isDeviceCredentialAllowed(mViewModel.allowedAuthenticators)) {
            getString(R.string.confirm_device_credential_password)
        } else {
            mViewModel.negativeButtonText!!
        }

        builder.setNegativeButton(negativeButtonText) { dialog, which ->
            mViewModel.setNegativeButtonPressPending(true)
        }

        builder.setView(binding.root)
        val dialog: Dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    override fun onResume() {
        super.onResume()
        mViewModel.fingerprintDialogPreviousState = STATE_NONE
        mViewModel.setFingerprintDialogState(STATE_FINGERPRINT)
        mViewModel.setFingerprintDialogHelpMessage(
            getString(R.string.fingerprint_dialog_touch_sensor)
        )
    }

    override fun onPause() {
        super.onPause()
        mHandler.removeCallbacksAndMessages(null)
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        mViewModel.setFingerprintDialogCancelPending(true)
    }

    /**
     * Connects the [BiometricViewModel] for the ongoing authentication session to this
     * fragment.
     */
    private fun connectViewModel() {
        mViewModel.fingerprintDialogState.observe(this) { state ->
            mHandler.removeCallbacks(mResetDialogRunnable)
            updateFingerprintIcon(state)
            updateHelpMessageColor(state)
            mHandler.postDelayed(mResetDialogRunnable, MESSAGE_DISPLAY_TIME_MS.toLong())
        }
        mViewModel.fingerprintDialogHelpMessage.observe(this) { helpMessage ->
            mHandler.removeCallbacks(mResetDialogRunnable)
            updateHelpMessageText(helpMessage)
            mHandler.postDelayed(mResetDialogRunnable, MESSAGE_DISPLAY_TIME_MS.toLong())
        }
    }

    /**
     * Updates the fingerprint icon to match the new dialog state, including animating between
     * states if necessary.
     *
     * @param state The new state for the fingerprint dialog.
     */
    fun updateFingerprintIcon(@State state: Int) {

        // Devices older than this do not have FP support (and also do not support SVG), so it's
        // fine for this to be a no-op. An error is returned immediately and the dialog is not
        // shown.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @State val previousState = mViewModel.fingerprintDialogPreviousState
            val icon = getAssetForTransition(previousState, state) ?: return
            binding.fingerprintIcon.setImageDrawable(icon)
            if (shouldAnimateForTransition(previousState, state)) {
                FingerprintDialogApi21Impl.startAnimation(icon)
            }
            mViewModel.fingerprintDialogPreviousState = state
        }
    }

    /**
     * Updates the color of the help message text to match the new dialog state.
     *
     * @param state The new state for the fingerprint dialog.
     */
    fun updateHelpMessageColor(@State state: Int) {

        val isError = state == STATE_FINGERPRINT_ERROR
        binding.fingerprintError.setTextColor(if (isError) mErrorTextColor else mNormalTextColor)
    }

    /**
     * Changes the help message text shown on the dialog.
     *
     * @param helpMessage The new help message text for the dialog.
     */
    fun updateHelpMessageText(helpMessage: CharSequence?) {
        binding.fingerprintError.text = helpMessage
    }

    /**
     * Resets the appearance of the dialog to its initial state (i.e. waiting for authentication).
     */
    fun resetDialog() {
        val context = context
        if (context == null) {
            Log.w(TAG, "Not resetting the dialog. Context is null.")
            return
        }
        mViewModel.setFingerprintDialogState(STATE_FINGERPRINT)
        mViewModel.setFingerprintDialogHelpMessage(
            context.getString(R.string.fingerprint_dialog_touch_sensor)
        )
    }

    /**
     * Gets the theme color corresponding to a given style attribute.
     *
     * @param attr The desired attribute.
     * @return The theme color for that attribute.
     */
    private fun getThemedColorFor(attr: Int): Int {
        val context = context
        val activity = activity
        if (context == null || activity == null) {
            Log.w(TAG, "Unable to get themed color. Context or activity is null.")
            return 0
        }
        val tv = TypedValue()
        val theme = context.theme
        theme.resolveAttribute(attr, tv, true /* resolveRefs */)
        val arr = activity.obtainStyledAttributes(tv.data, intArrayOf(attr))
        val color = arr.getColor(0 /* index */, 0 /* defValue */)
        arr.recycle()
        return color
    }

    /**
     * Checks if the fingerprint icon should animate when transitioning between dialog states.
     *
     * @param previousState The previous state for the fingerprint dialog.
     * @param state         The new state for the fingerprint dialog.
     * @return Whether the fingerprint icon should animate.
     */
    private fun shouldAnimateForTransition(@State previousState: Int, @State state: Int): Boolean {
        if (previousState == STATE_NONE && state == STATE_FINGERPRINT) {
            return false
        } else if (previousState == STATE_FINGERPRINT && state == STATE_FINGERPRINT_ERROR) {
            return true
        } else if (previousState == STATE_FINGERPRINT_ERROR && state == STATE_FINGERPRINT) {
            return true
        } else if (previousState == STATE_FINGERPRINT && state == STATE_FINGERPRINT_AUTHENTICATED) {
            // TODO(b/77328470): add animation when fingerprint is authenticated
            return false
        }
        return false
    }

    /**
     * Gets the icon or animation asset that should appear when transitioning between dialog states.
     *
     * @param previousState The previous state for the fingerprint dialog.
     * @param state         The new state for the fingerprint dialog.
     * @return A drawable asset to be used for the fingerprint icon.
     */
    private fun getAssetForTransition(@State previousState: Int, @State state: Int): Drawable? {
        val context = context
        if (context == null) {
            Log.w(TAG, "Unable to get asset. Context is null.")
            return null
        }
        val iconRes: Int = if (previousState == STATE_NONE && state == STATE_FINGERPRINT) {
            R.drawable.fingerprint_dialog_fp_icon
        } else if (previousState == STATE_FINGERPRINT && state == STATE_FINGERPRINT_ERROR) {
            R.drawable.fingerprint_dialog_error
        } else if (previousState == STATE_FINGERPRINT_ERROR && state == STATE_FINGERPRINT) {
            R.drawable.fingerprint_dialog_fp_icon
        } else if (previousState == STATE_FINGERPRINT && state == STATE_FINGERPRINT_AUTHENTICATED
        ) {
            // TODO(b/77328470): add animation when fingerprint is authenticated
            R.drawable.fingerprint_dialog_fp_icon
        } else {
            return null
        }
        return ContextCompat.getDrawable(context, iconRes)
    }

    companion object {
        private const val TAG = "FingerprintFragment"

        /**
         * The dialog has not been initialized.
         */
        const val STATE_NONE = 0

        /**
         * Waiting for the user to authenticate with fingerprint.
         */
        const val STATE_FINGERPRINT = 1

        /**
         * An error or failure occurred during fingerprint authentication.
         */
        const val STATE_FINGERPRINT_ERROR = 2

        /**
         * The user has successfully authenticated with fingerprint.
         */
        const val STATE_FINGERPRINT_AUTHENTICATED = 3

        /**
         * Transient errors and help messages will be displayed on the dialog for this amount of time.
         */
        private const val MESSAGE_DISPLAY_TIME_MS = 2000

        /**
         * Creates a new instance of [FingerprintDialogFragment].
         *
         * @return A [FingerprintDialogFragment].
         */
        @JvmStatic
        fun newInstance(): FingerprintDialogFragment {
            return FingerprintDialogFragment()
        }
    }
}