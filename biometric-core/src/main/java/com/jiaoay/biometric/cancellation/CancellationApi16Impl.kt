package com.jiaoay.biometric.cancellation

import android.os.Build
import android.os.CancellationSignal
import androidx.annotation.RequiresApi

/**
 * Nested class to avoid verification errors for methods introduced in Android 4.1 (API 16).
 */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
object CancellationApi16Impl {
    /**
     * Creates a new instance of the platform class [CancellationSignal].
     *
     * @return An instance of [CancellationSignal].
     */
    fun create(): CancellationSignal {
        return CancellationSignal()
    }

    /**
     * Calls [CancellationSignal.cancel] for the given cancellation signal.
     */
    fun cancel(cancellationSignal: CancellationSignal) {
        cancellationSignal.cancel()
    }
}