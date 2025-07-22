package com.kaii.photos.compose.app_bars

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarExitDirection
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastDistinctBy
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.LocalAppDatabase
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.MainActivity.Companion.immichViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.AnimatedLoginIcon
import com.kaii.photos.compose.SelectViewTopBarLeftButtons
import com.kaii.photos.compose.SelectViewTopBarRightButtons
import com.kaii.photos.compose.dialogs.AlbumAddChoiceDialog
import com.kaii.photos.compose.dialogs.ConfirmationDialog
import com.kaii.photos.compose.dialogs.LoadingDialog
import com.kaii.photos.compose.grids.MoveCopyAlbumListView
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.datastore.DefaultTabs
import com.kaii.photos.datastore.Permissions
import com.kaii.photos.helpers.GetDirectoryPermissionAndRun
import com.kaii.photos.helpers.GetPermissionAndRun
import com.kaii.photos.helpers.getParentFromPath
import com.kaii.photos.helpers.moveImageToLockedFolder
import com.kaii.photos.helpers.setTrashedOnPhotoList
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun MainAppTopBar(
    alternate: Boolean,
    showDialog: MutableState<Boolean>,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    currentView: MutableState<BottomBarTab>,
    isFromMediaPicker: Boolean = false
) {
    DualFunctionTopAppBar(
        alternated = alternate,
        title = {
            Row {
                Text(
                    text = stringResource(id = R.string.app_name_full).split(" ").first() + " ",
                    fontWeight = FontWeight.Bold,
                    fontSize = TextUnit(22f, TextUnitType.Sp)
                )
                Text(
                    text = stringResource(id = R.string.app_name_full).split(" ")[1], // second part
                    fontWeight = FontWeight.Normal,
                    fontSize = TextUnit(22f, TextUnitType.Sp)
                )
            }
        },
        actions = {
            AnimatedVisibility(
                visible = currentView.value == DefaultTabs.TabTypes.albums && !isFromMediaPicker,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ),
                exit = scaleOut(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            ) {
                var showAlbumTypeDialog by remember { mutableStateOf(false) }
                if (showAlbumTypeDialog) {
                    AlbumAddChoiceDialog {
                        showAlbumTypeDialog = false
                    }
                }

                IconButton(
                    onClick = {
                        showAlbumTypeDialog = true
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.add),
                        contentDescription = stringResource(id = R.string.album_add),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            if (!isFromMediaPicker) {
                val immichUserState by immichViewModel.immichUserLoginState.collectAsStateWithLifecycle()

                AnimatedLoginIcon(
                    immichUserLoginState = immichUserState
                ) {
                    showDialog.value = true
                }
            } else {
                val context = LocalContext.current
                IconButton(
                    onClick = {
                        (context as Activity).finish()
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = "Close media picker",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        alternateTitle = {
            SelectViewTopBarLeftButtons(selectedItemsList = selectedItemsList)
        },
        alternateActions = {
            SelectViewTopBarRightButtons(
                selectedItemsList = selectedItemsList
            )
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainAppBottomBar(
    currentView: MutableState<BottomBarTab>,
    tabs: List<BottomBarTab>,
    selectedItemsList: SnapshotStateList<MediaStoreData>
) {
    var expanded by remember { mutableStateOf(true) }

    LaunchedEffect(selectedItemsList.size) {
        expanded = selectedItemsList.isEmpty()
    }

    Box(
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth(1f)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(all = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        HorizontalFloatingToolbar(
            expanded = expanded,
            collapsedShadowElevation = 12.dp,
            expandedShadowElevation = 12.dp,
            scrollBehavior = FloatingToolbarDefaults.exitAlwaysScrollBehavior(
                exitDirection = FloatingToolbarExitDirection.Bottom // TODO: continue this
            ),
            leadingContent = {
                tabs.take(if (selectedItemsList.isNotEmpty()) 1 else 4).forEach { tab ->
                    ToggleButton(
                        checked = currentView.value == tab,
                        onCheckedChange = {
                            if (currentView.value != tab) {
                                selectedItemsList.clear()
                                currentView.value = tab
                            }
                        },
                        shapes = ToggleButtonDefaults.shapes(
                            shape = null,
                            pressedShape = CircleShape,
                            checkedShape = CircleShape
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)
                    ) {
                        AnimatedVisibility(
                            visible = currentView.value == tab
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = tab.icon.filled),
                                    contentDescription = stringResource(id = R.string.tabs_navigate_to, tab.name)
                                )

                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        }

                        Text(
                            text = tab.name,
                            fontSize = TextUnit(14f, TextUnitType.Sp),
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.width(2.dp))
                    }
                }
            }
        ) {
            val coroutineScope = rememberCoroutineScope()
            val context = LocalContext.current
            val selectedItemsWithoutSection by remember {
                derivedStateOf {
                    selectedItemsList.filter {
                        it.type != MediaType.Section && it != MediaStoreData.dummyItem
                    }
                }
            }

            if (selectedItemsList.isNotEmpty()) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            val hasVideos = selectedItemsWithoutSection.any {
                                it.type == MediaType.Video
                            }

                            val intent = Intent().apply {
                                action = Intent.ACTION_SEND_MULTIPLE
                                type = if (hasVideos) "video/*" else "images/*"
                            }

                            val fileUris = ArrayList<Uri>()
                            selectedItemsWithoutSection.forEach {
                                fileUris.add(it.uri)
                            }

                            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris)

                            context.startActivity(Intent.createChooser(intent, null))
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.share),
                        contentDescription = "share this image"
                    )
                }
                val show = remember { mutableStateOf(false) }
                var isMoving by remember { mutableStateOf(false) }
                MoveCopyAlbumListView(
                    show = show,
                    selectedItemsList = selectedItemsList,
                    isMoving = isMoving,
                    groupedMedia = null,
                    insetsPadding = WindowInsets.statusBars
                )

                IconButton(
                    onClick = {
                        isMoving = true
                        show.value = true
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.cut),
                        contentDescription = "move this image"
                    )
                }

                IconButton(
                    onClick = {
                        isMoving = false
                        show.value = true
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.copy),
                        contentDescription = "copy this image"
                    )
                }

                val showDeleteDialog = remember { mutableStateOf(false) }
                val runDeleteAction = remember { mutableStateOf(false) }

                val mainViewModel = LocalMainViewModel.current
                val applicationDatabase = LocalAppDatabase.current
                GetPermissionAndRun(
                    uris = selectedItemsWithoutSection.fastMapNotNull { it.uri },
                    shouldRun = runDeleteAction,
                    onGranted = {
                        mainViewModel.launch(Dispatchers.IO) {
                            setTrashedOnPhotoList(
                                context = context,
                                list = selectedItemsWithoutSection,
                                trashed = true,
                                appDatabase = applicationDatabase
                            )

                            selectedItemsList.clear()
                        }
                    }
                )

                ConfirmationDialog(
                    showDialog = showDeleteDialog,
                    dialogTitle = stringResource(id = R.string.media_trash_confirm),
                    confirmButtonLabel = stringResource(id = R.string.media_delete)
                ) {
                    runDeleteAction.value = true
                }

                val confirmToDelete by mainViewModel.settings.Permissions.getConfirmToDelete()
                    .collectAsStateWithLifecycle(initialValue = true)

                IconButton(
                    onClick = {
                        if (confirmToDelete) showDeleteDialog.value = true
                        else runDeleteAction.value = true
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.delete),
                        contentDescription = "Delete this image"
                    )
                }

                val moveToSecureFolder = remember { mutableStateOf(false) }
                val tryGetDirPermission = remember { mutableStateOf(false) }

                var showLoadingDialog by remember { mutableStateOf(false) }

                GetDirectoryPermissionAndRun(
                    absoluteDirPaths = selectedItemsWithoutSection.fastMap {
                        it.absolutePath.getParentFromPath()
                    }.fastDistinctBy {
                        it
                    },
                    shouldRun = tryGetDirPermission,
                    onGranted = {
                        showLoadingDialog = true
                        moveToSecureFolder.value = true
                    },
                    onRejected = {}
                )

                if (showLoadingDialog) {
                    LoadingDialog(
                        title = stringResource(id = R.string.secure_encrypting),
                        body = stringResource(id = R.string.secure_processing)
                    )
                }

                val appDatabase = LocalAppDatabase.current
                GetPermissionAndRun(
                    uris = selectedItemsWithoutSection.map { it.uri },
                    shouldRun = moveToSecureFolder,
                    onGranted = {
                        mainViewModel.launch(Dispatchers.IO) {
                            moveImageToLockedFolder(
                                list = selectedItemsWithoutSection,
                                context = context,
                                applicationDatabase = appDatabase
                            ) {
                                selectedItemsList.clear()
                                showLoadingDialog = false
                            }
                        }
                    }
                )

                IconButton(
                    onClick = {
                        if (selectedItemsWithoutSection.isNotEmpty()) tryGetDirPermission.value = true
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.locked_folder),
                        contentDescription = "Secure this media"
                    )
                }
            } else if (!expanded) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    tabs.drop(4).forEach { tab ->
                        val height by animateDpAsState(
                            targetValue = if (expanded) 0.dp else 48.dp,
                            animationSpec = tween(
                                delayMillis = 300
                            )
                        )

                        ToggleButton(
                            checked = currentView.value == tab,
                            onCheckedChange = {
                                if (currentView.value != tab) {
                                    selectedItemsList.clear()
                                    currentView.value = tab
                                }
                            },
                            shapes = ToggleButtonDefaults.shapes(
                                shape = null,
                                pressedShape = CircleShape,
                                checkedShape = CircleShape
                            ),
                            contentPadding = PaddingValues(all = 10.dp),
                            modifier = Modifier
                                .height(height)
                        ) {
                            AnimatedVisibility(
                                visible = currentView.value == tab
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = tab.icon.filled),
                                        contentDescription = stringResource(id = R.string.tabs_navigate_to, tab.name)
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                            }

                            Text(
                                text = tab.name,
                                fontSize = TextUnit(14f, TextUnitType.Sp),
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.width(2.dp))
                        }
                    }
                }
            }

            if (tabs.size > 4 && selectedItemsList.isEmpty()) {
                IconButton(
                    onClick = {
                        expanded = !expanded
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.other_page_indicator),
                        contentDescription = "Show more tabs"
                    )
                }
            }
        }
    }
}
