package com.kaii.photos.compose.immich

import android.os.CancellationSignal
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
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
import com.kaii.photos.immich.getImmichBackupMedia
import com.kaii.photos.models.multi_album.MultiAlbumViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.io.encoding.ExperimentalEncodingApi

private const val TAG = "IMMICH_ALBUM_PAGE"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalStdlibApi::class, ExperimentalEncodingApi::class)
@Composable
fun ImmichAlbumPage(
    albumInfo: AlbumInfo,
    multiAlbumViewModel: MultiAlbumViewModel
) {
    var currentAlbumImmichId by remember { mutableStateOf(albumInfo.immichId) }

    val navController = LocalNavController.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        multiAlbumViewModel.reinitDataSource(
            context = context,
            album = albumInfo,
            sortMode = multiAlbumViewModel.sortBy
        )
    }

    val coroutineScope = rememberCoroutineScope()
    val cancellationSignal = remember { CancellationSignal() }
    BackHandler {
        cancellationSignal.cancel()
        if (immichViewModel.immichUploadedMediaTotal.value != 0) {
            coroutineScope.launch {
                LavenderSnackbarController.pushEvent(
                    LavenderSnackbarEvents.MessageEvent(
                        message = context.resources.getString(R.string.immich_continuing_in_bg),
                        duration = SnackbarDuration.Short,
                        icon = R.drawable.cloud_upload
                    )
                )
            }
        }
        multiAlbumViewModel.cancelMediaFlow()
        navController.popBackStack()
    }

    val groupedMedia by multiAlbumViewModel.mediaFlow.collectAsStateWithLifecycle()
    var deviceBackupMedia by remember { mutableStateOf(emptyList<ImmichBackupMedia>()) }

    var loadingBackupState by remember { mutableStateOf(false) }
    LaunchedEffect(groupedMedia) {
        loadingBackupState = true
        withContext(Dispatchers.IO) {
            deviceBackupMedia = getImmichBackupMedia(
                groupedMedia = groupedMedia,
                cancellationSignal = cancellationSignal
            )

            immichViewModel.refreshAllFor(
                immichId = currentAlbumImmichId,
                expectedPhotoImmichIds = deviceBackupMedia.map {
                    ImmichBackupMedia(
                        deviceAssetId = it.deviceAssetId,
                        absolutePath = it.absolutePath,
                        checksum = it.checksum
                    )
                }.toSet()
            ) {
                loadingBackupState = false
            }
        }
    }

    Scaffold(
        topBar = {
            ImmichAlbumPageTopBar(album = albumInfo)
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = loadingBackupState,
            onRefresh = {
                coroutineScope.launch {
                    loadingBackupState = true
                    immichViewModel.refreshAllFor(
                        immichId = currentAlbumImmichId,
                        expectedPhotoImmichIds = deviceBackupMedia.map {
                            ImmichBackupMedia(
                                deviceAssetId = it.deviceAssetId,
                                absolutePath = it.absolutePath,
                                checksum = it.checksum
                            )
                        }.toSet()
                    ) {
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
                        (all as ImmichServerSidedAlbumsState.Synced).albums.find { it.id == currentAlbumImmichId }
                    }

                    else -> {
                        null
                    }
                }
            }

            val albumSyncState by remember {
                derivedStateOf {
                    albumsSyncState[currentAlbumImmichId]
                }
            }

            val dupes by immichViewModel.immichAlbumsDupState.collectAsStateWithLifecycle()
            val currentAlbumDupes by remember {
                derivedStateOf {
                    val dupe = dupes[currentAlbumImmichId]

                    when (dupe) {
                        is ImmichAlbumDuplicateState.HasDupes -> {
                            val map = deviceBackupMedia.map { it.deviceAssetId }
                            val needed = dupe.dupeAssets.filter { it.deviceAssetId in map }

                            needed.toSet()
                        }

                        else -> emptySet()
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .wrapContentSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    val dupeCount by remember {
                        derivedStateOf {
                            currentAlbumDupes.distinctBy { it.checksum }.map { it.deviceAssetId }.size
                        }
                    }
                    val actualCount by remember {
                        derivedStateOf {
                            deviceBackupMedia.filter { it.checksum !in currentAlbumDupes.map { it.checksum } }
                                .map { it.deviceAssetId }.size + dupeCount
                        }
                    }

                    PreferencesRow(
                        title = stringResource(id = R.string.immich_sync_status) + " " + "${serverSideAlbum?.assetCount ?: 0}/${actualCount}",
                        summary = "Duplicates: $dupeCount",
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
                    var delayMs by remember { mutableIntStateOf(0) }

                    LaunchedEffect(uploadCount, uploadTotal) {
                        if (delayMs == 0) {
                            notificationPercentage.floatValue = uploadCount.toFloat() / (if (uploadTotal == 0) 1 else uploadTotal)
                        } else {
                            delay(delayMs.toLong())
                        }

                        Log.d(TAG, "Notis percentage is ${notificationPercentage.floatValue}")

                        if (notificationPercentage.floatValue < 1f) {
                            val moddedTotal = if (uploadTotal == 0) "?" else uploadTotal.toString()
                            notificationBody.value =
                                "${uploadCount}/${moddedTotal} ${context.resources.getString(R.string.immich_done)}"
                        } else {
                            notificationBody.value = context.resources.getString(R.string.immich_operation_complete)
                            delayMs = 1000
                        }
                    }

                    val missingFromImmich by remember {
                        derivedStateOf {
                            when (albumSyncState) {
                                is ImmichAlbumSyncState.InSync -> {
                                    "0"
                                }

                                is ImmichAlbumSyncState.OutOfSync -> {
                                    (albumSyncState as ImmichAlbumSyncState.OutOfSync).missing.size
                                }

                                is ImmichAlbumSyncState.Error -> context.resources.getString(R.string.immich_state_unknown)

                                else -> context.resources.getString(R.string.immich_state_loading)
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

                                is ImmichAlbumSyncState.Error -> context.resources.getString(R.string.immich_state_unknown)

                                else -> context.resources.getString(R.string.immich_state_loading)
                            }
                        }
                    }

                    PreferencesRow(
                        title = stringResource(id = R.string.immich_sync_album),
                        summary = stringResource(id = R.string.immich_sync_album_out_of_sync_desc, missingFromImmich, extraInImmich),
                        iconResID = R.drawable.cloud_upload,
                        position = RowPosition.Single,
                        showBackground = false,
                        enabled = !loadingBackupState
                    ) {
                        coroutineScope.launch(Dispatchers.IO) {
                            currentAlbumImmichId = immichViewModel.addAlbumToSync(
                                albumInfo = AlbumInfo(
                                    id = albumInfo.id,
                                    immichId = currentAlbumImmichId,
                                    name = albumInfo.name,
                                    paths = albumInfo.paths,
                                    isCustomAlbum = albumInfo.isCustomAlbum,
                                    isPinned = albumInfo.isPinned
                                ),
                                notificationBody = notificationBody,
                                notificationPercentage = notificationPercentage
                            )

                            loadingBackupState = true

                            while (albumSyncState !is ImmichAlbumSyncState.InSync) {
                                delay(1000)
                                immichViewModel.refreshAllFor(
                                    immichId = currentAlbumImmichId,
                                    expectedPhotoImmichIds = deviceBackupMedia.map {
                                        ImmichBackupMedia(
                                            deviceAssetId = it.deviceAssetId,
                                            absolutePath = it.absolutePath,
                                            checksum = it.checksum
                                        )
                                    }.toSet()
                                ) {}
                            }

                            loadingBackupState = false
                        }
                    }
                }

                item {
                    val showDeleteConfirmationDialog = remember { mutableStateOf(false) }

                    if (showDeleteConfirmationDialog.value) {
                        // TODO: add option to delete with all images in it
                        ConfirmationDialogWithBody(
                            showDialog = showDeleteConfirmationDialog,
                            dialogTitle = stringResource(id = R.string.immich_album_delete),
                            dialogBody = stringResource(id = R.string.immich_album_delete_desc),
                            confirmButtonLabel = stringResource(id = R.string.custom_album_remove_media)
                        ) {
                            loadingBackupState = true
                            immichViewModel.removeAlbumFromSync(
                                albumInfo = AlbumInfo(
                                    id = albumInfo.id,
                                    immichId = currentAlbumImmichId,
                                    name = albumInfo.name,
                                    paths = albumInfo.paths,
                                    isCustomAlbum = albumInfo.isCustomAlbum,
                                    isPinned = albumInfo.isPinned
                                )
                            ) {
                                currentAlbumImmichId = ""
                            }
                            immichViewModel.refreshAllFor(
                                immichId = "",
                                expectedPhotoImmichIds = deviceBackupMedia.map {
                                    ImmichBackupMedia(
                                        deviceAssetId = it.deviceAssetId,
                                        absolutePath = it.absolutePath,
                                        checksum = it.checksum
                                    )
                                }.toSet()
                            ) {
                                loadingBackupState = false
                            }
                        }
                    }
                    PreferencesRow(
                        title = stringResource(id = R.string.immich_album_delete_full),
                        summary = stringResource(id = R.string.immich_album_delete_desc),
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