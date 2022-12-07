package com.jiaoay.biometric.ui

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import java.lang.ref.WeakReference

/**
 * A lifecycle observer that clears the client callback reference held by a
 * {@link BiometricViewModel} when the lifecycle owner is destroyed.
 */
class ResetCallbackObserver(viewModel: BiometricViewModel) : LifecycleObserver {
    private var mViewModelRef: WeakReference<BiometricViewModel> = WeakReference(viewModel)

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun resetCallback() {
        mViewModelRef.get()?.resetClientCallback()
    }
}