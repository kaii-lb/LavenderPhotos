package com.kaii.photos.compose

import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import android.util.Log
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.lavender_snackbars.LavenderSnackbarController
import com.kaii.lavender_snackbars.LavenderSnackbarEvents
import com.kaii.photos.LocalNavController
import com.kaii.photos.MainActivity.Companion.applicationDatabase
import com.kaii.photos.MainActivity.Companion.mainViewModel
import com.kaii.photos.R
import com.kaii.photos.datastore.AlbumsList
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.datastore.DefaultTabs
import com.kaii.photos.datastore.Versions
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.appRestoredFilesDir
import com.kaii.photos.helpers.appSecureFolderDir
import com.kaii.photos.helpers.baseInternalStorageDirectory
import com.kaii.photos.helpers.GetDirectoryPermissionAndRun
import com.kaii.photos.helpers.moveImageToLockedFolder
import com.kaii.photos.mediastore.getUriFromAbsolutePath
import com.kaii.photos.mediastore.getMediaStoreDataFromUri
import com.kaii.photos.mediastore.MediaType
import kotlin.io.path.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files

private const val TAG = "LOCKED_FOLDER_ENTRY_VIEW"

@Composable
fun LockedFolderEntryView(
    currentView: MutableState<BottomBarTab>
) {
    val navController = LocalNavController.current

    BackHandler(
        enabled = currentView.value == DefaultTabs.TabTypes.secure && navController.currentBackStackEntry?.destination?.route == MultiScreenViewType.MainScreen.name
    ) {
        currentView.value = DefaultTabs.TabTypes.photos
    }

    val context = LocalContext.current
    val cancellationSignal = CancellationSignal()

    // TODO: move again to Android/data for space purposes
    // moves media from old dir to new dir for secure folder
    var migrating by remember { mutableStateOf(false) }
    var launchSecureFolder by remember { mutableStateOf(false) }
    val showExplanationForMigration = remember { mutableStateOf(false) }
    val shouldMigrate by mainViewModel.settings.Versions.getShouldMigrateToEncryptedSecurePhotos().collectAsStateWithLifecycle(initialValue = false)
    var continueToEncryption by remember { mutableStateOf(false) }
    val getDirPermission = remember { mutableStateOf(false) }

    LaunchedEffect(launchSecureFolder) {
        if (launchSecureFolder) navController.navigate(MultiScreenViewType.LockedFolderView.name)
    }

   	GetDirectoryPermissionAndRun(
   		absolutePath = context.appRestoredFilesDir,
   		shouldRun = getDirPermission
   	) {
   		continueToEncryption = true
   	}

    LaunchedEffect(shouldMigrate, continueToEncryption) {
        withContext(Dispatchers.IO) {
            if (!shouldMigrate) return@withContext

        	val restoredFilesDir = context.appRestoredFilesDir
            val oldDir = context.getDir("locked_folder", Context.MODE_PRIVATE)
            val children = oldDir.listFiles()

            // migrate from old secure folder dir
            if (children?.isNotEmpty() == true) {
            	getDirPermission.value = true

            	if (continueToEncryption) {
	                migrating = true
	                val newDir = context.appSecureFolderDir

	                Log.d(TAG, "migrating from $oldDir to $newDir")
	                children.forEach { file ->
	                    val newPath = newDir + "/" + file.name
	                    val destination = File(newPath)
	                    val restoredPath = restoredFilesDir + "/" + file.name
	                    val restoredFile = File(restoredPath)

	                    if (!restoredFile.exists()) {
	                    	file.copyTo(restoredFile)
	                    }

	                    if (!destination.exists()) {
	                        file.copyTo(destination)
	                        file.delete()
	                    }
	                }

	                migrating = false
	                showExplanationForMigration.value = true
            	}
            }

            val maybeUnencryptedDir = File(context.appSecureFolderDir)
            val maybeUnencryptedDirChildren = maybeUnencryptedDir.listFiles()

            val unencryptedDirChildren = maybeUnencryptedDirChildren?.filter {
                try {
                    applicationDatabase.securedItemEntityDao().getIvFromSecuredPath(it.absolutePath) == null
                } catch (e: Throwable) {
                    Log.e(TAG, "${it.name} has no IV")
                    true
                }
            }

			if (unencryptedDirChildren?.isNotEmpty() == true) {
				getDirPermission.value = true

	            if (continueToEncryption) {
	                Log.d(TAG, "encrypting previously unencrypted photos")
	                migrating = true

	                unencryptedDirChildren.forEach { file ->
	                    val newPath = restoredFilesDir + "/" + file.name
	                    val destination = File(newPath)

	                    if (!destination.exists()) {
	                        file.copyTo(destination)
	                        file.delete()
	                    }

						val uri = context.contentResolver.getUriFromAbsolutePath(
							absolutePath = destination.absolutePath,
							type =
								if (Files.probeContentType(Path(destination.absolutePath)).startsWith("image")) MediaType.Image
								else MediaType.Video
						)

						uri?.let {
							context.contentResolver.getMediaStoreDataFromUri(it)?.let { mediaItem ->
			                    moveImageToLockedFolder(
			                    	list = listOf(
			                    		mediaItem
			                    	),
			                    	context = context,
			                    	onDone = {
			                    		migrating = false
			                    	}
			                    )
							}
						}

		                migrating = false
		                showExplanationForMigration.value = true
	                }
	            }
			}

            mainViewModel.settings.Versions.setShouldMigrateToEncryptedSecurePhotos(false)
            mainViewModel.settings.AlbumsList.addToAlbumsList(
                restoredFilesDir.replace(baseInternalStorageDirectory, "")
            )
        }
    }

    if (migrating) {
        LoadingDialog(
            title = "Migrating",
            body = "Migrating to encrypted photos, a copy can be found in \"Restored Files\" album"
        )

        return
    }

    if (showExplanationForMigration.value) {
        ExplanationDialog(
            title = "Migration Notice",
            explanation = "Secure folder is now encrypted! All your photos are now fully safe and untouchable by anyone. For safety reasons, a copy of your secured photos is now present in \"Restored Files\".",
            showDialog = showExplanationForMigration
        )
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
    if (showHelpDialog.value) {
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
        Row(
            modifier = Modifier
                .fillMaxSize(1f)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Column(
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

            Column(
                modifier = Modifier
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val coroutineScope = rememberCoroutineScope()

                Button(
                    onClick = {
                        if (!shouldMigrate) {
                            prompt.authenticate(
                                cancellationSignal,
                                context.mainExecutor,
                                promptCallback
                            )
                        } else {
                            coroutineScope.launch {
                                LavenderSnackbarController.pushEvent(
                                    LavenderSnackbarEvents.MessageEvent(
                                        message = "Migrating to encrypted photos, please wait",
                                        iconResId = R.drawable.locked_folder,
                                        duration = SnackbarDuration.Short
                                    )
                                )
                            }
                        }
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
        Column(
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
