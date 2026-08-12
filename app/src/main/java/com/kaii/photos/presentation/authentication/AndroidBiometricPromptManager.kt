package com.kaii.photos.presentation.authentication

import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.kaii.photos.domain.authentication.BiometricPromptManager
import com.kaii.photos.domain.authentication.PromptAuthResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class AndroidBiometricPromptManager(
    private val context: Context,
    title: String,
    subtitle: String
) : BiometricPromptManager {
    private val _events = Channel<PromptAuthResult>()
    override val events = _events.receiveAsFlow()

    private val prompt = BiometricPrompt.Builder(context)
        .setTitle(title)
        .setSubtitle(subtitle)
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        .build()

    private val promptCallback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
            super.onAuthenticationSucceeded(result)

            _events.trySend(
                element = PromptAuthResult.Succeeded
            )
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
            super.onAuthenticationError(errorCode, errString)

            _events.trySend(
                element = PromptAuthResult.Failed
            )
        }
    }

    override fun authenticate() {
        _events.trySend(
            element = PromptAuthResult.Loading
        )

        prompt.authenticate(
            CancellationSignal(),
            context.mainExecutor,
            promptCallback
        )
    }
}

@Composable
fun rememberBiometricPromptManager(
    title: String,
    subtitle: String
): BiometricPromptManager {
    val context = LocalContext.current

    return remember(context, title, subtitle) {
        AndroidBiometricPromptManager(
            context = context,
            title = title,
            subtitle = subtitle
        )
    }
}