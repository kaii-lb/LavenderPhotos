package com.kaii.photos.compose.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.kaii.lavender.immichintegration.state_managers.LoginState
import com.kaii.lavender.immichintegration.state_managers.LoginStateManager
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.MainDialogUserInfo
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.datastore.DefaultTabs
import com.kaii.photos.helpers.EncryptionManager
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.helpers.exif.MediaData
import com.kaii.photos.helpers.exif.getExifDataForMedia
import com.kaii.photos.helpers.getDecryptCacheForFile
import com.kaii.photos.helpers.getSecureDecryptedVideoFile
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.paging.PhotoLibraryUIModel
import com.kaii.photos.helpers.rememberVibratorManager
import com.kaii.photos.helpers.vibrateShort
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.getIv
import com.kaii.photos.models.main_activity.MainViewModel
import com.kaii.photos.permissions.auth.rememberSecureFolderAuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "com.kaii.photos.compose.dialogs.InfoDialogs"

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun MainAppDialog(
    showDialog: MutableState<Boolean>,
    pagerState: PagerState,
    selectionManager: SelectionManager,
    mainViewModel: MainViewModel,
    loginState: LoginStateManager
) {
    val navController = LocalNavController.current
    val vibratorManager = rememberVibratorManager()
    val userInfo by loginState.state.collectAsStateWithLifecycle()
    val tabList by mainViewModel.settings.defaultTabs.getTabList().collectAsStateWithLifecycle(initialValue = mainViewModel.settings.defaultTabs.defaultTabList)

    val currentTab by remember {
        derivedStateOf {
            tabList[pagerState.currentPage]
        }
    }

    if (showDialog.value) {
        LavenderDialogBase(
            onDismiss = {
                showDialog.value = false
            }
        ) {
            TitleCloseRow(
                title = currentTab.name,
                closeOffset = 8.dp,
            ) {
                showDialog.value = false
            }

            val alwaysShowInfo by mainViewModel.settings.immich.getAlwaysShowUserInfo().collectAsStateWithLifecycle(initialValue = false)
            if (userInfo is LoginState.LoggedIn || alwaysShowInfo) {
                MainDialogUserInfo(
                    loginState = userInfo,
                    uploadPfp = loginState::uploadPfp,
                    setUsername = loginState::updateUsername
                )
            }

            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .wrapContentHeight(),
                verticalArrangement = Arrangement.spacedBy(space = 2.dp)
            ) {
                if (currentTab != DefaultTabs.TabTypes.albums && currentTab != DefaultTabs.TabTypes.secure) {
                    DialogClickableItem(
                        text = stringResource(id = R.string.media_select),
                        iconResId = R.drawable.check_item,
                        position = RowPosition.Top,
                    ) {
                        showDialog.value = false
                        selectionManager.enterSelectMode()
                        vibratorManager.vibrateShort()
                    }
                }

                if (currentTab == DefaultTabs.TabTypes.albums) {
                    var showAlbumTypeDialog by remember { mutableStateOf(false) }
                    if (showAlbumTypeDialog) {
                        AlbumAddChoiceDialog {
                            showAlbumTypeDialog = false
                        }
                    }

                    DialogClickableItem(
                        text = stringResource(id = R.string.add_album),
                        iconResId = R.drawable.add,
                        position = RowPosition.Top,
                    ) {
                        showAlbumTypeDialog = true
                    }
                }

                // TODO
                // val immichUploadCount by immichViewModel.immichUploadedMediaCount.collectAsStateWithLifecycle()
                // val immichUploadTotal by immichViewModel.immichUploadedMediaTotal.collectAsStateWithLifecycle()

                // if (immichUploadTotal != 0) {
                //     DialogClickableItem(
                //         text = stringResource(id = R.string.immich_main_dialog_sync_state, immichUploadCount, immichUploadTotal),
                //         iconResId = R.drawable.cloud_upload,
                //         position = if (currentView.value == DefaultTabs.TabTypes.secure) RowPosition.Top else RowPosition.Middle,
                //     )
                // }

                DialogClickableItem(
                    text = stringResource(id = R.string.data_and_backup),
                    iconResId = R.drawable.data,
                    // position = if (currentView.value == DefaultTabs.TabTypes.secure && immichUploadTotal == 0) RowPosition.Top else RowPosition.Middle,
                    position = if (currentTab == DefaultTabs.TabTypes.secure) RowPosition.Top else RowPosition.Middle,
                ) {
                    showDialog.value = false
                    navController.navigate(Screens.Settings.Misc.DataAndBackup)
                }

                val showExtraSecureItem by mainViewModel.settings.lookAndFeel
                    .getShowExtraSecureNav()
                    .collectAsStateWithLifecycle(initialValue = false)

                if (showExtraSecureItem) {
                    val authManager = rememberSecureFolderAuthManager()
                    DialogClickableItem(
                        text = stringResource(id = R.string.secure_folder),
                        iconResId = R.drawable.secure_folder,
                        position = RowPosition.Middle,
                    ) {
                        showDialog.value = false

                        authManager.authenticate()
                    }
                }

                DialogClickableItem(
                    text = stringResource(id = R.string.settings),
                    iconResId = R.drawable.settings,
                    position = RowPosition.Middle,
                ) {
                    showDialog.value = false
                    navController.navigate(Screens.Settings.MainPage)
                }

                DialogClickableItem(
                    text = stringResource(id = R.string.settings_about_and_updates),
                    iconResId = R.drawable.info,
                    position = RowPosition.Bottom,
                ) {
                    showDialog.value = false
                    navController.navigate(Screens.Settings.Misc.AboutAndUpdates)
                }
            }
        }
    }
}

