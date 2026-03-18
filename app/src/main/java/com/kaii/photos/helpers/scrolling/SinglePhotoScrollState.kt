package com.kaii.photos.helpers.scrolling

import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SinglePhotoScrollState(
    private val coroutineScope: CoroutineScope,
    private val context: Context
) {
    var privacyMode by mutableStateOf(false)
        private set
    var videoLock by mutableStateOf(false)
        private set

    private val prompt = BiometricPrompt.Builder(context)
        .setTitle(context.resources.getString(R.string.privacy_scroll_mode))
        .setSubtitle(context.resources.getString(R.string.privacy_scroll_mode_prompt))
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        .build()

    private val promptCallback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
            super.onAuthenticationSucceeded(result)

            privacyMode = !privacyMode
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
            super.onAuthenticationError(errorCode, errString)

            coroutineScope.launch {
                LavenderSnackbarController.pushEvent(
                    LavenderSnackbarEvent.MessageEvent(
                        message = context.resources.getString(R.string.privacy_scroll_mode_fail),
                        duration = SnackbarDuration.Short,
                        icon = R.drawable.swipe
                    )
                )
            }
        }
    }

    @JvmName("setVideoLockMethod")
    fun setVideoLock(value: Boolean) {
        videoLock = value
    }

    fun togglePrivacyMode() {
        prompt.authenticate(
            CancellationSignal(),
            context.mainExecutor,
            promptCallback
        )
    }
}

@Composable
fun retainSinglePhotoScrollState(
    isOpenWithView: Boolean
): SinglePhotoScrollState {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val state = retain(isOpenWithView) {
        SinglePhotoScrollState(
            coroutineScope = coroutineScope,
            context = context
        )
    }

    val isLandscape by rememberDeviceOrientation()
    LaunchedEffect(isLandscape) {
        if (!isLandscape) state.setVideoLock(false)
    }

    return state
}