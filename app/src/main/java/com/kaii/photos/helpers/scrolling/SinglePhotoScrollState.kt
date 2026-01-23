package com.kaii.photos.helpers.scrolling

import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.datastore.Video
import com.kaii.photos.models.main_activity.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SinglePhotoScrollState(
    private val mainViewModel: MainViewModel,
    private val coroutineScope: CoroutineScope,
    private val isOpenWithView: Boolean,
    private val context: Context
) {
    private var _privacyMode by mutableStateOf(false)
    private var _videoLock by mutableStateOf(false)
    private var _videoWasMuted by mutableStateOf(false)
    private val _videoAutoplay = MutableStateFlow(false)

    val privacyMode by derivedStateOf { _privacyMode }
    val videoLock by derivedStateOf { _videoLock }
    val videoWasMuted by derivedStateOf { _videoWasMuted }
    val videoAutoplay = _videoAutoplay.asStateFlow()

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

            _privacyMode = !_privacyMode
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
            super.onAuthenticationError(errorCode, errString)

            coroutineScope.launch {
                LavenderSnackbarController.pushEvent(
                    LavenderSnackbarEvents.MessageEvent(
                        message = context.resources.getString(R.string.privacy_scroll_mode_fail),
                        duration = SnackbarDuration.Short,
                        icon = R.drawable.swipe
                    )
                )
            }
        }
    }

    init {
        coroutineScope.launch {
            mainViewModel.settings.Video.getMuteOnStart().collect {
                _videoWasMuted = it && !isOpenWithView
            }

            mainViewModel.settings.Video.getShouldAutoPlay().collect {
                _videoAutoplay.value = it || isOpenWithView
            }
        }
    }

    fun setVideoLock(value: Boolean) {
        _videoLock = value
    }

    fun resetMute() = coroutineScope.launch {
        _videoWasMuted = mainViewModel.settings.Video.getMuteOnStart().first() && !isOpenWithView
    }

    fun setWasMuted(value: Boolean) {
        _videoWasMuted = value
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
fun rememberSinglePhotoScrollState(
    isOpenWithView: Boolean
): SinglePhotoScrollState {
    val mainViewModel = LocalMainViewModel.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val state = remember(isOpenWithView) {
        SinglePhotoScrollState(
            mainViewModel = mainViewModel,
            coroutineScope = coroutineScope,
            isOpenWithView = isOpenWithView,
            context = context
        )
    }

    val isLandscape by rememberDeviceOrientation()
    LaunchedEffect(isLandscape) {
        if (!isLandscape) state.setVideoLock(false)
    }

    return state
}