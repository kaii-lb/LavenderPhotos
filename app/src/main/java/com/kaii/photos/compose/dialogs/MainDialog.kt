package com.kaii.photos.compose.dialogs

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kaii.lavender.immichintegration.state_managers.LoginState
import com.kaii.lavender.immichintegration.state_managers.LoginStateManager
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.ExpressiveDialogRow
import com.kaii.photos.compose.widgets.ExpressiveDialogRowWithAction
import com.kaii.photos.compose.widgets.MainDialogUserInfo
import com.kaii.photos.compose.widgets.PreferencesSeparatorText
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.permissions.auth.rememberSecureFolderAuthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "com.kaii.photos.compose.dialogs.MainDialog"

private enum class SettingsItems(
    val title: Int,
    val icon: Int,
    val screen: Screens
) {
    General(
        title = R.string.settings_general,
        icon = R.drawable.settings,
        screen = Screens.Settings.MainPage.General
    ),

    PrivacyAndSecurity(
        title = R.string.settings_privacy,
        icon = R.drawable.shield_lock,
        screen = Screens.Settings.MainPage.PrivacyAndSecurity
    ),

    LookAndFeel(
        title = R.string.settings_look_and_feel,
        icon = R.drawable.paintbrush,
        screen = Screens.Settings.MainPage.LookAndFeel
    ),

    Behaviour(
        title = R.string.settings_behaviour,
        icon = R.drawable.graph,
        screen = Screens.Settings.MainPage.Behaviour
    ),

    MemoryAndStorage(
        title = R.string.settings_memory_storage,
        icon = R.drawable.storage,
        screen = Screens.Settings.MainPage.MemoryAndStorage
    )
}

private enum class AboutLinkItems(
    val title: Int,
    val icon: Int,
    val intent: Intent,
    val enabled: Boolean = true
) {
    Developer(
        title = R.string.dev_name,
        icon = R.drawable.code,
        intent =
            Intent(Intent.ACTION_VIEW).apply {
                data = "https://github.com/kaii-lb".toUri()
            },
    ),

    Translation(
        title = R.string.translation,
        icon = R.drawable.globe,
        intent =
            Intent(Intent.ACTION_VIEW).apply {
                data = "https://hosted.weblate.org/projects/lavender-photos/".toUri()
            }
    ),

    Donations(
        title = R.string.support,
        icon = R.drawable.donation,
        enabled = false,
        intent =
            Intent(Intent.ACTION_VIEW).apply {
                // data = TODO()
            }
    );

    val color: Color
        @Composable
        get() =
            when (this) {
                Donations -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surfaceContainerHighest
            }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainDialog(
    sheetState: SheetState,
    loginState: LoginStateManager,
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier,
    toggleSelectMode: () -> Unit,
    dismiss: () -> Unit
) {
    // remove (weird) drag handle ripple
    CompositionLocalProvider(
        LocalRippleConfiguration provides
                RippleConfiguration(
                    color = Color.Transparent,
                    rippleAlpha = RippleAlpha(0f, 0f, 0f, 0f)
                )
    ) {
        val mainViewModel = LocalMainViewModel.current
        val navController = LocalNavController.current
        val isLandscape by rememberDeviceOrientation()

        val extraSecureFolderEntry by mainViewModel.settings.lookAndFeel.getShowExtraSecureNav().collectAsStateWithLifecycle(initialValue = false)

        ModalBottomSheet(
            sheetState = sheetState,
            tonalElevation = 16.dp,
            shape = RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            onDismissRequest = dismiss,
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
                        .padding(start = 24.dp, end = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(
                        space = 4.dp,
                        alignment = Alignment.Top
                    ),
                    horizontalAlignment = Alignment.Start
                ) {
                    item {
                        val userInfo by loginState.state.collectAsStateWithLifecycle()
                        val alwaysShowInfo by mainViewModel.settings.immich.getAlwaysShowUserInfo().collectAsStateWithLifecycle(initialValue = false)

                        if (userInfo is LoginState.LoggedIn || alwaysShowInfo) {
                            MainDialogUserInfo(
                                loginState = userInfo,
                                coroutineScope = coroutineScope,
                                dismiss = dismiss
                            )
                        } else {
                            Text(
                                text = stringResource(id = R.string.app_name_full),
                                fontSize = TextStylingConstants.EXTRA_EXTRA_LARGE_TEXT_SIZE.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                        }
                    }

                    item {
                        PreferencesSeparatorText(
                            text = stringResource(id = R.string.management)
                        )
                    }

                    item {
                        ExpressiveDialogRow(
                            title = stringResource(id = R.string.media_select),
                            icon = painterResource(id = R.drawable.checklist),
                            position = if (extraSecureFolderEntry) RowPosition.Top else RowPosition.Single,
                            onClick = toggleSelectMode
                        )
                    }

                    if (extraSecureFolderEntry) {
                        item {
                            val authManager = rememberSecureFolderAuthManager(
                                coroutineScope = coroutineScope,
                                extraAction = dismiss
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
                            onClick = {
                                coroutineScope.launch {
                                    dismiss()
                                    delay(AnimationConstants.DURATION_SHORT.toLong())
                                    navController.navigate(Screens.Settings.Misc.DataAndBackup)
                                }
                            },
                            onActionClick = {
                                coroutineScope.launch {
                                    dismiss()
                                    delay(AnimationConstants.DURATION_SHORT.toLong())
                                    navController.navigate(Screens.Immich.InfoPage)
                                }
                            }
                        )
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

fun LazyListScope.settingsColumnItems(
    navController: NavController,
    coroutineScope: CoroutineScope,
    dismiss: () -> Unit
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
                    delay(AnimationConstants.DURATION_SHORT.toLong())
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

    itemsIndexed(
        items = AboutLinkItems.entries
    ) { index, item ->
        val context = LocalContext.current

        ExpressiveDialogRow(
            title = stringResource(id = item.title),
            icon = painterResource(id = item.icon),
            enabled = item.enabled,
            containerColor = item.color,
            position =
                when (index) {
                    0 -> RowPosition.Top
                    else -> RowPosition.Middle
                },
            onClick = {
                coroutineScope.launch {
                    dismiss()
                    delay(AnimationConstants.DURATION_SHORT.toLong())
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
                    delay(AnimationConstants.DURATION_SHORT.toLong())
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
                    delay(AnimationConstants.DURATION_SHORT.toLong())
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
        val resources = LocalResources.current
        val versionName = remember {
            try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (e: Throwable) {
                Log.e(TAG, e.toString())
                resources.getString(R.string.settings_about_unknown_version)
            }
        }

        var showVersionInfoDialog by remember { mutableStateOf(false) }
        if (showVersionInfoDialog) {
            VersionInfoDialog(
                changelog = stringResource(id = R.string.changelog),
                onDismiss = { showVersionInfoDialog = false }
            )
        }

        ExpressiveDialogRow(
            title = versionName ?: stringResource(id = R.string.version_info),
            icon = painterResource(id = R.drawable.info),
            position = RowPosition.Bottom,
            onClick = {
                showVersionInfoDialog = true
            }
        )
    }
}