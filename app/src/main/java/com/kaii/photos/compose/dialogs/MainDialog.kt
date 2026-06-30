package com.kaii.photos.compose.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kaii.photos.LocalNavController
import com.kaii.photos.PhotosApplication
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.user_action.ExplanationDialog
import com.kaii.photos.compose.widgets.ExpressiveDialogRow
import com.kaii.photos.compose.widgets.ExpressiveDialogRowWithAction
import com.kaii.photos.compose.widgets.MainDialogUserInfo
import com.kaii.photos.compose.widgets.PreferencesSeparatorText
import com.kaii.photos.compose.widgets.news.NewsPopup
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.file_management.sync.ProgressManager
import com.kaii.photos.helpers.ComponentViewModelScope
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.models.news.NewsViewModelFactory
import com.kaii.photos.permissions.auth.rememberSecureFolderAuthManager
import com.kaii.photos.presentation.main_dialog.AboutLinkItems
import com.kaii.photos.presentation.main_dialog.SettingsItems
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainDialog(
    sheetState: SheetState,
    coroutineScope: CoroutineScope,
    extraSecureFolderEntry: () -> Boolean,
    immichInfo: () -> ImmichBasicInfo,
    modifier: Modifier = Modifier,
    dismiss: suspend () -> Unit
) {
    // remove (weird) drag handle ripple
    CompositionLocalProvider(
        LocalRippleConfiguration provides null
    ) {
        val progressManager = PhotosApplication.appModule.cloudProgressManager
        val navController = LocalNavController.current
        val isLandscape by rememberDeviceOrientation()

        ModalBottomSheet(
            sheetState = sheetState,
            tonalElevation = 16.dp,
            shape = RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            onDismissRequest = {
                coroutineScope.launch {
                    dismiss()
                }
            },
            contentWindowInsets = {
                if (!isLandscape) WindowInsets.systemBars
                else WindowInsets()
            },
            modifier = modifier
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // reset ripple for normal buttons
            CompositionLocalProvider(
                LocalRippleConfiguration provides RippleConfiguration()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .padding(start = 24.dp, end = 24.dp)
                        .clip(RoundedCornerShape(size = 32.dp)),
                    verticalArrangement = Arrangement.spacedBy(
                        space = 4.dp,
                        alignment = Alignment.Top
                    ),
                    horizontalAlignment = Alignment.Start
                ) {
                    item {
                        if (immichInfo().username.isNotBlank()) {
                            MainDialogUserInfo(
                                coroutineScope = coroutineScope,
                                immichInfo = immichInfo,
                                dismiss = {
                                    coroutineScope.launch {
                                        dismiss()
                                    }
                                }
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(space = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.lavender_no_padding),
                                    contentDescription = stringResource(id = R.string.app_name_full),
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(color = MaterialTheme.colorScheme.primary)
                                        .padding(all = 12.dp)
                                )

                                Text(
                                    text = stringResource(id = R.string.app_name_full),
                                    fontSize = TextStylingConstants.LARGE_TEXT_SIZE.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                )
                            }
                        }
                    }

                    if (extraSecureFolderEntry()) {
                        item {
                            val authManager = rememberSecureFolderAuthManager(
                                coroutineScope = coroutineScope,
                                extraAction = {
                                    coroutineScope.launch {
                                        dismiss()
                                    }
                                }
                            )

                            ExpressiveDialogRow(
                                title = stringResource(id = R.string.secure_folder),
                                icon = painterResource(id = R.drawable.secure_folder),
                                position = RowPosition.Bottom,
                                onClick = {
                                    authManager.authenticate()
                                }
                            )
                        }
                    }

                    item {
                        PreferencesSeparatorText(
                            text = stringResource(id = R.string.data_and_backup)
                        )
                    }

                    item {
                        ExpressiveDialogRowWithAction(
                            title = stringResource(id = R.string.data_and_backup),
                            icon = painterResource(id = R.drawable.data),
                            actionIcon = painterResource(id = R.drawable.cloud_upload),
                            position =
                                if (progressManager.state != ProgressManager.State.Idle) RowPosition.Top
                                else RowPosition.Single,
                            onClick = {
                                coroutineScope.launch {
                                    dismiss()
                                    navController.navigate(Screens.Settings.Misc.DataAndBackup)
                                }
                            },
                            onActionClick = {
                                coroutineScope.launch {
                                    dismiss()
                                    navController.navigate(Screens.Immich.Dashboard)
                                }
                            }
                        )
                    }

                    item {
                        AnimatedVisibility(
                            visible = progressManager.state != ProgressManager.State.Idle,
                            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
                        ) {
                            val errorColor = MaterialTheme.colorScheme.errorContainer
                            val containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            val color by animateColorAsState(
                                targetValue =
                                    if (progressManager.state == ProgressManager.State.Error) errorColor
                                    else containerColor
                            )

                            ExpressiveDialogRow(
                                title = stringResource(id = R.string.immich_backup_sync_count, progressManager.currentItems, progressManager.totalItems),
                                icon = painterResource(id = R.drawable.cloud_sync),
                                position = RowPosition.Bottom,
                                containerColor = color,
                                onClick = {
                                    progressManager.dismiss()
                                }.takeIf {
                                    progressManager.state == ProgressManager.State.Error
                                }
                            )
                        }
                    }

                    settingsColumnItems(
                        navController = navController,
                        coroutineScope = coroutineScope,
                        dismiss = dismiss
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
fun LazyListScope.settingsColumnItems(
    navController: NavController,
    coroutineScope: CoroutineScope,
    dismiss: suspend () -> Unit
) {
    item {
        PreferencesSeparatorText(
            text = stringResource(id = R.string.settings)
        )
    }

    itemsIndexed(
        items = SettingsItems.entries
    ) { index, item ->
        ExpressiveDialogRow(
            title = stringResource(id = item.title),
            icon = painterResource(id = item.icon),
            position =
                when (index) {
                    0 -> RowPosition.Top
                    SettingsItems.entries.size - 1 -> RowPosition.Bottom
                    else -> RowPosition.Middle
                },
            onClick = {
                coroutineScope.launch {
                    dismiss()
                    navController.navigate(item.screen)
                }
            }
        )
    }

    item {
        PreferencesSeparatorText(
            stringResource(id = R.string.settings_about_and_updates)
        )
    }

    item {
        ExpressiveDialogRow(
            title = stringResource(id = R.string.settings_about),
            icon = painterResource(id = R.drawable.info),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            position = RowPosition.Top,
            onClick = {
                coroutineScope.launch {
                    dismiss()

                    navController.navigate(Screens.Settings.Misc.AboutPage)
                }
            }
        )
    }

    items(
        items = AboutLinkItems.entries
    ) { item ->
        val context = LocalContext.current

        ExpressiveDialogRow(
            title = stringResource(id = item.title),
            icon = painterResource(id = item.icon),
            enabled = item.enabled,
            containerColor = item.color,
            position = RowPosition.Middle,
            onClick = {
                coroutineScope.launch {
                    dismiss()
                    context.startActivity(item.intent)
                }
            }
        )
    }

    item {
        ExpressiveDialogRow(
            title = stringResource(id = R.string.updates),
            icon = painterResource(id = R.drawable.update),
            position = RowPosition.Bottom,
            onClick = {
                coroutineScope.launch {
                    dismiss()
                    navController.navigate(Screens.Settings.Misc.UpdatePage)
                }
            }
        )
    }

    item {
        PreferencesSeparatorText(
            stringResource(id = R.string.immich_misc)
        )
    }

    item {
        ExpressiveDialogRow(
            title = stringResource(id = R.string.licenses),
            icon = painterResource(id = R.drawable.license),
            position = RowPosition.Top,
            onClick = {
                coroutineScope.launch {
                    dismiss()
                    navController.navigate(Screens.Settings.Misc.LicensesPage)
                }
            }
        )
    }

    item {
        var showPrivacyPolicy by remember { mutableStateOf(false) }
        if (showPrivacyPolicy) {
            ExplanationDialog(
                title = stringResource(id = R.string.privacy_policy_title),
                explanation = stringResource(id = R.string.privacy_policy)
            ) {
                showPrivacyPolicy = false
            }
        }

        ExpressiveDialogRow(
            title = stringResource(id = R.string.privacy_policy_title),
            icon = painterResource(id = R.drawable.privacy_policy),
            position = RowPosition.Middle,
            onClick = {
                showPrivacyPolicy = true
            }
        )
    }

    item {
        val context = LocalContext.current
        var showVersionInfoDialog by remember { mutableStateOf(false) }

        if (showVersionInfoDialog) {
            ComponentViewModelScope(key = "News Section") {
                NewsPopup(
                    viewModel = viewModel(
                        factory = NewsViewModelFactory(context = context),
                        viewModelStoreOwner = LocalViewModelStoreOwner.current!!
                    ),
                    onDismiss = { showVersionInfoDialog = false }
                )
            }
        }

        ExpressiveDialogRow(
            title = stringResource(id = R.string.news),
            icon = painterResource(id = R.drawable.newspaper),
            position = RowPosition.Bottom,
            onClick = {
                showVersionInfoDialog = true
            }
        )
    }

    item {
        PreferencesSeparatorText(
            text = stringResource(id = R.string.debugging_development)
        )
    }

    item {
        ExpressiveDialogRow(
            title = stringResource(id = R.string.debugging),
            icon = painterResource(id = R.drawable.bug_report),
            position = RowPosition.Single,
            onClick = {
                navController.navigate(Screens.Settings.MainPage.Debugging)
            }
        )
    }
}