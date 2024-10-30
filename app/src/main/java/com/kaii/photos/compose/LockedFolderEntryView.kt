package com.kaii.photos.compose

import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.kaii.photos.R
import com.kaii.photos.helpers.CustomMaterialTheme
import com.kaii.photos.helpers.MultiScreenViewType

@Composable
fun LockedFolderEntryView(navController: NavHostController) {
    Column (
        modifier = Modifier
            .fillMaxSize(1f)
            .padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val context = LocalContext.current
        val cancellationSignal = CancellationSignal()
        var showHelpDialog by remember { mutableStateOf(false) }

        val prompt = BiometricPrompt.Builder(LocalContext.current)
            .setTitle("Unlock Secure Folder")
            .setSubtitle("Use your biometric credentials to unlock")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        val promptCallback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                super.onAuthenticationSucceeded(result)

                navController.navigate(MultiScreenViewType.LockedFolderView.name)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                super.onAuthenticationError(errorCode, errString)

                Toast.makeText(context, "Failed to authenticate :<", Toast.LENGTH_LONG).show()
            }
        }

        if(showHelpDialog) {
            AlertDialog(
                onDismissRequest = {
                    showHelpDialog = false
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showHelpDialog = false
                        }
                    ) {
                        Text(
                            text = "Close",
                            fontSize = TextUnit(14f, TextUnitType.Sp),
                        )
                    }
                },
                text = {
                    Column (
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Locked Folder Help",
                            fontSize = TextUnit(18f, TextUnitType.Sp),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(id = R.string.locked_folder_help_top),
                            fontSize = TextUnit(14f, TextUnitType.Sp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
	                        text = stringResource(id = R.string.locked_folder_help_bottom),
	                        fontSize = TextUnit(14f, TextUnitType.Sp),
	                    )
                    }
                }
            )
        }


        Icon(
            painter = painterResource(id = R.drawable.locked_folder),
            contentDescription = "Locked Folder Icon",
            modifier = Modifier.size(72.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                prompt.authenticate(
                    cancellationSignal,
                    context.mainExecutor,
                    promptCallback
                )
            },
        ) {
            Text(
                text = "Unlock Folder",
                fontSize = TextUnit(16f, TextUnitType.Sp)
            )
        }

        Button(
            onClick = {
                showHelpDialog = true
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = CustomMaterialTheme.colorScheme.surfaceVariant,
                contentColor = CustomMaterialTheme.colorScheme.onSurface
            )
        ) {
            Text(
                text = "More Info",
                fontSize = TextUnit(16f, TextUnitType.Sp)
            )
        }
    }
}
