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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kaii.photos.R
import com.kaii.photos.compose.FolderIsEmpty
import com.kaii.photos.compose.dialogs.getDefaultShapeSpacerForPosition
import com.kaii.photos.compose.widgets.ClearableTextField
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.state.AlbumGridState
import com.kaii.photos.di.appModule
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.permissions.files.rememberDirectoryPermissionManager
import com.kaii.photos.permissions.files.rememberFilePermissionManager
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MoveCopyAlbumListView(
    albumGridState: AlbumGridState = LocalContext.current.appModule.albumGridState,
    show: MutableState<Boolean>,
    currentAlbum: () -> AlbumType,
    isMoving: () -> Boolean,
    selectedItemsList: List<SelectionManager.SelectedItem>,
    insetsPadding: WindowInsets,
    allowedAlbumsFor: () -> List<KClass<out AlbumType>>,
    dismissInfoDialog: () -> Unit = {},
    clear: () -> Unit,
    onClick: (album: AlbumType) -> Unit
) {
    val originalAlbumsList by albumGridState.singleAlbums.collectAsStateWithLifecycle()

    var albumsList by remember { mutableStateOf(originalAlbumsList) }

    val searchedForText = remember { mutableStateOf("") }

    val state = rememberLazyListState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(searchedForText.value, originalAlbumsList, selectedItemsList.lastOrNull(), allowedAlbumsFor(), isMoving()) {
        albumsList = originalAlbumsList.filter { album ->
            album.name.contains(searchedForText.value, true)
                    && album.info.album::class in allowedAlbumsFor()
                    && if (isMoving()) album.id != currentAlbum().id else true
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
                )
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
                        AlbumsListItem(
                            album = albumsList[index],
                            position = if (index == albumsList.size - 1 && albumsList.size != 1) RowPosition.Bottom else if (albumsList.size == 1) RowPosition.Single else if (index == 0) RowPosition.Top else RowPosition.Middle,
                            selectedItemsList = selectedItemsList,
                            show = show,
                            dismissInfoDialog = dismissInfoDialog,
                            clear = clear,
                            modifier = Modifier
                                .fillParentMaxWidth(1f)
                                .padding(8.dp, 0.dp)
                                .animateItem(),
                            onClick = {
                                onClick(albumsList[index].info.album)
                            }
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
    album: AlbumGridState.Album.Single,
    position: RowPosition,
    selectedItemsList: List<SelectionManager.SelectedItem>,
    show: MutableState<Boolean>,
    modifier: Modifier,
    dismissInfoDialog: () -> Unit,
    clear: () -> Unit,
    onClick: () -> Unit
) {
    val (shape, spacerHeight) = getDefaultShapeSpacerForPosition(position, 24.dp)

    val filePermissionManager = rememberFilePermissionManager(
        onGranted = {
            show.value = false

            onClick()

            clear()
            dismissInfoDialog()
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
                if (album.info.album is AlbumType.Folder) {
                    dirPermissionManager.start(
                        directories = album.info.album.paths
                    )
                } else {
                    show.value = false

                    onClick()

                    clear()
                    dismissInfoDialog()
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(12.dp))

        GlideImage(
            model = album.info.thumbnail.uri,
            contentDescription = album.name,
            contentScale = ContentScale.Crop,
            failure = placeholder(R.drawable.broken_image),
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            it.signature(album.info.thumbnail.signature)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = album.name,
            fontSize = TextUnit(16f, TextUnitType.Sp),
            textAlign = TextAlign.Start,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
        )

        if (album.info.album !is AlbumType.Folder) {
            Icon(
                painter = painterResource(
                    id =
                        if (album.info.album is AlbumType.Custom) R.drawable.art_track
                        else R.drawable.cloud
                ),
                contentDescription = stringResource(
                    id =
                        if (album.info.album is AlbumType.Custom) R.string.albums_is_custom
                        else R.string.albums_is_cloud
                ),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(end = 2.dp)
                    .size(
                        if (album.info.album is AlbumType.Custom) 22.dp
                        else 20.dp
                    )
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
