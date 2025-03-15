package com.kaii.photos.compose

import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.net.Uri
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.kaii.lavender_snackbars.LavenderSnackbarController
import com.kaii.lavender_snackbars.LavenderSnackbarEvents
import com.kaii.photos.LocalNavController
import com.kaii.photos.MainActivity.Companion.applicationDatabase
import com.kaii.photos.MainActivity.Companion.mainViewModel
import com.kaii.photos.R
import com.kaii.photos.datastore.AlbumsList
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.datastore.DefaultTabs
import com.kaii.photos.helpers.AppDirectories
import com.kaii.photos.helpers.DataAndBackupHelper
import com.kaii.photos.helpers.GetDirectoryPermissionAndRun
import com.kaii.photos.helpers.GetPermissionAndRun
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.appRestoredFilesDir
import com.kaii.photos.helpers.appSecureFolderDir
import com.kaii.photos.helpers.baseInternalStorageDirectory
import com.kaii.photos.helpers.copyImageListToPath
import com.kaii.photos.helpers.moveImageToLockedFolder
import com.kaii.photos.helpers.relativePath
import com.kaii.photos.helpers.toRelativePath
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.getMediaStoreDataFromUri
import com.kaii.photos.mediastore.getUriFromAbsolutePath
import com.kaii.photos.mediastore.LAVENDER_FILE_PROVIDER_AUTHORITY
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path

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
    val coroutineScope = rememberCoroutineScope()

    // TODO: move again to Android/data for space purposes
    // moves media from old dir to new dir for secure folder
    var canOpenSecureFolder by remember { mutableStateOf(true) }

    var migrating by remember { mutableStateOf(false) }
    val showExplanationForMigration = remember { mutableStateOf(false) }

    val newFolderDirPermission = remember { mutableStateOf(false) }
    val encryptionDirPermission = remember { mutableStateOf(false) }

    val runEncryptAction = remember { mutableStateOf(false) }
    val uriList = remember { mutableStateListOf<Uri>() }
    val unencryptedFilesList = remember { mutableStateListOf<File>() }

    var rerunMigration by remember { mutableStateOf(false) }

    // migrate from old secure folder dir to new one
    GetDirectoryPermissionAndRun(
        absolutePath = context.appRestoredFilesDir,
        shouldRun = newFolderDirPermission
    ) {
        coroutineScope.launch(Dispatchers.IO) {
			val oldDir = context.getDir(AppDirectories.OldSecureFolder.path, Context.MODE_PRIVATE)
			val oldFiles = oldDir.listFiles()

			if (oldFiles == null || oldFiles.isEmpty()) return@launch

            migrating = true
            canOpenSecureFolder = false

            Log.d(TAG, "Exporting backup of old secure folder items")

            val helper = DataAndBackupHelper()
            val success = helper.exportRawSecureFolderItems(
                context = context,
                secureFolder = oldDir
            )

            if (success) {
                val exportDir = helper.getRawExportDir(context = context)
                mainViewModel.settings.AlbumsList.addToAlbumsList(exportDir.relativePath)

                oldFiles.forEach {
                	Log.d(TAG, "item in old dir ${it.name}")

                	val destination = File(context.appSecureFolderDir, it.name)
                	if (!destination.exists()) {
                		it.copyTo(destination)
                		it.delete()
                	}
                }

                showExplanationForMigration.value = true
            } else {
                LavenderSnackbarController.pushEvent(
                    LavenderSnackbarEvents.MessageEvent(
                        message = "Failed exporting backup!",
                        iconResId = R.drawable.error_2,
                        duration = SnackbarDuration.Long
                    )
                )
            }

            migrating = false
            canOpenSecureFolder = true

            rerunMigration = true
        }
    }

    GetDirectoryPermissionAndRun(
        absolutePath = context.appRestoredFilesDir,
        shouldRun = encryptionDirPermission
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            if (unencryptedFilesList.isEmpty()) return@launch

            migrating = true
            canOpenSecureFolder = false

            val restoredFilesDir = context.appRestoredFilesDir

            val uris = unencryptedFilesList.mapNotNull { file ->
            	val destination = File(restoredFilesDir, file.name)
            	if (!destination.exists()) {
            		file.copyTo(destination)
            	}

                val uri = context.contentResolver.getUriFromAbsolutePath(
                    absolutePath = destination.absolutePath,
                    type =
                        if (Files.probeContentType(Path(destination.absolutePath)).startsWith("image")) MediaType.Image
                        else MediaType.Video
                )

                Log.d(TAG, "Uri for file ${file.name} is $uri")

                uri
            }

            if (uris.isEmpty()) {
            	migrating = false
            	return@launch
            }

            Log.d(TAG, "Starting encryption process...")

            uriList.clear()
            uriList.addAll(uris)

            runEncryptAction.value = true
        }
    }

    GetPermissionAndRun(
        uris = uriList,
        shouldRun = runEncryptAction,
        onGranted = {
            coroutineScope.launch(Dispatchers.IO) {
                val mediaItems = uriList.mapNotNull { uri ->
                    context.contentResolver.getMediaStoreDataFromUri(uri)
                }

                Log.d(TAG, "Creating a backup of the secure folder media...")
                copyImageListToPath(
                    list = mediaItems,
                    context = context,
                    destination = context.appRestoredFilesDir,
                    overrideDisplayName = { displayName ->
                        val extension = displayName.replaceBeforeLast(".", "")

                        val name = displayName.replace(extension, ".backup")
                        Log.d(TAG, "Final name of file is $name")
                        name
                    }
                )
                mainViewModel.settings.AlbumsList.addToAlbumsList(context.appRestoredFilesDir.toRelativePath())

                Log.d(TAG, "Encrypting secure folder media...")
                moveImageToLockedFolder(
                    list = mediaItems,
                    context = context,
                    onDone = {
                        migrating = false
                        canOpenSecureFolder = true
                        showExplanationForMigration.value = true
                    }
                )
            }
        },
        onRejected = {
            coroutineScope.launch {
                LavenderSnackbarController.pushEvent(
                    LavenderSnackbarEvents.MessageEvent(
                        message = "Can't encrypt photos without permission",
                        iconResId = R.drawable.error_2,
                        duration = SnackbarDuration.Long
                    )
                )
            }
        }
    )

    LaunchedEffect(rerunMigration) {
        withContext(Dispatchers.IO) {
            val oldDir = context.getDir(AppDirectories.OldSecureFolder.path, Context.MODE_PRIVATE)
            if (oldDir.listFiles()?.isNotEmpty() == true) {
                newFolderDirPermission.value = true
            }

            while (newFolderDirPermission.value == true) {
            	delay(100)
            }

            val maybeUnencryptedDir = File(context.appSecureFolderDir)
            val maybeUnencryptedDirChildren = maybeUnencryptedDir.listFiles()

            val unencryptedDirChildren = maybeUnencryptedDirChildren?.filter {
                try {
                    val hasIV = applicationDatabase.securedItemEntityDao().getIvFromSecuredPath(it.absolutePath) != null
                    Log.e(TAG, "${it.name} has IV? $hasIV")
                    !hasIV // we want items that don't have an IV, that means they have not been encrypted yet
                } catch (e: Throwable) {
                    Log.e(TAG, "${it.name} has no IV")
                    true
                }
            }

            if (unencryptedDirChildren?.isNotEmpty() == true) {
                unencryptedFilesList.clear()
                unencryptedFilesList.addAll(unencryptedDirChildren.mapNotNull { it })

                encryptionDirPermission.value = true
            }
        }
    }

    if (migrating) {
        LoadingDialog(
            title = "Migrating",
            body = "Backing up and encrypting your photos, hold on..."
        )

        return
    }

    if (showExplanationForMigration.value) {
        ExplanationDialog(
            title = "Migration Notice",
            explanation = "Secure folder is now encrypted! All your photos are now fully safe and untouchable by anyone. \n\nAs a precaution, a copy of your secured photos is now present in an export folder, you can find it in the albums page or under \"Internal Storage/Android/media/com.kaii.photos/LavenderPhotos/Exports\".",
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
            navController.navigate(MultiScreenViewType.LockedFolderView.name)
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
                    "\n\n" +
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
                Button(
                    onClick = {
                        prompt.authenticate(
                            cancellationSignal,
                            context.mainExecutor,
                            promptCallback
                        )
                    },
                    enabled = canOpenSecureFolder
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
                enabled = canOpenSecureFolder
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
