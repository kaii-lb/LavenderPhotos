package com.kaii.photos.compose.grids

import android.content.ContentValues
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
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
import com.kaii.photos.MainActivity.Companion.mainViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.FolderIsEmpty
import com.kaii.photos.compose.SearchTextField
import com.kaii.photos.compose.dialogs.getDefaultShapeSpacerForPosition
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.AlbumsList
import com.kaii.photos.helpers.GetDirectoryPermissionAndRun
import com.kaii.photos.helpers.GetPermissionAndRun
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.baseInternalStorageDirectory
import com.kaii.photos.helpers.copyImageListToPath
import com.kaii.photos.helpers.moveImageListToPath
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
    val originalAlbumsList by mainViewModel.settings.AlbumsList.getAlbumsList()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    if (originalAlbumsList == emptyList<String>()) return

    val albumsViewModel: AlbumsViewModel = viewModel(
        factory = AlbumsViewModelFactory(context, originalAlbumsList.fastFilter { !it.isCustomAlbum })
    )
    val dataList by albumsViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)

    var albumsList by remember { mutableStateOf(originalAlbumsList) }

    val searchedForText = remember { mutableStateOf("") }

    val state = rememberLazyListState()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(key1 = searchedForText.value) {
        albumsList = originalAlbumsList.filter {
            it.name.contains(searchedForText.value, true)
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
                SearchTextField(
                    searchedForText = searchedForText,
                    placeholder = "Search for an album's name",
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .height(56.dp)
                        .padding(16.dp, 0.dp),
                    onClear = {
                        searchedForText.value = ""
                    },
                    onSearch = {}
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (albumsList.isEmpty()) {
                FolderIsEmpty(
                    emptyText = "No such albums exists",
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

                        AlbumsListItem(
                            album = album,
                            data =
                                dataList.find { item ->
                                    item.first.id == album.id
                                }?.second ?: MediaStoreData.dummyItem,
                            position = if (index == albumsList.size - 1 && albumsList.size != 1) RowPosition.Bottom else if (albumsList.size == 1) RowPosition.Single else if (index == 0) RowPosition.Top else RowPosition.Middle,
                            selectedItemsList = selectedItemsList,
                            isMoving = isMoving,
                            show = show,
                            groupedMedia = groupedMedia,
                            modifier = Modifier
                                .fillParentMaxWidth(1f)
                                .padding(8.dp, 0.dp)
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
                it.type != MediaType.Section && it != MediaStoreData()
            }
        }
    }

    val runOnUriGranted = remember { mutableStateOf(false) }
    val runOnDirGranted = remember { mutableStateOf(false) }

    GetDirectoryPermissionAndRun(
        absoluteDirPaths = listOf(baseInternalStorageDirectory + album),
        shouldRun = runOnDirGranted,
        onGranted = {
            runOnUriGranted.value = true
        },
        onRejected = {}
    )

    GetPermissionAndRun(
        uris = selectedItemsWithoutSection.map { it.uri },
        shouldRun = runOnUriGranted,
        onGranted = {
            show.value = false

            if (isMoving) {
                moveImageListToPath(
                    context,
                    selectedItemsWithoutSection,
                    album.mainPath
                )

                if (groupedMedia != null) {
                    val newList = groupedMedia.value.toMutableList()
                    newList.removeAll(selectedItemsWithoutSection.toSet())
                    groupedMedia.value = newList
                }
            } else {
                copyImageListToPath(
                    context,
                    selectedItemsWithoutSection,
                    album.mainPath
                )
            }

            selectedItemsList.clear()
        }
    )

    Row(
        modifier = modifier
            .height(88.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable {
                if (!album.isCustomAlbum) {
                    runOnDirGranted.value = true
                } else {
                    val data = mutableListOf<Long>()
                    context.contentResolver.query(
                        LavenderContentProvider.CONTENT_URI,
                        arrayOf(LavenderMediaColumns.ID),
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        while (cursor.moveToNext()) {
                            val idCol = cursor.getColumnIndexOrThrow(LavenderMediaColumns.ID)
                            val id = cursor.getLong(idCol)

                            data.add(id)
                        }
                    }

                    val inserted = context.contentResolver.bulkInsert(
                        LavenderContentProvider.CONTENT_URI,
                        selectedItemsWithoutSection
                            .fastFilter { media ->
                                media.id !in data
                            }.fastMap { media ->
                                ContentValues().apply {
                                    put(LavenderMediaColumns.ID, media.id)
                                    put(LavenderMediaColumns.URI, media.uri.toString())
                                    put(LavenderMediaColumns.PARENT_ID, album.id)
                                    put(LavenderMediaColumns.MIME_TYPE, media.mimeType)
                                }
                            }.toTypedArray()
                    )

                    Log.d(TAG, "Number of inserted items: $inserted")
                    Log.d(TAG, "Got album id ${album.id}")

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
    }

    Spacer(
        modifier = Modifier
            .height(spacerHeight)
            .background(MaterialTheme.colorScheme.surface)
    )
}
