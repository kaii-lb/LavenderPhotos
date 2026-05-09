package com.kaii.photos.compose.app_bars.album_view

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.IsSelectingTopBar
import com.kaii.photos.compose.app_bars.getAppBarContentTransition
import com.kaii.photos.compose.dialogs.user_action.AlbumPathsDialog
import com.kaii.photos.database.sync.CloudSyncWorker
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.di.appModule
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.grid_management.SelectionManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleAlbumViewTopBar(
    albumInfo: () -> AlbumType,
    selectionManager: SelectionManager,
    showTagDialog: () -> Boolean,
    isMediaPicker: Boolean = false,
    showDialog: () -> Unit,
    setShowTagDialog: (show: Boolean) -> Unit
) {
    val show by selectionManager.enabled.collectAsStateWithLifecycle(initialValue = false)

    LaunchedEffect(show) {
        if (!show) setShowTagDialog(false)
    }

    AnimatedContent(
        targetState = show,
        transitionSpec = {
            getAppBarContentTransition(show)
        },
        label = "SingleAlbumViewTopBarAnimatedContent"
    ) { target ->
        if (!target) {
            val navController = LocalNavController.current

            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                navigationIcon = {
                    val navController = LocalNavController.current

                    IconButton(
                        onClick = {
                            navController.popBackStack()
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.back_arrow),
                            contentDescription = "Go back to previous page",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = albumInfo().name,
                        fontSize = TextUnit(18f, TextUnitType.Sp),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .width(160.dp)
                    )
                },
                actions = {
                    val info = albumInfo()

                    if (info.immichId != null) {
                        val context = LocalContext.current
                        val coroutineScope = rememberCoroutineScope()
                        var loading by remember { mutableStateOf(false) }

                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    loading = true
                                    val id = CloudSyncWorker.immediateEnqueue(context)

                                    WorkManager.getInstance(context)
                                        .getWorkInfoByIdFlow(id)
                                        .collect {
                                            if (it?.state != WorkInfo.State.RUNNING
                                                && it?.state != WorkInfo.State.ENQUEUED
                                            ) {
                                                loading = false
                                            }
                                        }
                                }
                            },
                            enabled = !loading
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.cloud_sync),
                                contentDescription = stringResource(id = R.string.refresh)
                            )
                        }
                    }

                    if (!isMediaPicker && info is AlbumType.Folder) {
                        var showPathsDialog by remember { mutableStateOf(false) }
                        val context = LocalContext.current

                        if (showPathsDialog) {
                            AlbumPathsDialog(
                                albumInfo = info,
                                onConfirm = { selectedPaths ->
                                    val newInfo =
                                        info.copy(
                                            id = info.id,
                                            paths = selectedPaths
                                        )

                                    context.appModule.settings.albums.edit(
                                        id = info.id,
                                        newInfo = newInfo
                                    )

                                    navController.popBackStack()
                                    navController.navigate(
                                        route =
                                            Screens.Album.GridView(
                                                album = newInfo
                                            )
                                    )
                                },
                                onDismiss = {
                                    showPathsDialog = false
                                }
                            )
                        }

                        IconButton(
                            onClick = {
                                showPathsDialog = true
                            },
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.add),
                                contentDescription = "show more options for the album view",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier
                                    .size(24.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = showDialog
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.settings),
                            contentDescription = "show more options for the album view",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }
                }
            )
        } else {
            IsSelectingTopBar(
                selectionManager = selectionManager,
                showTags = true,
                showTagDialog = showTagDialog,
                setShowTagDialog = setShowTagDialog
            )
        }
    }
}