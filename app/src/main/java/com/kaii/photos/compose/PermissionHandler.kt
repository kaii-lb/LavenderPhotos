package com.kaii.photos.compose

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kaii.photos.MainActivity.Companion.mainViewModel
import com.kaii.photos.R
import com.kaii.photos.helpers.CustomMaterialTheme
import com.kaii.photos.helpers.RowPosition

@Composable
fun PermissionHandler(
    continueToApp: MutableState<Boolean>
) {
    Scaffold { innerPadding ->
        val localConfig = LocalConfiguration.current
        var isLandscape by remember { mutableStateOf(localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) }

        LaunchedEffect(localConfig) {
            isLandscape = localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        }

        val safeDrawingPadding = if (isLandscape) {
            val safeDrawing = WindowInsets.safeDrawing.asPaddingValues()

            val layoutDirection = LocalLayoutDirection.current
            val left = safeDrawing.calculateStartPadding(layoutDirection)
            val right = safeDrawing.calculateEndPadding(layoutDirection)

            Pair(left, right)
        } else {
            Pair(0.dp, 0.dp)
        }

        Column(
            modifier = Modifier
                .padding(
                    safeDrawingPadding.first,
                    innerPadding.calculateTopPadding() + 8.dp,
                    safeDrawingPadding.second,
                    innerPadding.calculateBottomPadding()
                )
                .fillMaxSize()
                .background(CustomMaterialTheme.colorScheme.background)
                .padding(16.dp, 8.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val context = LocalContext.current

            var onGrantPermissionClicked by remember { mutableStateOf({}) }

            val showPermDeniedDialog = remember { mutableStateOf(false) }
            val showExplanationDialog = remember { mutableStateOf(false) }
            var whyButtonExplanation by rememberSaveable { mutableStateOf("") }

            if (showPermDeniedDialog.value) {
                PermissionDeniedDialog(showDialog = showPermDeniedDialog, showExplanationDialog = showExplanationDialog) {
                    onGrantPermissionClicked()
                }
            }

            if (showExplanationDialog.value) {
                ExplanationDialog(
                    showDialog = showExplanationDialog,
                    showPreviousDialog = showPermDeniedDialog,
                    explanation = whyButtonExplanation
                )
            }

            Text(
                text = "Permissions",
                fontSize = TextUnit(22f, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                color = CustomMaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(32.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(
                    space = 4.dp,
                    alignment = Alignment.Top
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    item {
                        // we can ignore READ_MEDIA_VIDEO since requesting both shows a single dialog
                        val readMediaImageLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.RequestPermission()
                        ) { granted ->
                            mainViewModel.onPermissionResult(
                                permission = Manifest.permission.READ_MEDIA_IMAGES,
                                isGranted = granted
                            )

                            showPermDeniedDialog.value = !granted
                        }

                        val appDetailsLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.StartActivityForResult()
                        ) { _ ->
                            val granted = context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED

                            mainViewModel.onPermissionResult(
                                permission = Manifest.permission.READ_MEDIA_IMAGES,
                                isGranted = granted
                            )

                            showPermDeniedDialog.value = !granted
                        }

                        PermissionButton(
                            name = "Read Media",
                            description = "Allow Lavender Photos to discover photos and videos on the device",
                            position = RowPosition.Top,
                            granted = !mainViewModel.permissionQueue.contains(Manifest.permission.READ_MEDIA_IMAGES)
                        ) {
                            readMediaImageLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)

                            whyButtonExplanation = Explanations.READ_MEDIA

                            onGrantPermissionClicked = {
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                )

                                appDetailsLauncher.launch(intent)
                            }
                        }
                    }
                } else {
                    item {
                        val readExternalStorageLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.RequestPermission()
                        ) { granted ->
                            mainViewModel.onPermissionResult(
                                permission = Manifest.permission.READ_EXTERNAL_STORAGE,
                                isGranted = granted
                            )

                            showPermDeniedDialog.value = !granted
                        }

                        val appDetailsLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.StartActivityForResult()
                        ) { _ ->
                            val granted = context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

                            mainViewModel.onPermissionResult(
                                permission = Manifest.permission.READ_EXTERNAL_STORAGE,
                                isGranted = granted
                            )

                            showPermDeniedDialog.value = !granted
                        }

                        PermissionButton(
                            name = "Read External Storage",
                            description = "Allow Lavender Photos to discover photos and videos on the device",
                            position = RowPosition.Top,
                            granted = !mainViewModel.permissionQueue.contains(Manifest.permission.READ_EXTERNAL_STORAGE)
                        ) {
                            readExternalStorageLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)

                            whyButtonExplanation = Explanations.READ_MEDIA

                            onGrantPermissionClicked = {
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                )

                                appDetailsLauncher.launch(intent)
                            }
                        }
                    }
                }

                item {
                    val manageExternalStorageLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { _ ->
                        val granted = Environment.isExternalStorageManager()

                        mainViewModel.onPermissionResult(
                            permission = Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                            isGranted = granted
                        )

                        showPermDeniedDialog.value = !granted
                    }

                    PermissionButton(
                        name = "Manage All Files",
                        description = "Allow read/write access to all files, used to trash, delete and edit media",
                        position = RowPosition.Bottom,
                        granted = !mainViewModel.permissionQueue.contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                    ) {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        manageExternalStorageLauncher.launch(intent)

                        whyButtonExplanation = Explanations.MANAGE_ALL_FILES

                        onGrantPermissionClicked = {
                            manageExternalStorageLauncher.launch(intent)
                        }
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    item {
                        val manageMediaLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.StartActivityForResult()
                        ) { _ ->
                            val granted = MediaStore.canManageMedia(context)

                            mainViewModel.onPermissionResult(
                                permission = Manifest.permission.MANAGE_MEDIA,
                                isGranted = granted
                            )
                        }

                        PermissionButton(
                            name = "Manage Media",
                            description = "Optional permission. Is used for faster trash/delete functionality",
                            position = RowPosition.Middle,
                            granted = !mainViewModel.permissionQueue.contains(Manifest.permission.MANAGE_MEDIA)
                        ) {
                            val intent = Intent(Settings.ACTION_REQUEST_MANAGE_MEDIA)
                            manageMediaLauncher.launch(intent)

                            whyButtonExplanation = Explanations.MANAGE_MEDIA

                            onGrantPermissionClicked = {
                                manageMediaLauncher.launch(intent)
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .height(64.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        (context as Activity).finish()
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                ) {
                    Text(text = "Exit")
                }

                Button(
                    onClick = {
                        continueToApp.value = true
                    },
                    enabled = mainViewModel.checkCanPass(),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                ) {
                    Text(text = "Continue")
                }
            }
        }
    }
}

