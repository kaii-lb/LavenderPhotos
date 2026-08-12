package com.kaii.photos.compose.pages

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.modifiers.wiggle
import com.kaii.photos.compose.widgets.infiniteLoadingIndicator
import com.kaii.photos.domain.authentication.BiometricPromptManager
import com.kaii.photos.domain.authentication.PromptAuthAction
import com.kaii.photos.domain.authentication.PromptAuthResult
import com.kaii.photos.helpers.Screens
import com.kaii.photos.models.PrivacyModeActiveViewModel
import com.kaii.photos.presentation.authentication.rememberBiometricPromptManager
import com.kaii.photos.ui.theme.LocalExtraColorsPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlin.time.Duration.Companion.seconds

@Preview
@Composable
private fun PrivacyModeActivePagePreview() {
    var state by remember { mutableStateOf(PromptAuthResult.Idle) }
    val biometricPromptManager = remember {
        object : BiometricPromptManager {
            override val events = emptyFlow<PromptAuthResult>()

            override fun authenticate() {
                state = PromptAuthResult.Succeeded
            }
        }
    }

    PrivacyModeActiveImpl(
        state = { state },
        modifier = Modifier,
        unlock = biometricPromptManager::authenticate
    )
}

@Composable
fun PrivacyModeActivePage(
    viewModel: PrivacyModeActiveViewModel,
    modifier: Modifier = Modifier
) {
    val navController = LocalNavController.current

    var state by remember { mutableStateOf(PromptAuthResult.Idle) }

    val biometricPromptManager = rememberBiometricPromptManager(
        title = stringResource(id = R.string.privacy_scroll_mode),
        subtitle = stringResource(id = R.string.privacy_scroll_mode_prompt)
    )

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                PromptAuthAction.UseBiometrics -> biometricPromptManager.authenticate()

                PromptAuthAction.UsePassword -> navController.navigate(
                    route = Screens.Startup.ScreenLock
                ) {
                    popUpTo(Screens.Startup.PrivacyModeActive) {
                        inclusive = true
                    }
                }
            }
        }
    }

    LaunchedEffect(biometricPromptManager) {
        biometricPromptManager.events.collect { event ->
            when (event) {
                PromptAuthResult.Succeeded -> {
                    state = event
                    viewModel.markUnlocked()

                    delay(1.5.seconds)

                    navController.navigate(
                        route = Screens.MainPages
                    ) {
                        popUpTo(Screens.Startup.PrivacyModeActive) {
                            inclusive = true
                        }
                    }
                }

                else -> state = event
            }
        }
    }

    PrivacyModeActiveImpl(
        state = { state },
        modifier = modifier,
        unlock = viewModel::unlock
    )
}

@Composable
private fun PrivacyModeActiveImpl(
    state: () -> PromptAuthResult,
    modifier: Modifier,
    unlock: () -> Unit
) {
    Scaffold(
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(paddingValues = padding)
                .fillMaxSize()
                .padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(
                space = 16.dp,
                alignment = Alignment.CenterVertically
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val animationSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
            val iconBackgroundColor by animateColorAsState(
                targetValue =
                    if (state() == PromptAuthResult.Succeeded) LocalExtraColorsPalette.current.success
                    else MaterialTheme.colorScheme.errorContainer,
                animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()
            )

            AnimatedContent(
                targetState = state() == PromptAuthResult.Succeeded,
                transitionSpec = {
                    val enter = fadeIn() + scaleIn(animationSpec)
                    val exit = fadeOut() + scaleOut(animationSpec)

                    enter.togetherWith(exit)
                },
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(96.dp)
                    .infiniteLoadingIndicator(
                        loadingIndicatorColor = MaterialTheme.colorScheme.error,
                        loading = {
                            state() == PromptAuthResult.Loading
                        }
                    )
                    .background(
                        color = iconBackgroundColor,
                        shape = CircleShape
                    )
                    .wiggle(
                        start = state() == PromptAuthResult.Failed,
                        ratio = 0.05f,
                        times = 8,
                        durationMillis = 300
                    )
                    .padding(all = 20.dp)
            ) { state ->
                if (state) {
                    Icon(
                        painter = painterResource(id = R.drawable.checkmark_thin),
                        contentDescription = stringResource(id = R.string.privacy_scroll_mode_last_active),
                        tint = LocalExtraColorsPalette.current.onSuccess,
                        modifier = Modifier
                            .size(76.dp)
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.do_not_touch),
                        contentDescription = stringResource(id = R.string.privacy_scroll_mode_last_active),
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier
                            .size(76.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(id = R.string.privacy_scroll_mode_last_active),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = stringResource(id = R.string.privacy_scroll_mode_last_active_desc),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = unlock,
                shapes = ButtonDefaults.shapes()
            ) {
                Text(
                    text = stringResource(id = R.string.privacy_authenticate),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}