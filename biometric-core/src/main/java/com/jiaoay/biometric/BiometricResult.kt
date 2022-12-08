package com.jiaoay.biometric

sealed class BiometricResult {
    object Success : BiometricResult()
    object Failed : BiometricResult()
    object Error : BiometricResult()
}
