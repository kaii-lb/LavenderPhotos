package com.kaii.photos.compose.pages

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.ExplanationDialog
import com.kaii.photos.compose.dialogs.getDefaultShapeSpacerForPosition
import com.kaii.photos.compose.widgets.PreferencesRow
import com.kaii.photos.compose.widgets.PreferencesSeparatorText
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.models.permissions.PermissionsViewModel
import com.kaii.photos.permissions.StartupManager

@Composable
fun PermissionHandler(
    startupManager: StartupManager,
    viewModel: PermissionsViewModel
) {
    Scaffold { innerPadding ->
        val isLandscape by rememberDeviceOrientation()

        val safeDrawingPadding = if (isLandscape) {
            val safeDrawing = WindowInsets.safeDrawing.asPaddingValues()

            val layoutDirection = LocalLayoutDirection.current
            val left = safeDrawing.calculateStartPadding(layoutDirection)
            val right = safeDrawing.calculateEndPadding(layoutDirection)

            Pair(left, right)
        } else {
            Pair(0.dp, 0.dp)
        }

        Row(
            modifier = Modifier
                .semantics {
                    testTagsAsResourceId = true
                }
                .padding(
                    safeDrawingPadding.first,
                    innerPadding.calculateTopPadding() + 8.dp,
                    safeDrawingPadding.second,
                    innerPadding.calculateBottomPadding()
                )
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp, 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val context = LocalContext.current

            var onGrantPermissionClicked by remember { mutableStateOf({}) }

            val showPermDeniedDialog = remember { mutableStateOf(false) }
            val showExplanationDialog = remember { mutableStateOf(false) }
            var whyButtonExplanation by rememberSaveable { mutableStateOf("") }

            if (showPermDeniedDialog.value) {
                PermissionDeniedDialog(
                    showExplanationDialog = showExplanationDialog,
                    onGrantPermissionClicked = onGrantPermissionClicked,
                    onDismiss = {
                        showPermDeniedDialog.value = false
                    }
                )
            }

            if (showExplanationDialog.value) {
                ExplanationDialog(
                    title = stringResource(id = R.string.permissions_explanation),
                    explanation = whyButtonExplanation,
                    showPreviousDialog = showPermDeniedDialog
                ) {
                    showExplanationDialog.value = false
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.permissions),
                    fontSize = TextUnit(22f, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
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
                    item {
                        PreferencesSeparatorText(
                            text = stringResource(id = R.string.permissions)
                        )
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        item {
                            val readMediaImageLauncher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.RequestPermission()
                            ) { granted ->
                                startupManager.onPermissionResult(
                                    permission = Manifest.permission.READ_MEDIA_IMAGES,
                                    isGranted = granted
                                )

                                showPermDeniedDialog.value = !granted
                            }

                            val appDetailsLauncher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.StartActivityForResult()
                            ) { _ ->
                                val granted =
                                    context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED

                                startupManager.onPermissionResult(
                                    permission = Manifest.permission.READ_MEDIA_IMAGES,
                                    isGranted = granted
                                )

                                showPermDeniedDialog.value = !granted
                            }

                            val resources = LocalResources.current
                            PermissionButton(
                                name = stringResource(id = R.string.permissions_read_images),
                                description = stringResource(id = R.string.permissions_read_images_desc),
                                position = RowPosition.Top,
                                granted = startupManager.permissionGranted(permission = Manifest.permission.READ_MEDIA_IMAGES),
                                modifier = Modifier
                                    .testTag("read_image_permission")
                            ) {
                                readMediaImageLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)

                                whyButtonExplanation = resources.getString(Explanations.READ_MEDIA)

                                onGrantPermissionClicked = {
                                    val intent = Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null)
                                    )

                                    appDetailsLauncher.launch(intent)
                                }
                            }
                        }

                        item {
                            val readMediaVideoLauncher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.RequestPermission()
                            ) { granted ->
                                startupManager.onPermissionResult(
                                    permission = Manifest.permission.READ_MEDIA_VIDEO,
                                    isGranted = granted
                                )

                                showPermDeniedDialog.value = !granted
                            }

                            val appDetailsLauncher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.StartActivityForResult()
                            ) { _ ->
                                val granted =
                                    context.checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED

                                startupManager.onPermissionResult(
                                    permission = Manifest.permission.READ_MEDIA_VIDEO,
                                    isGranted = granted
                                )

                                showPermDeniedDialog.value = !granted
                            }

                            val resources = LocalResources.current
                            PermissionButton(
                                name = stringResource(id = R.string.permissions_read_videos),
                                description = stringResource(id = R.string.permissions_read_videos_desc),
                                position = RowPosition.Middle,
                                granted = startupManager.permissionGranted(Manifest.permission.READ_MEDIA_VIDEO),
                                modifier = Modifier
                                    .testTag("read_video_permission")
                            ) {
                                readMediaVideoLauncher.launch(Manifest.permission.READ_MEDIA_VIDEO)

                                whyButtonExplanation = resources.getString(Explanations.READ_MEDIA)

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
                                startupManager.onPermissionResult(
                                    permission = Manifest.permission.READ_EXTERNAL_STORAGE,
                                    isGranted = granted
                                )

                                showPermDeniedDialog.value = !granted
                            }

                            val appDetailsLauncher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.StartActivityForResult()
                            ) { _ ->
                                val granted =
                                    context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

                                startupManager.onPermissionResult(
                                    permission = Manifest.permission.READ_EXTERNAL_STORAGE,
                                    isGranted = granted
                                )

                                showPermDeniedDialog.value = !granted
                            }

                            val resources = LocalResources.current
                            PermissionButton(
                                name = stringResource(id = R.string.permissions_read_external_storage),
                                description = stringResource(id = R.string.permissions_read_external_storage_desc),
                                position = RowPosition.Top,
                                granted = startupManager.permissionGranted(permission = Manifest.permission.READ_EXTERNAL_STORAGE)
                            ) {
                                readExternalStorageLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)

                                whyButtonExplanation = resources.getString(Explanations.READ_MEDIA)

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

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        item {
                            val manageMediaLauncher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.StartActivityForResult()
                            ) { _ ->
                                val granted = MediaStore.canManageMedia(context)

                                startupManager.onPermissionResult(
                                    permission = Manifest.permission.MANAGE_MEDIA,
                                    isGranted = granted
                                )

                                viewModel.setIsMediaManager(granted)
                            }

                            val resources = LocalResources.current
                            PermissionButton(
                                name = stringResource(id = R.string.permissions_manage_media),
                                description = stringResource(id = R.string.permissions_manage_media_desc),
                                position = RowPosition.Bottom,
                                granted = startupManager.permissionGranted(permission = Manifest.permission.MANAGE_MEDIA)
                            ) {
                                val intent = Intent(Settings.ACTION_REQUEST_MANAGE_MEDIA)
                                manageMediaLauncher.launch(intent)

                                whyButtonExplanation = resources.getString(Explanations.MANAGE_MEDIA)

                                onGrantPermissionClicked = {
                                    manageMediaLauncher.launch(intent)
                                }
                            }
                        }
                    }

                    item {
                        PreferencesSeparatorText(
                            text = stringResource(id = R.string.permissions_other_info)
                        )
                    }

                    item {
                        PreferencesRow(
                            title = stringResource(id = R.string.permissions_install_packages),
                            summary = stringResource(id = R.string.permissions_install_packages_desc),
                            iconResID = R.drawable.error_2,
                            position = RowPosition.Single,
                            backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (!isLandscape) {
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
                            Text(text = stringResource(id = R.string.editing_exit))
                        }

                        Button(
                            onClick = {
                                viewModel.launch {
                                    startupManager.checkState()
                                }
                            },
                            enabled = startupManager.checkPermissions(),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .testTag("continue_button")
                        ) {
                            Text(text = stringResource(id = R.string.permissions_continue))
                        }
                    }
                }
            }

            if (isLandscape) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            viewModel.launch {
                                startupManager.checkState()
                            }
                        },
                        enabled = startupManager.checkPermissions(),
                        modifier = Modifier
                            .testTag("continue_button")
                    ) {
                        Text(text = stringResource(id = R.string.permissions_continue))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FilledTonalButton(
                        onClick = {
                            (context as Activity).finish()
                        }
                    ) {
                        Text(text = stringResource(id = R.string.editing_exit))
                    }
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
    modifier: Modifier = Modifier,
    granted: Boolean = false,
    onClick: () -> Unit
) {
    val (shape, _) = getDefaultShapeSpacerForPosition(position, 32.dp)

    val clickModifier = if (!granted) Modifier.clickable { onClick() } else Modifier

    val color by animateColorAsState(
        targetValue = if (!granted) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.primary,
        animationSpec = AnimationConstants.expressiveSpring()
    )

    Box(
        modifier = modifier
            .fillMaxWidth(1f)
            .height(104.dp)
            .clip(shape)
            .background(color)
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
                color = if (!granted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary,
                fontSize = TextUnit(18f, TextUnitType.Sp),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                color = if (!granted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onPrimary.copy(
                    alpha = 0.8f
                ),
                fontSize = TextUnit(14f, TextUnitType.Sp),
                fontWeight = FontWeight.Bold
            )
        }

        AnimatedVisibility(
            visible = granted,
            enter = scaleIn(animationSpec = AnimationConstants.expressiveSpring()) + fadeIn(),
            exit = scaleOut(animationSpec = AnimationConstants.expressiveSpring()) + fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight(1f)
                    .width(32.dp)
                    .background(MaterialTheme.colorScheme.primary),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.checkmark_thin),
                    contentDescription = name,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(28.dp)
                )
            }
        }
    }
}

