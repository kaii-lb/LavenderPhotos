package com.kaii.photos.permissions.secure_folder

import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.helpers.Screens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SecureFolderLaunchManager(
    private val context: Context,
    navController: NavController,
    scope: CoroutineScope
) {
    private val cancellationSignal = CancellationSignal()
    private val prompt =
        BiometricPrompt.Builder(context)
            .setTitle(context.resources.getString(R.string.secure_unlock))
            .setSubtitle(context.resources.getString(R.string.secure_unlock_desc))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

    private val promptCallback =
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                super.onAuthenticationSucceeded(result)

                navController.navigate(route = Screens.SecureFolder.GridView)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                super.onAuthenticationError(errorCode, errString)

                scope.launch {
                    LavenderSnackbarController.pushEvent(
                        LavenderSnackbarEvents.MessageEvent(
                            message = context.resources.getString(R.string.secure_unlock_failed),
                            duration = SnackbarDuration.Short,
                            icon = R.drawable.secure_folder
                        )
                    )
                }
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
fun rememberSecureFolderLaunchManager(): SecureFolderLaunchManager {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val coroutineScope = rememberCoroutineScope()

    return remember {
        SecureFolderLaunchManager(
            context = context,
            navController = navController,
            scope = coroutineScope
        )
    }
}