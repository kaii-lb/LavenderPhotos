package com.kaii.photos.permissions.auth

import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.Screens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AuthManager(
    private val context: Context,
    title: String,
    subtitle: String,
    onSuccess: () -> Unit,
    onFailure: () -> Unit
) {
    private val cancellationSignal = CancellationSignal()
    private val prompt =
        BiometricPrompt.Builder(context)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

    private val promptCallback =
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                super.onAuthenticationSucceeded(result)

                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                super.onAuthenticationError(errorCode, errString)

                onFailure()
            }
        }

    fun authenticate() {
        prompt.authenticate(
            cancellationSignal,
            context.mainExecutor,
            promptCallback
        )
    }
}

@Composable
fun rememberSecureFolderAuthManager(
    extraAction: (() -> Unit)? = null
): AuthManager {
    val context = LocalContext.current
    val resources = LocalResources.current
    val navController = LocalNavController.current
    val coroutineScope = rememberCoroutineScope()

    return remember {
        AuthManager(
            context = context,
            title = resources.getString(R.string.secure_unlock),
            subtitle = resources.getString(R.string.secure_unlock_desc),
            onSuccess = {
                coroutineScope.launch {
                    if (extraAction != null) {
                        extraAction()
                        delay(AnimationConstants.DURATION.toLong())
                    }

                    navController.navigate(route = Screens.SecureFolder.GridView)
                }
            },
            onFailure = {
                coroutineScope.launch {
                    LavenderSnackbarController.pushEvent(
                        LavenderSnackbarEvents.MessageEvent(
                            message = resources.getString(R.string.secure_unlock_failed),
                            duration = SnackbarDuration.Short,
                            icon = R.drawable.secure_folder
                        )
                    )
                }
            }
        )
    }
}

@Composable
fun rememberExportAuthManager(
    onSuccess: () -> Unit
): AuthManager {
    val context = LocalContext.current
    val resources = LocalResources.current
    val coroutineScope = rememberCoroutineScope()

    return remember {
        AuthManager(
            context = context,
            title = resources.getString(R.string.data_and_backup),
            subtitle = resources.getString(R.string.exporting_backup),
            onSuccess = onSuccess,
            onFailure = {
                coroutineScope.launch {
                    LavenderSnackbarController.pushEvent(
                        LavenderSnackbarEvents.MessageEvent(
                            message = resources.getString(R.string.secure_unlock_failed),
                            duration = SnackbarDuration.Short,
                            icon = R.drawable.secure_folder
                        )
                    )
                }
            }
        )
    }
}