@Composable
fun PermissionDeniedDialog(
    showExplanationDialog: MutableState<Boolean>,
    onGrantPermissionClicked: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = true,
            dismissOnBackPress = true
        ),
    ) {
        Column(
            modifier = Modifier
                .wrapContentSize()
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.permissions_necessary),
                fontSize = TextUnit(18f, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.wrapContentSize()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(id = R.string.permissions_necessary_desc),
                fontSize = TextUnit(14f, TextUnitType.Sp),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.wrapContentSize()
            )

            Spacer(modifier = Modifier.height(32.dp))

            FullWidthDialogButton(
                text = stringResource(id = R.string.permissions_grant_permissions),
                color = MaterialTheme.colorScheme.primary,
                textColor = MaterialTheme.colorScheme.onPrimary,
                position = RowPosition.Top
            ) {
                onGrantPermissionClicked()
                onDismiss()
            }

            FullWidthDialogButton(
                text = stringResource(id = R.string.permissions_why),
                color = MaterialTheme.colorScheme.surfaceContainer,
                textColor = MaterialTheme.colorScheme.onSurface,
                position = RowPosition.Middle
            ) {
                showExplanationDialog.value = true
            }

            FullWidthDialogButton(
                text = stringResource(id = R.string.permissions_dismiss),
                color = MaterialTheme.colorScheme.surfaceContainer,
                textColor = MaterialTheme.colorScheme.onSurface,
                position = RowPosition.Bottom,
                onClick = onDismiss
            )
        }
    }
}

@Composable
fun FullWidthDialogButton(
    text: String,
    color: Color,
    textColor: Color,
    position: RowPosition,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val (shape, spacerHeight) = getDefaultShapeSpacerForPosition(
        position,
        cornerRadius = 32.dp,
        innerCornerRadius = 8.dp
    )

    Row(
        modifier = modifier
            .fillMaxWidth(1f)
            .height(48.dp)
            .clip(shape)
            .background(
                if (enabled) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
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
    val READ_MEDIA = R.string.permissions_read_media_explanation
    val MANAGE_MEDIA = R.string.permissions_manage_media_explanation
}
