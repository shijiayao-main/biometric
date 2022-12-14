package com.jiaoay.biometric.demo

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jiaoay.biometric.FingerprintApi21Compat
import com.jiaoay.biometric.FingerprintApi28Compat
import com.jiaoay.biometric.demo.ui.theme.BiometricDemoTheme
import com.jiaoay.biometric.ui.BiometricV2ViewModel
import com.jiaoay.biometric.ui.fragment.BiometricFragment.Companion.CANCELED_FROM_INTERNAL

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private val biometricHelper by lazy {
        BiometricHelper(this)
    }

    val viewModel by viewModels<BiometricV2ViewModel>()


    private val compat by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && false) {
            FingerprintApi28Compat(this)
        } else {
            FingerprintApi21Compat(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
//            val viewModel by viewModels<BiometricV2ViewModel>()
            BiometricDemoTheme {
                var biometricType by remember {
                    mutableStateOf(BiometricType.NONE)
                }

                var isEnableBiometric by remember {
                    mutableStateOf(false)
                }

                LaunchedEffect(key1 = true) {
                    Log.d(TAG, "onCreate: LaunchEffect")
                    biometricType = biometricHelper.getBiometricType()
                    isEnableBiometric = biometricHelper.biometricEnable()
                }

                BiometricInfoView(
                    biometricType = biometricType,
                    isEnableBiometric = isEnableBiometric,
                    negativeClick = {
                        compat.recycle()
                    }
                ) {
                    compat.authWithFingerprint(null)
//                    biometricHelper.showBiometricPrompt(
//                        title = "Title",
//                        negativeButtonText = "Cancel",
//                        subtitle = "Subtitle",
//                        description = "Description",
//                        confirmationRequired = true,
//                    ) {
//                        // Do something when success
//                        Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show()
//                    }
                }
            }
        }
    }

    override fun onDestroy() {
        viewModel.cancelAuthentication(CANCELED_FROM_INTERNAL)
        super.onDestroy()
    }
}

@Composable
private fun BiometricInfoView(
    biometricType: BiometricType,
    isEnableBiometric: Boolean,
    negativeClick: () -> Unit,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "biometricType: $biometricType")
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                enabled = isEnableBiometric,
                onClick = {
                    onClick()
                }
            ) {
                Text(text = "?????????????????????")
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(onClick = {
                negativeClick()
            }) {
                Text(text = "????????????")
            }
        }
    }
}