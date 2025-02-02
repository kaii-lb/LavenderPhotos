package com.kaii.photos.compose

import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.MainActivity.Companion.mainViewModel
import com.kaii.photos.datastore.AlbumsList
import com.kaii.photos.datastore.Versions
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.MainScreenViewType
import com.kaii.photos.helpers.appRestoredFilesDir
import com.kaii.photos.helpers.appSecureFolderDir
import com.kaii.photos.helpers.baseInternalStorageDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun LockedFolderEntryView(
    currentView: MutableState<MainScreenViewType>
) {
    val navController = LocalNavController.current

    BackHandler(
        enabled = currentView.value == MainScreenViewType.SecureFolder && navController.currentBackStackEntry?.destination?.route == MultiScreenViewType.MainScreen.name
    ) {
        currentView.value = MainScreenViewType.PhotosGridView
    }

    val context = LocalContext.current
    val cancellationSignal = CancellationSignal()

	// TODO: move again to Android/data for space purposes
	// moves media from old dir to new dir for secure folder
	val shouldMigrate by mainViewModel.settings.Versions.getShouldMigrateToEncryptedSecurePhotos(context).collectAsStateWithLifecycle(initialValue = false)
	var launchSecureFolder by remember { mutableStateOf(false) }

	LaunchedEffect(launchSecureFolder) {
		if (launchSecureFolder) navController.navigate(MultiScreenViewType.LockedFolderView.name)
	}

	LaunchedEffect(shouldMigrate) {
		withContext(Dispatchers.IO) {
			val oldDir = context.getDir("locked_folder", Context.MODE_PRIVATE)
			oldDir?.let { oldFilesDir ->
				val subFiles = oldFilesDir.listFiles()

				if (subFiles?.isNotEmpty() == true) {
					val newDir = context.appSecureFolderDir
					subFiles.forEach { file ->
						val newPath = newDir + "/" + file.name
						file.copyTo(File(newPath))
						file.delete()
					}
				}
			}

			// move unencrypted files to restored files dir
			if (shouldMigrate) {
				val restoredFilesDir = context.appRestoredFilesDir

				val unencryptedDir = File(context.appSecureFolderDir)
				val subFiles = unencryptedDir.listFiles()

				if (subFiles?.isNotEmpty() == true) {
					subFiles.forEach { file ->
						val newPath = restoredFilesDir + "/" + file.name
						file.copyTo(File(newPath))
						file.delete()
					}
				}

				mainViewModel.settings.Versions.setShouldMigrateToEncryptedSecurePhotos(false)
                mainViewModel.settings.AlbumsList.addToAlbumsList(
                    restoredFilesDir.replace(baseInternalStorageDirectory, "")
                )
			}
		}
	}

	if (shouldMigrate) {
        LoadingDialog(
            title = "Migrating",
            body = "Moving photos out of secure folder, they can be found in \"Restored Files\" album"
        )

		return
	}

    val prompt = BiometricPrompt.Builder(LocalContext.current)
        .setTitle("Unlock Secure Folder")
        .setSubtitle("Use your biometric credentials to unlock")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        .build()

    val promptCallback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
            super.onAuthenticationSucceeded(result)
            launchSecureFolder = true
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
            super.onAuthenticationError(errorCode, errString)

            Toast.makeText(context, "Failed to authenticate :<", Toast.LENGTH_LONG).show()
        }
    }

    val showHelpDialog = remember { mutableStateOf(false) }
    if(showHelpDialog.value) {
        ExplanationDialog(
            title = "Secure Folder",
            explanation = stringResource(id = R.string.locked_folder_help_top) +
                    "\n" +
                    stringResource(id = R.string.locked_folder_help_bottom),
            showDialog = showHelpDialog
        )
    }

    val isLandscape by rememberDeviceOrientation()
    if (isLandscape) {
        Row (
            modifier = Modifier
                .fillMaxSize(1f)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Column (
                modifier = Modifier
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.locked_folder),
                    contentDescription = "Secure Folder Icon",
                    modifier = Modifier.size(72.dp)
                )

                Text(
                    text = "Secure Folder",
                    fontSize = TextUnit(16f, TextUnitType.Sp)
                )
            }

            Column (
                modifier = Modifier
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                        showHelpDialog.value = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(
                        text = "More Info",
                        fontSize = TextUnit(16f, TextUnitType.Sp)
                    )
                }
            }
        }
    } else {
        Column (
            modifier = Modifier
                .fillMaxSize(1f)
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                    showHelpDialog.value = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(
                    text = "More Info",
                    fontSize = TextUnit(16f, TextUnitType.Sp)
                )
            }
        }
    }
}
