package com.kaii.photos.compose.grids

import android.content.ContentValues
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.LocalAppDatabase
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.ClearableTextField
import com.kaii.photos.compose.FolderIsEmpty
import com.kaii.photos.compose.dialogs.getDefaultShapeSpacerForPosition
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.AlbumsList
import com.kaii.photos.datastore.Permissions
import com.kaii.photos.helpers.GetDirectoryPermissionAndRun
import com.kaii.photos.helpers.GetPermissionAndRun
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.copyImageListToPath
import com.kaii.photos.helpers.moveImageListToPath
import com.kaii.photos.helpers.setTrashedOnPhotoList
import com.kaii.photos.helpers.toBasePath
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.content_provider.LavenderContentProvider
import com.kaii.photos.mediastore.content_provider.LavenderMediaColumns
import com.kaii.photos.models.album_grid.AlbumsViewModel
import com.kaii.photos.models.album_grid.AlbumsViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "MOVE_COPY_ALBUM_LIST_VIEW"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MoveCopyAlbumListView(
    show: MutableState<Boolean>,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    isMoving: Boolean,
    groupedMedia: MutableState<List<MediaStoreData>>? = null,
    insetsPadding: WindowInsets
) {
    val context = LocalContext.current
    val mainViewModel = LocalMainViewModel.current
    val appDatabase = LocalAppDatabase.current
    val autoDetectAlbums by mainViewModel.settings.AlbumsList.getAutoDetect().collectAsStateWithLifecycle(initialValue = true)
    val displayDateFormat by mainViewModel.displayDateFormat.collectAsStateWithLifecycle()

    val originalAlbumsList by if (autoDetectAlbums) {
        mainViewModel.settings.AlbumsList.getAutoDetectedAlbums(displayDateFormat, appDatabase).collectAsStateWithLifecycle(initialValue = emptyList())
    } else {
        mainViewModel.settings.AlbumsList.getNormalAlbums().collectAsStateWithLifecycle(initialValue = emptyList())
    }

    val albumsViewModel: AlbumsViewModel = viewModel(
        factory = AlbumsViewModelFactory(
            context = context,
            albums = originalAlbumsList.filter { if (isMoving) !it.isCustomAlbum else true }
        )
    )

    LaunchedEffect(originalAlbumsList) {
        albumsViewModel.refresh(
            context = context,
            albums = originalAlbumsList.filter { if (isMoving) !it.isCustomAlbum else true }
        )
    }

    val albumToThumbnailMapping by albumsViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)

    var albumsList by remember { mutableStateOf(originalAlbumsList) }

    val searchedForText = remember { mutableStateOf("") }

    val state = rememberLazyListState()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(searchedForText.value, originalAlbumsList) {
        albumsList = originalAlbumsList.filter {
            it.name.contains(searchedForText.value, true)
        }.sortedByDescending { album ->
            val mediaItem = albumToThumbnailMapping.find {
                it.first.id == album.id
            }?.second ?: MediaStoreData.dummyItem

            (if (album.isCustomAlbum) 1L else 0L) or mediaItem.dateModified
        }

        if (albumsList.isNotEmpty()) state.scrollToItem(0)
    }

    LaunchedEffect(show.value) {
        searchedForText.value = ""
    }

    Log.d("MOVE_COPY_ALBUMS_LIST", albumsList.toString())

    if (show.value) {
        ModalBottomSheet(
            sheetState = sheetState,
            tonalElevation = 16.dp,
            shape = RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp),
            properties = ModalBottomSheetProperties(
                shouldDismissOnBackPress = false
            ),
            containerColor = MaterialTheme.colorScheme.background,
            onDismissRequest = { show.value = false },
            modifier = Modifier
                .windowInsetsPadding(
                    insetsPadding
                ),
        ) {
            BackHandler(
                enabled = show.value && !WindowInsets.isImeVisible
            ) {
                coroutineScope.launch {
                    sheetState.hide()
                    show.value = false
                }
            }

            AnimatedVisibility(
                visible = sheetState.currentValue == SheetValue.Expanded,
                enter = expandVertically(
                    expandFrom = Alignment.Top
                ) + fadeIn(),
                exit = shrinkVertically(
                    shrinkTowards = Alignment.Top
                ) + fadeOut(),
                modifier = Modifier
                    .fillMaxWidth(1f)
            ) {
                ClearableTextField(
                    text = searchedForText,
                    placeholder = stringResource(id = R.string.media_move_copy_list_search_bar),
                    icon = R.drawable.search,
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .height(56.dp)
                        .padding(16.dp, 0.dp),
                    onClear = {
                        searchedForText.value = ""
                    },
                    onConfirm = {}
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (albumsList.isEmpty()) {
                FolderIsEmpty(
                    emptyText = stringResource(id = R.string.albums_non_existent),
                    emptyIconResId = R.drawable.error,
                    backgroundColor = Color.Transparent
                )
            } else {
                LazyColumn(
                    state = state,
                    modifier = Modifier
                        .fillMaxSize(1f)
                        .padding(8.dp, 8.dp, 8.dp, 0.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    items(
                        count = albumsList.size,
                        key = {
                            albumsList[it].id
                        }
                    ) { index ->
                        val album = albumsList[index]
                        val mediaItem = albumToThumbnailMapping.find {
                            it.first.id == album.id
                        }?.second ?: MediaStoreData.dummyItem

                        AlbumsListItem(
                            album = album,
                            data = mediaItem,
                            position = if (index == albumsList.size - 1 && albumsList.size != 1) RowPosition.Bottom else if (albumsList.size == 1) RowPosition.Single else if (index == 0) RowPosition.Top else RowPosition.Middle,
                            selectedItemsList = selectedItemsList,
                            isMoving = isMoving,
                            show = show,
                            groupedMedia = groupedMedia,
                            modifier = Modifier
                                .fillParentMaxWidth(1f)
                                .padding(8.dp, 0.dp)
                                .animateItem()
                        )
                    }
                }
            }
        }
    } else {
        LaunchedEffect(Unit) {
            state.scrollToItem(0)
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun AlbumsListItem(
    album: AlbumInfo,
    data: MediaStoreData,
    position: RowPosition,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    isMoving: Boolean,
    show: MutableState<Boolean>,
    modifier: Modifier,
    groupedMedia: MutableState<List<MediaStoreData>>? = null
) {
    val (shape, spacerHeight) = getDefaultShapeSpacerForPosition(position, 24.dp)
    val context = LocalContext.current

    val selectedItemsWithoutSection by remember {
        derivedStateOf {
            selectedItemsList.filter {
                it.type != MediaType.Section && it != MediaStoreData.dummyItem
            }
        }
    }

    val runOnUriGranted = remember { mutableStateOf(false) }
    val runOnDirGranted = remember { mutableStateOf(false) }

    GetDirectoryPermissionAndRun(
        absoluteDirPaths = listOf(album.mainPath),
        shouldRun = runOnDirGranted,
        onGranted = {
            runOnUriGranted.value = true
        },
        onRejected = {}
    )

    val mainViewModel = LocalMainViewModel.current
    val overwriteDate by mainViewModel.settings.Permissions.getOverwriteDateOnMove().collectAsStateWithLifecycle(initialValue = true)
    GetPermissionAndRun(
        uris = selectedItemsWithoutSection.map { it.uri },
        shouldRun = runOnUriGranted,
        onGranted = {
            show.value = false

            if (isMoving && album.paths.size == 1) {
                moveImageListToPath(
                    context = context,
                    list = selectedItemsWithoutSection,
                    destination = album.mainPath,
                    overwriteDate = overwriteDate,
                    basePath = album.mainPath.toBasePath()
                )

                if (groupedMedia != null) {
                    val newList = groupedMedia.value.toMutableList()
                    newList.removeAll(selectedItemsWithoutSection.toSet())
                    groupedMedia.value = newList
                }
            } else {
                val list = mutableListOf<Pair<Uri, String>>()

                album.paths.forEach { path ->
                    copyImageListToPath(
                        context = context,
                        list = selectedItemsWithoutSection,
                        destination = path,
                        overwriteDate = overwriteDate,
                        basePath = path.toBasePath()
                    ) { uri, path ->
                        if (isMoving) {
                            if (!list.contains(Pair(uri, path))) list.add(Pair(uri, path))
                        }
                    }
                }

                if (list.isNotEmpty()) {
                    setTrashedOnPhotoList(
                        context = context,
                        list = list,
                        trashed = true
                    )
                }
            }

            selectedItemsList.clear()
        }
    )

    val coroutineScope = rememberCoroutineScope()
    Row(
        modifier = modifier
            .height(88.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable {
                if (!album.isCustomAlbum) {
                    runOnDirGranted.value = true
                } else {
                    val items = selectedItemsWithoutSection

                    coroutineScope.launch(Dispatchers.IO) {
                        val data = mutableListOf<String>()
                        context.contentResolver.query(
                            LavenderContentProvider.CONTENT_URI,
                            arrayOf(LavenderMediaColumns.URI, LavenderMediaColumns.PARENT_ID),
                            "${LavenderMediaColumns.PARENT_ID} = ?",
                            arrayOf("${album.id}"),
                            null
                        )?.use { cursor ->
                            while (cursor.moveToNext()) {
                                val uriCol = cursor.getColumnIndexOrThrow(LavenderMediaColumns.URI)
                                val uri = cursor.getString(uriCol)

                                data.add(uri)
                            }
                        }

                        val inserted = context.contentResolver.bulkInsert(
                            LavenderContentProvider.CONTENT_URI,
                            items
                                .fastFilter { media ->
                                    media.uri.toString() !in data
                                }.fastMap { media ->
                                    ContentValues().apply {
                                        // no id since the content provider handles that on its own
                                        put(LavenderMediaColumns.URI, media.uri.toString())
                                        put(LavenderMediaColumns.PARENT_ID, album.id)
                                        put(LavenderMediaColumns.MIME_TYPE, media.mimeType)
                                        put(LavenderMediaColumns.DATE_TAKEN, media.dateTaken)
                                    }
                                }.toTypedArray()
                        )

                        Log.d(TAG, "Number of inserted items: $inserted")
                        Log.d(TAG, "Got album id ${album.id}")

                        if (inserted == 0) {
                            LavenderSnackbarController.pushEvent(
                                LavenderSnackbarEvents.MessageEvent(
                                    message = context.resources.getString(R.string.albums_already_contains_all),
                                    icon = R.drawable.error_2,
                                    duration = SnackbarDuration.Short
                                )
                            )
                            show.value = false
                        }
                    }

                    selectedItemsList.clear()
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(12.dp))

        GlideImage(
            model = data.uri,
            contentDescription = album.mainPath,
            contentScale = ContentScale.Crop,
            failure = placeholder(R.drawable.broken_image),
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = album.name,
            fontSize = TextUnit(16f, TextUnitType.Sp),
            textAlign = TextAlign.Start,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
        )

        if (album.isCustomAlbum) {
            Icon(
                painter = painterResource(id = R.drawable.art_track),
                contentDescription = stringResource(id = R.string.albums_is_custom),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(end = 2.dp)
            )
        }

        Spacer (modifier = Modifier.width(16.dp))
    }

    Spacer(
        modifier = Modifier
            .height(spacerHeight)
            .background(MaterialTheme.colorScheme.surface)
    )
}