@Composable
fun PermissionButton(
    name: String,
    description: String,
    position: RowPosition,
    granted: Boolean = false,
    onClick: () -> Unit
) {
    val (shape, _) = getDefaultShapeSpacerForPosition(position, 32.dp)

    val clickModifier = if (!granted) Modifier.clickable { onClick() } else Modifier

    Box(
        modifier = Modifier
            .fillMaxWidth(1f)
            .height(104.dp)
            .clip(shape)
            .background(if (!granted) CustomMaterialTheme.colorScheme.surfaceContainer else CustomMaterialTheme.colorScheme.primary)
            .then(clickModifier)
            .padding(16.dp, 12.dp)
    ) {
        Column(
            modifier = Modifier
                .wrapContentWidth()
                .fillMaxHeight(1f)
                .align(Alignment.CenterStart),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = name,
                color = if (!granted) CustomMaterialTheme.colorScheme.onSurface else CustomMaterialTheme.colorScheme.onPrimary,
                fontSize = TextUnit(18f, TextUnitType.Sp),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                color = if (!granted) CustomMaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else CustomMaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                fontSize = TextUnit(14f, TextUnitType.Sp),
                fontWeight = FontWeight.Bold
            )
        }

        if (granted) {
            Column(
                modifier = Modifier
                    .fillMaxHeight(1f)
                    .width(32.dp)
                    .background(CustomMaterialTheme.colorScheme.primary)
                    .align(Alignment.CenterEnd),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.file_is_selected_foreground),
                    contentDescription = name,
                    tint = CustomMaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(28.dp)
                )
            }
        }
    }
}

