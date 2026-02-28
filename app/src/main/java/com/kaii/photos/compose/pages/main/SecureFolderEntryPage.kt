package com.kaii.photos.compose.pages.main

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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.ExplanationDialog
import com.kaii.photos.compose.dialogs.LoadingDialog
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.di.appModule
import com.kaii.photos.helpers.appRestoredFilesDir
import com.kaii.photos.permissions.auth.rememberSecureFolderAuthManager
import com.kaii.photos.permissions.files.rememberDirectoryPermissionManager
import com.kaii.photos.permissions.files.rememberFilePermissionManager
import com.kaii.photos.permissions.secure_folder.rememberSecureFolderManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SecureFolderEntryPage() {
    val context = LocalContext.current

    // moves media from old dir to new dir for secure folder
    var migrating by remember { mutableStateOf(false) }
    var canOpenSecureFolder by remember { mutableStateOf(true) }
    var showExplanationForMigration by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val resources = LocalResources.current

    val secureFolderManager = rememberSecureFolderManager()

    val migrateEncryptedFilePM = rememberFilePermissionManager(
        onGranted = {
            context.appModule.scope.launch {
                secureFolderManager.migrateFromUnencrypted {
                    migrating = false
                    canOpenSecureFolder = true
                }
            }
        }
    )

    val migrateUnencryptedDirectoryPM = rememberDirectoryPermissionManager(
        onGranted = {
            context.appModule.scope.launch {
                migrating = true
                canOpenSecureFolder = false

                secureFolderManager.setupMigrationFromUnencrypted()
                migrateEncryptedFilePM.get(uris = secureFolderManager.uris)
            }
        },
        onRejected = {
            coroutineScope.launch {
                LavenderSnackbarController.pushEvent(
                    LavenderSnackbarEvents.MessageEvent(
                        message = resources.getString(R.string.secure_encryption_failed_no_permission),
                        icon = R.drawable.error_2,
                        duration = SnackbarDuration.Long
                    )
                )
            }
        }
    )

    val migrateOldDirectoryPM = rememberDirectoryPermissionManager(
        onGranted = {
            context.appModule.scope.launch {
                migrating = true
                canOpenSecureFolder = false

                secureFolderManager.migrateFromOldDirectory()

                migrating = false
                canOpenSecureFolder = true
            }
        }
    )

    LaunchedEffect(Unit) {
        if (secureFolderManager.needsMigrationFromOld || secureFolderManager.needsMigrationFromUnencrypted()) {
            showExplanationForMigration = true
        }
    }

    if (migrating) {
        LoadingDialog(
            title = stringResource(id = R.string.secure_migrating),
            body = stringResource(id = R.string.secure_migrating_desc)
        )

        return
    }

    if (showExplanationForMigration) {
        ExplanationDialog(
            title = stringResource(id = R.string.secure_migrating_notice),
            explanation = stringResource(id = R.string.secure_migrating_notice_desc)
        ) {
            context.appModule.scope.launch {
                if (secureFolderManager.needsMigrationFromOld) {
                    migrateOldDirectoryPM.start(directories = setOf(context.appRestoredFilesDir))
                } else {
                    migrateUnencryptedDirectoryPM.start(directories = setOf(context.appRestoredFilesDir))
                }
            }

            showExplanationForMigration = false
        }
    }


    var showHelpDialog by remember { mutableStateOf(false) }
    if (showHelpDialog) {
        ExplanationDialog(
            title = stringResource(id = R.string.secure_folder),
            explanation = stringResource(id = R.string.locked_folder_help_top) +
                    "\n\n" +
                    stringResource(id = R.string.locked_folder_help_bottom)
        ) {
            showHelpDialog = false
        }
    }

    val authManager = rememberSecureFolderAuthManager()
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
                    painter = painterResource(id = R.drawable.secure_folder),
                    contentDescription = stringResource(id = R.string.secure_folder),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(72.dp)
                )

                Text(
                    text = stringResource(id = R.string.secure_folder),
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
                        authManager.authenticate()
                    },
                    enabled = canOpenSecureFolder,
                    shapes = ButtonDefaults.shapes(
                        shape = MaterialShapes.Circle.toShape(),
                        pressedShape = MaterialShapes.Square.toShape()
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.secure_unlock_short),
                        fontSize = TextUnit(16f, TextUnitType.Sp)
                    )
                }

                Button(
                    onClick = {
                        showHelpDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.more_info),
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
                painter = painterResource(id = R.drawable.secure_folder),
                contentDescription = stringResource(id = R.string.secure_folder),
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    authManager.authenticate()
                },
                enabled = canOpenSecureFolder,
                shapes = ButtonDefaults.shapes(
                    shape = ButtonDefaults.shape,
                    pressedShape = ButtonDefaults.pressedShape
                )
            ) {
                Text(
                    text = stringResource(id = R.string.secure_unlock_short),
                    fontSize = TextUnit(16f, TextUnitType.Sp)
                )
            }

            TextButton(
                onClick = {
                    showHelpDialog = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shapes = ButtonDefaults.shapes(
                    shape = ButtonDefaults.shape,
                    pressedShape = ButtonDefaults.pressedShape
                )
            ) {
                Text(
                    text = stringResource(id = R.string.more_info),
                    fontSize = TextUnit(16f, TextUnitType.Sp)
                )
            }
        }
    }
}