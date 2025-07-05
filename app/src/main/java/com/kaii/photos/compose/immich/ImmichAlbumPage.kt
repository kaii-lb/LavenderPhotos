package com.kaii.photos.compose.immich

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.LocalNavController
import com.kaii.photos.MainActivity.Companion.immichViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.PreferencesRow
import com.kaii.photos.compose.dialogs.ConfirmationDialogWithBody
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.ImmichBackupMedia
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.immich.ImmichAlbumDuplicateState
import com.kaii.photos.immich.ImmichAlbumSyncState
import com.kaii.photos.immich.ImmichServerSidedAlbumsState
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.models.multi_album.MultiAlbumViewModel
import kotlinx.coroutines.launch

private const val TAG = "IMMICH_ALBUM_PAGE"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmichAlbumPage(
    albumInfo: AlbumInfo,
    multiAlbumViewModel: MultiAlbumViewModel
) {
    var dynamicAlbumInfo by remember { mutableStateOf(albumInfo) }
    val navController = LocalNavController.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        multiAlbumViewModel.reinitDataSource(
            context = context,
            album = dynamicAlbumInfo,
            sortMode = multiAlbumViewModel.sortBy
        )
    }

    BackHandler {
        multiAlbumViewModel.cancelMediaFlow()
        navController.popBackStack()
    }

    val deviceAssetIds = multiAlbumViewModel.mediaFlow
        .collectAsStateWithLifecycle().value
        .fastMapNotNull {
            if (it.type != MediaType.Section) {
                ImmichBackupMedia(
                    deviceAssetId = "${it.displayName}-${it.size}",
                    absolutePath = it.absolutePath
                )
            } else {
                null
            }
        }

    Log.d(TAG, "Device asset ids $deviceAssetIds")

    var loadingBackupState by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        loadingBackupState = true
        immichViewModel.refreshAlbums()
        immichViewModel.checkSyncStatus(
            immichAlbumId = dynamicAlbumInfo.immichId,
            expectedPhotoImmichIds = deviceAssetIds.map { it.deviceAssetId }.toSet()
        )
        immichViewModel.refreshDuplicateState(
            deviceAssetIds = deviceAssetIds.fastMap { it.deviceAssetId }
        ) {
            loadingBackupState = false
        }
    }

    Scaffold(
        topBar = {
            ImmichAlbumPageTopBar(album = dynamicAlbumInfo)
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = loadingBackupState,
            onRefresh = {
                coroutineScope.launch {
                    loadingBackupState = true
                    immichViewModel.checkSyncStatus(
                        immichAlbumId = dynamicAlbumInfo.immichId,
                        expectedPhotoImmichIds = deviceAssetIds.map { it.deviceAssetId }.toSet()
                    )
                    immichViewModel.refreshDuplicateState(
                        deviceAssetIds = deviceAssetIds.fastMap { it.deviceAssetId }
                    )
                    immichViewModel.refreshAlbums {
                        loadingBackupState = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize(1f)
                .padding(innerPadding)
        ) {
            val albumsSyncState by immichViewModel.immichAlbumsSyncState.collectAsStateWithLifecycle()
            val serverSideAlbum = run {
                val all by immichViewModel.immichServerAlbums.collectAsStateWithLifecycle()
                when (all) {
                    is ImmichServerSidedAlbumsState.Synced -> {
                        (all as ImmichServerSidedAlbumsState.Synced).albums.find { it.id == dynamicAlbumInfo.immichId }
                    }

                    else -> {
                        null
                    }
                }
            }

            val albumSyncState by remember {
                derivedStateOf {
                    albumsSyncState[dynamicAlbumInfo.immichId]
                }
            }
            val dupes by immichViewModel.immichAlbumsDupState.collectAsStateWithLifecycle()
            val currentAlbumDupe by remember {
                derivedStateOf {
                    val state = dupes[dynamicAlbumInfo.immichId]
                    when (state) {
                        is ImmichAlbumDuplicateState.HasDupes -> {
                            state.deviceAssetIds
                        }

                        else -> {
                            emptySet()
                        }
                    }
                }
            }
            Log.d(TAG, "Duplicates $currentAlbumDupe")

            LazyColumn(
                modifier = Modifier
                    .wrapContentSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    PreferencesRow(
                        title = stringResource(id = R.string.immich_sync_status) + " " + "${serverSideAlbum?.assetCount ?: 0}/${deviceAssetIds.size - currentAlbumDupe.size}",
                        summary = "Duplicates: ${currentAlbumDupe.size}",
                        iconResID = R.drawable.cloud_upload,
                        position = RowPosition.Single,
                        showBackground = false
                    )
                }

                item {
                    val uploadCount by immichViewModel.immichUploadedMediaCount.collectAsStateWithLifecycle()
                    val uploadTotal by immichViewModel.immichUploadedMediaTotal.collectAsStateWithLifecycle()
                    val notificationBody = remember { mutableStateOf("") }
                    val notificationPercentage = remember { mutableFloatStateOf(0f) }

                    LaunchedEffect(uploadCount, uploadTotal) {
                        if (uploadTotal != 0) notificationBody.value =
                            "${uploadCount}/${uploadTotal} done"
                        else notificationBody.value = "Operation complete!"
                        notificationPercentage.floatValue = uploadCount.toFloat() / uploadTotal
                    }

                    val missingFromImmich by remember {
                        derivedStateOf {
                            when (albumSyncState) {
                                is ImmichAlbumSyncState.InSync -> {
                                    "0"
                                }

                                is ImmichAlbumSyncState.OutOfSync -> {
                                    (albumSyncState as ImmichAlbumSyncState.OutOfSync).missing.minus(currentAlbumDupe).size
                                }

                                is ImmichAlbumSyncState.Error -> "Unknown"

                                else -> "Loading"
                            }
                        }
                    }

                    val extraInImmich by remember {
                        derivedStateOf {
                            when (albumSyncState) {
                                is ImmichAlbumSyncState.InSync -> {
                                    "0"
                                }

                                is ImmichAlbumSyncState.OutOfSync -> {
                                    (albumSyncState as ImmichAlbumSyncState.OutOfSync).extra.size
                                }

                                is ImmichAlbumSyncState.Error -> "Unknown"

                                else -> "Loading"
                            }
                        }
                    }

                    PreferencesRow(
                        title = "Sync Album and Immich",
                        summary = "Extra in: Album: $missingFromImmich. Immich: $extraInImmich",
                        iconResID = R.drawable.cloud_upload,
                        position = RowPosition.Single,
                        showBackground = false,
                        enabled = !loadingBackupState
                    ) {
                        immichViewModel.addAlbumToSync(
                            albumInfo = dynamicAlbumInfo,
                            context = context,
                            notificationBody = notificationBody,
                            notificationPercentage = notificationPercentage
                        ) { newId ->
                            dynamicAlbumInfo = dynamicAlbumInfo.copy(
                                immichId = newId
                            )
                            immichViewModel.refreshDuplicateState(
                                deviceAssetIds = deviceAssetIds.fastMap { it.deviceAssetId }
                            )
                            immichViewModel.checkSyncStatus(
                                immichAlbumId = newId,
                                expectedPhotoImmichIds = deviceAssetIds.map { it.deviceAssetId }
                                    .toSet()
                            )
                            immichViewModel.refreshAlbums {
                                loadingBackupState = false
                            }
                        }
                    }
                }

                item {
                    val showDeleteConfirmationDialog = remember { mutableStateOf(false) }

                    if (showDeleteConfirmationDialog.value) {
                        // TODO: add option to delete with all images in it
                        ConfirmationDialogWithBody(
                            showDialog = showDeleteConfirmationDialog,
                            dialogTitle = stringResource(id = R.string.immich_albums_clear),
                            dialogBody = stringResource(id = R.string.immich_albums_clear_desc),
                            confirmButtonLabel = stringResource(id = R.string.media_delete)
                        ) {
                            loadingBackupState = true
                            immichViewModel.removeAlbumFromSync(
                                albumInfo = dynamicAlbumInfo
                            ) {
                                dynamicAlbumInfo = dynamicAlbumInfo.copy(
                                    immichId = ""
                                )
                                loadingBackupState = false
                            }
                            immichViewModel.refreshAlbums {
                                loadingBackupState = false
                            }
                            immichViewModel.refreshDuplicateState(
                                deviceAssetIds = deviceAssetIds.fastMap { it.deviceAssetId }
                            )
                        }
                    }
                    PreferencesRow(
                        title = "Delete album",
                        summary = "Removes album without removing contents",
                        iconResID = R.drawable.cloud_upload,
                        position = RowPosition.Single,
                        showBackground = false,
                        enabled = !loadingBackupState
                    ) {
                        showDeleteConfirmationDialog.value = true
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ImmichAlbumPageTopBar(album: AlbumInfo) {
    val navController = LocalNavController.current

    TopAppBar(
        title = {
            Text(
                text = album.name,
                fontSize = TextUnit(18f, TextUnitType.Sp)
            )
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    navController.popBackStack()
                },
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.back_arrow),
                    contentDescription = stringResource(id = R.string.return_to_previous_page),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(24.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}