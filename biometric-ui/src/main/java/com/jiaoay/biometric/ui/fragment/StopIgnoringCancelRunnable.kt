package com.jiaoay.biometric.ui.fragment

import com.jiaoay.biometric.ui.BiometricViewModel
import java.lang.ref.WeakReference

/**
 * A runnable with a weak reference to a [BiometricViewModel] that can be used to invoke
 * [BiometricViewModel.setIgnoringCancel] with a value of `false`.
 */
class StopIgnoringCancelRunnable(viewModel: BiometricViewModel?) : Runnable {
    private val mViewModelRef: WeakReference<BiometricViewModel?>

    init {
        mViewModelRef = WeakReference(viewModel)
    }

    override fun run() {
        if (mViewModelRef.get() != null) {
            mViewModelRef.get()!!.isIgnoringCancel = false
        }
    }
}