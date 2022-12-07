package com.jiaoay.biometric.ui

import android.os.Build
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import com.jiaoay.biometric.AuthenticatorUtils
import com.jiaoay.biometric.PromptInfo
import com.jiaoay.biometric.authentication.AuthenticationCallback
import com.jiaoay.biometric.crypto.CryptoObject
import com.jiaoay.biometric.manager.AuthenticatorTypes
import com.jiaoay.biometric.ui.fragment.BiometricFragment

class BiometricDialogManager(
    private val activity: FragmentActivity,
    private val fragmentManager: FragmentManager,
    private val callback: AuthenticationCallback
) {

    companion object {
        private const val TAG = "BiometricDialogManager"

        /**
         * Tag used to identify the [BiometricFragment] attached to the client activity/fragment.
         */
        private const val BIOMETRIC_FRAGMENT_TAG = "com.jiaoay.biometric.ui.fragment.BiometricFragment"
    }

    init {

        val viewModel = getViewModel(activity);
        addObservers(
            fragment = activity,
            viewModel = viewModel
        )
        // TODO:
//        if (executor != null) {
//            viewModel.setClientExecutor(executor)
//        }
        viewModel.clientCallback = callback
    }

    /**
     * Gets the biometric view model instance for the given activity, creating one if necessary.
     *
     * @param activity The client activity that will (directly or indirectly) host the prompt.
     * @return A biometric view model tied to the lifecycle of the given activity.
     */
    private fun getViewModel(activity: FragmentActivity): BiometricViewModel {
        return ViewModelProvider(activity)[BiometricViewModel::class.java]
    }


    /**
     * Adds the necessary lifecycle observers to the given fragment host.
     *
     * @param fragment  The fragment of the client application that will host the prompt.
     * @param viewModel A biometric view model tied to the lifecycle of the client activity.
     */
    private fun addObservers(
        fragment: FragmentActivity, viewModel: BiometricViewModel?
    ) {
        if (viewModel != null) {
            // Ensure that the callback is reset to avoid leaking fragment instances (b/167014923).
            fragment.lifecycle.addObserver(ResetCallbackObserver(viewModel))
        }
    }


    /**
     * Shows the biometric prompt to the user. The prompt survives lifecycle changes by default. To
     * cancel authentication and dismiss the prompt, use [.cancelAuthentication].
     *
     *
     * Calling this method invokes crypto-based authentication, which is incompatible with
     * **Class 2** (formerly **Weak**) biometrics and (prior to Android
     * 11) device credential. Therefore, it is an error for `info` to explicitly allow any
     * of these authenticator types on an incompatible Android version.
     *
     * @param info   An object describing the appearance and behavior of the prompt.
     * @param crypto A crypto object to be associated with this authentication.
     *
     * @throws IllegalArgumentException If any of the allowed authenticator types specified by
     * `info` do not support crypto-based authentication.
     *
     * @see .authenticate
     * @see PromptInfo.Builder.setAllowedAuthenticators
     */
    fun authenticate(info: PromptInfo, crypto: CryptoObject) {

        // Ensure that all allowed authenticators support crypto auth.
        @AuthenticatorTypes val authenticators = AuthenticatorUtils.getConsolidatedAuthenticators(info, crypto)
        if (AuthenticatorUtils.isWeakBiometricAllowed(authenticators)) {
            throw IllegalArgumentException(
                "Crypto-based authentication is not supported for "
                        + "Class 2 (Weak) biometrics."
            )
        }
        if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.R
                    && AuthenticatorUtils.isDeviceCredentialAllowed(authenticators))
        ) {
            throw IllegalArgumentException(
                ("Crypto-based authentication is not supported for "
                        + "device credential prior to API 30.")
            )
        }
        authenticateInternal(info, crypto)
    }

    /**
     * Shows the biometric prompt to the user. The prompt survives lifecycle changes by default. To
     * cancel authentication and dismiss the prompt, use [.cancelAuthentication].
     *
     * @param info An object describing the appearance and behavior of the prompt.
     *
     * @see .authenticate
     */
    fun authenticate(info: PromptInfo) {
        authenticateInternal(info = info, crypto = null)
    }

    /**
     * Shows the biometric prompt to the user and begins authentication.
     *
     * @param info   An object describing the appearance and behavior of the prompt.
     * @param crypto A crypto object to be associated with this authentication.
     */
    private fun authenticateInternal(info: PromptInfo, crypto: CryptoObject?) {
        if (fragmentManager.isStateSaved) {
            Log.e(TAG, "Unable to start authentication. Called after onSaveInstanceState().")
            return
        }
        val biometricFragment = findOrAddBiometricFragment(fragmentManager)
        biometricFragment.authenticate(info, crypto)
    }

    /**
     * Cancels the ongoing authentication session and dismisses the prompt.
     *
     *
     * On versions prior to Android 10 (API 29), calling this method while the user is
     * authenticating with their device credential will NOT work as expected. See
     * [PromptInfo.Builder.setDeviceCredentialAllowed] for more details.
     */
    fun cancelAuthentication() {
        val biometricFragment = findBiometricFragment(fragmentManager)
        if (biometricFragment == null) {
            Log.e(TAG, "Unable to cancel authentication. BiometricFragment not found.")
            return
        }
        biometricFragment.cancelAuthentication(BiometricFragment.CANCELED_FROM_CLIENT)
    }

    /**
     * Searches for a [BiometricFragment] instance that has been added to an activity or
     * fragment.
     *
     * @param fragmentManager The fragment manager that will be used to search for the fragment.
     * @return An instance of [BiometricFragment] found by the fragment manager, or
     * `null` if no such fragment is found.
     */
    private fun findBiometricFragment(
        fragmentManager: FragmentManager
    ): BiometricFragment? {
        return fragmentManager.findFragmentByTag(
            BIOMETRIC_FRAGMENT_TAG
        ) as BiometricFragment?
    }


    /**
     * Returns a [BiometricFragment] instance that has been added to an activity or fragment,
     * adding one if necessary.
     *
     * @param fragmentManager The fragment manager used to search for and/or add the fragment.
     * @return An instance of [BiometricFragment] associated with the fragment manager.
     */
    private fun findOrAddBiometricFragment(
        fragmentManager: FragmentManager
    ): BiometricFragment {
        val biometricFragment: BiometricFragment = findBiometricFragment(fragmentManager) ?: let {
            val fragment = BiometricFragment.newInstance()
            fragmentManager.beginTransaction()
                .add(fragment, BIOMETRIC_FRAGMENT_TAG)
                .commitAllowingStateLoss()

            // For the case when onResume() is being called right after authenticate,
            // we need to make sure that all fragment transactions have been committed.
            fragmentManager.executePendingTransactions()
            fragment
        }
        return biometricFragment
    }
}