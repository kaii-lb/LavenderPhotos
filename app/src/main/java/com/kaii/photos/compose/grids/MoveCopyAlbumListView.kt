package com.kaii.photos.compose.grids

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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.util.fastMap
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.FolderIsEmpty
import com.kaii.photos.compose.dialogs.getDefaultShapeSpacerForPosition
import com.kaii.photos.compose.widgets.ClearableTextField
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.state.AlbumGridState
import com.kaii.photos.datastore.state.rememberAlbumGridState
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.copyImageListToPath
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.moveImageListToPath
import com.kaii.photos.helpers.permanentlyDeletePhotoList
import com.kaii.photos.mediastore.content_provider.LavenderContentProvider
import com.kaii.photos.mediastore.content_provider.LavenderMediaColumns
import com.kaii.photos.permissions.files.rememberDirectoryPermissionManager
import com.kaii.photos.permissions.files.rememberFilePermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MoveCopyAlbumListView(
    show: MutableState<Boolean>,
    selectedItemsList: List<SelectionManager.SelectedItem>,
    isMoving: Boolean,
    insetsPadding: WindowInsets,
    onMoveMedia: () -> Unit = {},
    dismissInfoDialog: () -> Unit = {},
    clear: () -> Unit
) {
    val albumGridState = rememberAlbumGridState()
    val originalAlbumsList by albumGridState.albums.collectAsStateWithLifecycle()

    var albumsList by remember { mutableStateOf(originalAlbumsList) }

    val searchedForText = remember { mutableStateOf("") }

    val state = rememberLazyListState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(searchedForText.value, originalAlbumsList, selectedItemsList.lastOrNull()) {
        albumsList = originalAlbumsList.filter {
            it.info.name.contains(searchedForText.value, true)
        }

        if (albumsList.isNotEmpty()) state.scrollToItem(0)
    }

    LaunchedEffect(show.value) {
        searchedForText.value = ""
    }

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
                            albumsList[it].info.id
                        }
                    ) { index ->
                        AlbumsListItem(
                            album = albumsList[index],
                            position = if (index == albumsList.size - 1 && albumsList.size != 1) RowPosition.Bottom else if (albumsList.size == 1) RowPosition.Single else if (index == 0) RowPosition.Top else RowPosition.Middle,
                            selectedItemsList = selectedItemsList,
                            isMoving = isMoving,
                            show = show,
                            onMoveMedia = onMoveMedia,
                            dismissInfoDialog = dismissInfoDialog,
                            clear = clear,
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
    album: AlbumGridState.Album,
    position: RowPosition,
    selectedItemsList: List<SelectionManager.SelectedItem>,
    isMoving: Boolean,
    show: MutableState<Boolean>,
    modifier: Modifier,
    onMoveMedia: () -> Unit,
    dismissInfoDialog: () -> Unit,
    clear: () -> Unit
) {
    val (shape, spacerHeight) = getDefaultShapeSpacerForPosition(position, 24.dp)
    val context = LocalContext.current

    val mainViewModel = LocalMainViewModel.current
    val coroutineScope = rememberCoroutineScope()
    val preserveDate by mainViewModel.settings.permissions.getPreserveDateOnMove().collectAsStateWithLifecycle(initialValue = true)

    val filePermissionManager = rememberFilePermissionManager(
        onGranted = {
            show.value = false

            mainViewModel.launch(Dispatchers.IO) {
                if (isMoving && album.info.paths.size == 1) {
                    onMoveMedia()

                    moveImageListToPath(
                        context = context,
                        list = selectedItemsList,
                        destination = album.info.mainPath,
                        preserveDate = preserveDate
                    )
                } else {
                    val list = mutableListOf<MediaStoreData>()

                    album.info.paths.forEach { path ->
                        copyImageListToPath(
                            context = context,
                            list = selectedItemsList,
                            destination = path,
                            overwriteDate = preserveDate
                        ) { media ->
                            if (isMoving && !list.contains(media)) {
                                list.add(media)
                            }
                        }
                    }

                    if (list.isNotEmpty()) {
                        onMoveMedia()

                        permanentlyDeletePhotoList(
                            context = context,
                            list = list.fastMap { it.uri.toUri() }
                        )
                    }
                }

                clear()
                dismissInfoDialog()
            }
        }
    )

    val dirPermissionManager = rememberDirectoryPermissionManager(
        onGranted = {
            filePermissionManager.get(
                uris = selectedItemsList.fastMap { it.toUri() }
            )
        }
    )

    Row(
        modifier = modifier
            .height(88.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable {
                if (!album.info.isCustomAlbum) {
                    dirPermissionManager.start(
                        directories = album.info.paths
                    )
                } else {
                    show.value = false

                    coroutineScope.launch(Dispatchers.IO) {
                        val data = mutableListOf<String>()
                        context.contentResolver.query(
                            LavenderContentProvider.CONTENT_URI,
                            arrayOf(LavenderMediaColumns.URI, LavenderMediaColumns.PARENT_ID),
                            "${LavenderMediaColumns.PARENT_ID} = ?",
                            arrayOf("${album.info.id}"),
                            null
                        )?.use { cursor ->
                            while (cursor.moveToNext()) {
                                val uriCol = cursor.getColumnIndexOrThrow(LavenderMediaColumns.URI)
                                val uri = cursor.getString(uriCol)

                                data.add(uri)
                            }
                        }

                        // TODO:
                        // val inserted = context.contentResolver.bulkInsert(
                        //     LavenderContentProvider.CONTENT_URI,
                        //     items
                        //         .fastFilter { media ->
                        //             media.uri !in data
                        //         }.fastMap { media ->
                        //             ContentValues().apply {
                        //                 // no id since the content provider handles that on its own
                        //                 put(LavenderMediaColumns.URI, media.uri)
                        //                 put(LavenderMediaColumns.PARENT_ID, album.id)
                        //                 put(LavenderMediaColumns.MIME_TYPE, media.mimeType)
                        //                 put(LavenderMediaColumns.DATE_TAKEN, media.dateTaken)
                        //             }
                        //         }.toTypedArray()
                        // )

                        // Log.d(TAG, "Number of inserted items: $inserted")
                        // Log.d(TAG, "Got album id ${album.id}")
                        //
                        // if (inserted == 0) {
                        //     LavenderSnackbarController.pushEvent(
                        //         LavenderSnackbarEvents.MessageEvent(
                        //             message = resources.getString(R.string.albums_already_contains_all),
                        //             icon = R.drawable.error_2,
                        //             duration = SnackbarDuration.Short
                        //         )
                        //     )
                        // }
                    }

                    clear()
                    dismissInfoDialog()
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(12.dp))

        GlideImage(
            model = album.thumbnail,
            contentDescription = album.info.name,
            contentScale = ContentScale.Crop,
            failure = placeholder(R.drawable.broken_image),
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            it.signature(album.signature)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = album.info.name,
            fontSize = TextUnit(16f, TextUnitType.Sp),
            textAlign = TextAlign.Start,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
        )

        if (album.info.isCustomAlbum) {
            Icon(
                painter = painterResource(id = R.drawable.art_track),
                contentDescription = stringResource(id = R.string.albums_is_custom),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(end = 2.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))
    }

    Spacer(
        modifier = Modifier
            .height(spacerHeight)
            .background(MaterialTheme.colorScheme.surface)
    )
}