@Composable
fun PermissionDeniedDialog(
    showDialog: MutableState<Boolean>,
    showExplanationDialog: MutableState<Boolean>,
    onGrantPermissionClicked: () -> Unit
) {
    Dialog(
        onDismissRequest = {
            showDialog.value = false
        },
        properties = DialogProperties(
            dismissOnClickOutside = true,
            dismissOnBackPress = true
        ),
    ) {
        Column(
            modifier = Modifier
                .wrapContentSize()
                .clip(RoundedCornerShape(32.dp))
                .background(CustomMaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Necessary Permission",
                fontSize = TextUnit(18f, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                color = CustomMaterialTheme.colorScheme.onBackground,
                modifier = Modifier.wrapContentSize()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "This permission is necessary for app functionality",
                fontSize = TextUnit(14f, TextUnitType.Sp),
                color = CustomMaterialTheme.colorScheme.onBackground,
                modifier = Modifier.wrapContentSize()
            )

            Spacer(modifier = Modifier.height(32.dp))

            PermissionDeniedDialogButton(
                text = "Grant Permission",
                color = CustomMaterialTheme.colorScheme.primary,
                textColor = CustomMaterialTheme.colorScheme.onPrimary,
                position = RowPosition.Top
            ) {
                showDialog.value = false
                onGrantPermissionClicked()
            }

            PermissionDeniedDialogButton(
                text = "Why?",
                color = CustomMaterialTheme.colorScheme.surfaceContainer,
                textColor = CustomMaterialTheme.colorScheme.onSurface,
                position = RowPosition.Middle
            ) {
                showExplanationDialog.value = true
            }

            PermissionDeniedDialogButton(
                text = "Dismiss",
                color = CustomMaterialTheme.colorScheme.surfaceContainer,
                textColor = CustomMaterialTheme.colorScheme.onSurface,
                position = RowPosition.Bottom
            ) {
                showDialog.value = false
            }
        }
    }
}

@Composable
fun PermissionDeniedDialogButton(
    text: String,
    color: Color,
    textColor: Color,
    position: RowPosition,
    onClick: () -> Unit
) {
    val (shape, spacerHeight) = getDefaultShapeSpacerForPosition(position, cornerRadius = 24.dp, innerCornerRadius = 4.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth(1f)
            .height(48.dp)
            .clip(shape)
            .background(color)
            .clickable {
                onClick()
            }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = TextUnit(14f, TextUnitType.Sp)
        )
    }

    Spacer(modifier = Modifier.height(spacerHeight))
}

private object Explanations {
    const val READ_MEDIA = "This permission is needed to find photos and videos on the device. Lavender Photos is very strict with what files it reads, and never shares or exploits this info."
    const val MANAGE_ALL_FILES =
        "This permission is needed to modify or delete photos and videos on this device, specifically moving them to secure folder and for some trash functionality. This might change in the future.\nLavender Photos will never touch your files without an explicit action being done (eg: clicking the delete button on an image)"
    const val MANAGE_MEDIA = "This permission is optional, but is highly recommended. Manage Media permission allows Lavender Photos to use Android's Content Resolver API to trash/delete/move/copy media, which makes the process much smoother and more interoperable with other apps."
}

@Composable
fun ExplanationDialog(
    showDialog: MutableState<Boolean>,
    showPreviousDialog: MutableState<Boolean>? = null,
    explanation: String
) {
    showPreviousDialog?.value = false

    Dialog(
        onDismissRequest = {
            showDialog.value = false
        },
        properties = DialogProperties(
            dismissOnClickOutside = true,
            dismissOnBackPress = true
        ),
    ) {
        Column(
            modifier = Modifier
                .wrapContentSize()
                .clip(RoundedCornerShape(32.dp))
                .background(CustomMaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Permission Explanation",
                fontSize = TextUnit(18f, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                color = CustomMaterialTheme.colorScheme.onBackground,
                modifier = Modifier.wrapContentSize()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = explanation,
                fontSize = TextUnit(14f, TextUnitType.Sp),
                color = CustomMaterialTheme.colorScheme.onBackground,
                modifier = Modifier.wrapContentSize()
            )

            Spacer(modifier = Modifier.height(24.dp))

            PermissionDeniedDialogButton(
                text = "Okay",
                color = CustomMaterialTheme.colorScheme.primary,
                textColor = CustomMaterialTheme.colorScheme.onPrimary,
                position = RowPosition.Single
            ) {
                showDialog.value = false
                showPreviousDialog?.value = true
            }
        }
    }
}
