package com.kaii.photos.compose.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.ExpressivePasswordField
import com.kaii.photos.di.appModule
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.Screens
import com.kaii.photos.ui.theme.PhotosTheme
import com.kaii.photos.widgets.ExpressivePasswordFieldState
import com.kaii.photos.widgets.rememberExpressivePasswordFieldState
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@Preview
@Composable
private fun ScreenLockPreview() {
    PhotosTheme(
        theme = 1
    ) {
        val state = rememberExpressivePasswordFieldState(
            action = ExpressivePasswordFieldState.Action.Unlock,
            passwordBytes = flowOf(
                "123456".toByteArray()
            ),
            saltBytes = flowOf(
                ByteArray(0)
            ),
            onSuccess = { _, _ -> }
        )

        val code by state.code.collectAsStateWithLifecycle(initialValue = emptyList())
        val status by state.status.collectAsStateWithLifecycle()

        ScreenLock(
            action = ExpressivePasswordFieldState.Action.Unlock,
            code = { code },
            status = { status },
            modifier = Modifier,
            onKeyPressed = state::addCode,
            onDeletePressed = state::deleteCode,
            onSubmitPressed = state::submit
        )
    }
}

@Composable
fun ScreenLock(
    action: ExpressivePasswordFieldState.Action,
    modifier: Modifier = Modifier
) {
    val resources = LocalResources.current
    val navController = LocalNavController.current
    val settings = LocalContext.current.appModule.settings.permissions
    val coroutineScope = rememberCoroutineScope()

    val state = rememberExpressivePasswordFieldState(action) { password, salt ->
        coroutineScope.launch {
            when (action) {
                ExpressivePasswordFieldState.Action.Unlock -> {
                    delay(AnimationConstants.DURATION_LONG.toLong())
                    navController.navigate(Screens.MainPages) {
                        popUpTo(Screens.Startup.ScreenLock) {
                            inclusive = true
                        }
                    }
                }

                ExpressivePasswordFieldState.Action.Verify -> {
                    navController.navigate(
                        Screens.Settings.MainPage.PrivacyAndSecurity.ScreenLock(
                            action = ExpressivePasswordFieldState.Action.Set
                        )
                    ) {
                        popUpTo(Screens.Settings.MainPage.PrivacyAndSecurity) {
                            inclusive = false
                        }
                    }
                }

                ExpressivePasswordFieldState.Action.Set -> {
                    settings.setPassword(password)
                    settings.setSalt(salt)

                    val message = if (password != null) {
                        R.string.app_lock_password_set
                    } else {
                        R.string.app_lock_password_cleared
                    }

                    LavenderSnackbarController.pushEvent(
                        event = LavenderSnackbarEvent.MessageEvent(
                            message = resources.getString(message),
                            icon = R.drawable.password,
                            duration = SnackbarDuration.Short
                        )
                    )

                    navController.popBackStack(Screens.Settings.MainPage.PrivacyAndSecurity, false)
                }
            }
        }
    }

    val code by state.code.collectAsStateWithLifecycle(initialValue = emptyList())
    val status by state.status.collectAsStateWithLifecycle()

    ScreenLock(
        action = action,
        code = { code },
        status = { status },
        modifier = modifier,
        onKeyPressed = state::addCode,
        onDeletePressed = state::deleteCode,
        onSubmitPressed = state::submit
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScreenLock(
    action: ExpressivePasswordFieldState.Action,
    code: () -> List<ExpressivePasswordFieldState.Code>,
    status: () -> ExpressivePasswordFieldState.Status,
    modifier: Modifier = Modifier,
    onKeyPressed: (code: Int) -> Unit,
    onDeletePressed: () -> Unit,
    onSubmitPressed: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                alignment = Alignment.CenterVertically
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (action) {
                    ExpressivePasswordFieldState.Action.Unlock -> stringResource(id = R.string.app_lock_unlock)
                    ExpressivePasswordFieldState.Action.Verify -> stringResource(id = R.string.app_lock_verify)
                    ExpressivePasswordFieldState.Action.Set -> stringResource(id = R.string.app_lock_set)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = stringResource(id = R.string.app_lock_enter_pin),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(48.dp))

            ExpressivePasswordField(
                code = code,
                status = status,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(64.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            repeat(3) { index ->
                KeyRow(
                    buttons = listOf(
                        3 * index + 1,
                        3 * index + 2,
                        3 * index + 3
                    ),
                    onKeyPressed = onKeyPressed
                )
            }

            LastKeyRow(
                onZeroPressed = {
                    onKeyPressed(0)
                },
                onDeletePressed = onDeletePressed,
                onSubmitPressed = onSubmitPressed
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun KeyRow(
    buttons: List<Int>,
    modifier: Modifier = Modifier,
    onKeyPressed: (code: Int) -> Unit
) {
    ButtonGroup(
        overflowIndicator = {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = Alignment.CenterHorizontally
        ),
        modifier = modifier
            .fillMaxWidth(0.7f)
    ) {
        buttons.forEach { code ->
            customItem(
                menuContent = {},
                buttonGroupContent = {
                    Button(
                        onClick = {
                            onKeyPressed(code)
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(),
                        shapes = ButtonDefaults.shapes(
                            shape = CircleShape,
                            pressedShape = RoundedCornerShape(16.dp)
                        ),
                        modifier = Modifier
                            .aspectRatio(1f)
                            .weight(1f)
                    ) {
                        Text(
                            text = code.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun LastKeyRow(
    modifier: Modifier = Modifier,
    onZeroPressed: () -> Unit,
    onDeletePressed: () -> Unit,
    onSubmitPressed: () -> Unit
) {
    ButtonGroup(
        overflowIndicator = {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            space = 4.dp,
            alignment = Alignment.CenterHorizontally
        ),
        modifier = modifier
            .fillMaxWidth(0.7f)
    ) {
        customItem(
            menuContent = {},
            buttonGroupContent = {
                Button(
                    onClick = onDeletePressed,
                    shapes = ButtonDefaults.shapes(
                        shape = CircleShape,
                        pressedShape = RoundedCornerShape(16.dp)
                    ),
                    modifier = Modifier
                        .aspectRatio(1f)
                        .weight(1f)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.backspace),
                        contentDescription = stringResource(id = R.string.app_lock_backspace),
                        modifier = Modifier
                            .size(32.dp)
                    )
                }
            }
        )

        customItem(
            menuContent = {},
            buttonGroupContent = {
                Button(
                    onClick = onZeroPressed,
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    shapes = ButtonDefaults.shapes(
                        shape = CircleShape,
                        pressedShape = RoundedCornerShape(16.dp)
                    ),
                    modifier = Modifier
                        .aspectRatio(1f)
                        .weight(1f)
                ) {
                    Text(
                        text = "0",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        )

        customItem(
            menuContent = {},
            buttonGroupContent = {
                Button(
                    onClick = onSubmitPressed,
                    shapes = ButtonDefaults.shapes(
                        shape = CircleShape,
                        pressedShape = RoundedCornerShape(16.dp)
                    ),
                    modifier = Modifier
                        .aspectRatio(1f)
                        .weight(1f)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.checkmark_thin),
                        contentDescription = stringResource(id = R.string.app_lock_submit),
                        modifier = Modifier
                            .size(32.dp)
                    )
                }
            }
        )
    }
}