@Composable
fun FeatureNotAvailableDialog(onDismiss: () -> Unit) {
    ExplanationDialog(
        title = stringResource(id = R.string.not_available),
        explanation = stringResource(id = R.string.not_available_desc),
        onDismiss = onDismiss
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SingleSecurePhotoInfoDialog(
    currentMediaItem: PhotoLibraryUIModel.SecuredMedia,
    privacyMode: Boolean,
    dismiss: () -> Unit,
    togglePrivacyMode: () -> Unit
) {
    val isLandscape by rememberDeviceOrientation()
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    LaunchedEffect(Unit) {
        sheetState.partialExpand()
    }

    // remove (weird) drag handle ripple
    CompositionLocalProvider(
        LocalRippleConfiguration provides
                RippleConfiguration(
                    color = Color.Transparent,
                    rippleAlpha = RippleAlpha(0f, 0f, 0f, 0f)
                )
    ) {
        ModalBottomSheet(
            sheetState = sheetState,
            tonalElevation = 16.dp,
            shape = RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp),
            containerColor = MaterialTheme.colorScheme.background,
            onDismissRequest = dismiss,
            contentWindowInsets = {
                if (!isLandscape) WindowInsets.systemBars
                else WindowInsets()
            },
            modifier = Modifier
                .windowInsetsPadding(
                    WindowInsets.statusBars
                )
        ) {
            BackHandler(
                enabled = !WindowInsets.isImeVisible
            ) {
                coroutineScope.launch {
                    sheetState.hide()
                    dismiss()
                }
            }

            var showLoadingDialog by remember { mutableStateOf(false) }
            if (showLoadingDialog) {
                LoadingDialog(
                    title = stringResource(id = R.string.secure_getting_file_info),
                    body = stringResource(id = R.string.secure_please_wait)
                )
            }

            val context = LocalContext.current
            var mediaData by remember {
                mutableStateOf(
                    emptyMap<MediaData, Any>()
                )
            }

            LaunchedEffect(currentMediaItem) {
                withContext(Dispatchers.IO) {
                    showLoadingDialog = true

                    val file = if (currentMediaItem.item.type == MediaType.Video) {
                        val originalFile = File(currentMediaItem.item.absolutePath)
                        val cachedFile = getSecureDecryptedVideoFile(
                            name = currentMediaItem.item.displayName,
                            context = context
                        )

                        if (!cachedFile.exists()) {
                            val iv = currentMediaItem.bytes?.getIv()

                            if (iv == null) {
                                Log.e(TAG, "IV for ${currentMediaItem.item.displayName} was null, aborting")
                                return@withContext
                            }
                            EncryptionManager.decryptVideo(
                                absolutePath = originalFile.absolutePath,
                                iv = iv,
                                context = context,
                                progress = {}
                            )
                        } else if (cachedFile.length() < originalFile.length()) {
                            while (cachedFile.length() < originalFile.length()) {
                                delay(100)
                            }

                            cachedFile
                        } else {
                            cachedFile
                        }
                    } else {
                        val originalFile = File(currentMediaItem.item.absolutePath)
                        val cachedFile = getDecryptCacheForFile(
                            file = originalFile,
                            context = context
                        )

                        if (!cachedFile.exists()) {
                            val iv = currentMediaItem.bytes?.getIv()

                            if (iv == null) {
                                Log.e(TAG, "IV for ${currentMediaItem.item.displayName} was null, aborting")
                                return@withContext
                            }
                            EncryptionManager.decryptInputStream(
                                inputStream = originalFile.inputStream(),
                                outputStream = cachedFile.outputStream(),
                                iv = iv
                            )

                            cachedFile
                        } else if (cachedFile.length() < originalFile.length()) {
                            val threshold = 500
                            while (cachedFile.length() + threshold < originalFile.length()) {
                                delay(100)
                            }

                            cachedFile
                        } else {
                            cachedFile
                        }
                    }

                    showLoadingDialog = false
                    mediaData = getExifDataForMedia(
                        context = context,
                        inputStream = file.inputStream(),
                        absolutePath = file.absolutePath,
                        fallback = currentMediaItem.item.dateModified
                    ).toMutableMap().apply {
                        set(MediaData.Path, currentMediaItem.item.parentPath)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .padding(top = 4.dp, start = 16.dp, bottom = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(
                    space = 8.dp,
                    alignment = Alignment.Top
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.media_information),
                    fontSize = TextStylingConstants.MEDIUM_TEXT_SIZE.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    items(
                        items = mediaData.keys.toList()
                    ) { key ->
                        val value = mediaData[key]

                        val splitBy = Regex("(?=[A-Z])")
                        val split = key.toString().split(splitBy)
                        val name =
                            "${if (split.size >= 3) "${split[1]} ${split[2]}" else key.toString()}:"

                        TallDialogInfoRow(
                            title = name,
                            info = value.toString(),
                            icon = key.icon,
                            position =
                                if (mediaData.keys.indexOf(key) == mediaData.keys.size - 1)
                                    RowPosition.Bottom
                                else if (mediaData.keys.indexOf(key) == 0)
                                    RowPosition.Top
                                else
                                    RowPosition.Middle
                        ) {
                            val clipboardManager =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipData = ClipData.newPlainText(name, value.toString())
                            clipboardManager.setPrimaryClip(clipData)
                        }
                    }

                    if (isLandscape) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))

                            TallDialogInfoRow(
                                title = stringResource(id = if (privacyMode) R.string.privacy_scroll_mode_enabled else R.string.privacy_scroll_mode_disabled),
                                info = "",
                                icon = if (!privacyMode) R.drawable.swipe else R.drawable.do_not_touch,
                                position = RowPosition.Single
                            ) {
                                togglePrivacyMode()
                            }
                        }
                    }
                }

                if (!isLandscape) {
                    TallDialogInfoRow(
                        title = stringResource(id = if (privacyMode) R.string.privacy_scroll_mode_enabled else R.string.privacy_scroll_mode_disabled),
                        info = "",
                        icon = if (!privacyMode) R.drawable.swipe else R.drawable.do_not_touch,
                        position = RowPosition.Single
                    ) {
                        togglePrivacyMode()
                    }
                }
            }
        }
    }
}

@Composable
fun TrashDeleteDialog(
    showDialog: Boolean,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()

                        onDismiss()
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.media_delete),
                        fontSize = TextStylingConstants.SMALL_TEXT_SIZE.sp
                    )
                }
            },
            title = {
                Text(
                    text = stringResource(id = R.string.media_delete_permanently_confirm),
                    fontSize = TextStylingConstants.MEDIUM_TEXT_SIZE.sp
                )
            },
            dismissButton = {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.media_cancel),
                        fontSize = TextStylingConstants.SMALL_TEXT_SIZE.sp
                    )
                }
            },
            shape = RoundedCornerShape(32.dp)
        )
    }
}