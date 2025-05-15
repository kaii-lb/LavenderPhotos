package com.kaii.photos.compose.app_bars

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMapNotNull
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.MainActivity.Companion.mainViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.SelectViewTopBarLeftButtons
import com.kaii.photos.compose.SelectViewTopBarRightButtons
import com.kaii.photos.compose.dialogs.AlbumAddChoiceDialog
import com.kaii.photos.compose.dialogs.ConfirmationDialog
import com.kaii.photos.compose.grids.MoveCopyAlbumListView
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.datastore.DefaultTabs
import com.kaii.photos.datastore.Permissions
import com.kaii.photos.helpers.GetPermissionAndRun
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
    currentView: MutableState<BottomBarTab>
) {
    DualFunctionTopAppBar(
        alternated = alternate,
        title = {
            Row {
                Text(
                    text = "Lavender ",
                    fontWeight = FontWeight.Bold,
                    fontSize = TextUnit(22f, TextUnitType.Sp)
                )
                Text(
                    text = "Photos",
                    fontWeight = FontWeight.Normal,
                    fontSize = TextUnit(22f, TextUnitType.Sp)
                )
            }
        },
        actions = {
            AnimatedVisibility(
                visible = currentView.value == DefaultTabs.TabTypes.albums,
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
                        contentDescription = "Add album",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            IconButton(
                onClick = {
                    showDialog.value = true
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.settings),
                    contentDescription = "Settings Button",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        alternateTitle = {
            SelectViewTopBarLeftButtons(selectedItemsList = selectedItemsList)
        },
        alternateActions = {
            SelectViewTopBarRightButtons(
                selectedItemsList = selectedItemsList,
                currentView = currentView
            )
        },
    )
}

@Composable
fun MainAppBottomBar(
    currentView: MutableState<BottomBarTab>,
    tabs: List<BottomBarTab>,
    selectedItemsList: SnapshotStateList<MediaStoreData>
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        contentPadding = PaddingValues(0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEach { tab ->
                SelectableBottomAppBarItem(
                    selected = currentView.value == tab,
                    action = {
                        if (currentView.value != tab) {
                            selectedItemsList.clear()
                            currentView.value = tab
                        }
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = if (currentView.value == tab) tab.icon.filled else tab.icon.nonFilled),
                            contentDescription = "Navigate to ${tab.name} page",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = tab.name,
                            fontSize = TextUnit(14f, TextUnitType.Sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .wrapContentSize()
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun MainAppSelectingBottomBar(
    selectedItemsList: SnapshotStateList<MediaStoreData>
) {
    IsSelectingBottomAppBar {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        val selectedItemsWithoutSection by remember {
            derivedStateOf {
                selectedItemsList.filter {
                    it.type != MediaType.Section && it != MediaStoreData()
                }
            }
        }

        BottomAppBarItem(
            text = "Share",
            iconResId = R.drawable.share,
            action = {
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
        )

        val show = remember { mutableStateOf(false) }
        var isMoving by remember { mutableStateOf(false) }
        MoveCopyAlbumListView(
            show = show,
            selectedItemsList = selectedItemsList,
            isMoving = isMoving,
            groupedMedia = null,
            insetsPadding = WindowInsets.statusBars
        )

        BottomAppBarItem(
            text = "Move",
            iconResId = R.drawable.cut,
            action = {
                isMoving = true
                show.value = true
            }
        )

        BottomAppBarItem(
            text = "Copy",
            iconResId = R.drawable.copy,
            action = {
                isMoving = false
                show.value = true
            }
        )

        val showDeleteDialog = remember { mutableStateOf(false) }
        val runDeleteAction = remember { mutableStateOf(false) }

        GetPermissionAndRun(
            uris = selectedItemsWithoutSection.fastMapNotNull { it.uri },
            shouldRun = runDeleteAction,
            onGranted = {
                mainViewModel.launch(Dispatchers.IO) {
                    setTrashedOnPhotoList(
                        context = context,
                        list = selectedItemsWithoutSection.map { Pair(it.uri, it.absolutePath) },
                        trashed = true
                    )

                    selectedItemsList.clear()
                }
            }
        )

        val confirmToDelete by mainViewModel.settings.Permissions.getConfirmToDelete()
            .collectAsStateWithLifecycle(initialValue = true)
        BottomAppBarItem(
            text = "Delete",
            iconResId = R.drawable.delete,
            cornerRadius = 16.dp,
            action = {
                if (confirmToDelete) showDeleteDialog.value = true
                else runDeleteAction.value = true
            },
            dialogComposable = {
                ConfirmationDialog(
                    showDialog = showDeleteDialog,
                    dialogTitle = "Move these items to Trash Bin?",
                    confirmButtonLabel = "Delete"
                ) {
                    runDeleteAction.value = true
                }
            }
        )
    }
}