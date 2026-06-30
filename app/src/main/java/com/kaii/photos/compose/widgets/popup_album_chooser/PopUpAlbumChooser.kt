package com.kaii.photos.compose.widgets.popup_album_chooser

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.PhotosApplication
import com.kaii.photos.R
import com.kaii.photos.compose.FolderIsEmpty
import com.kaii.photos.compose.widgets.ClearableTextField
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.state.AlbumGridState
import com.kaii.photos.helpers.RowPosition
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PopUpAlbumChooser(
    selectedAlbums: SnapshotStateList<String>,
    sheetState: SheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    ),
    albumGridState: AlbumGridState = PhotosApplication.appModule.albumGridState,
    key: (album: AlbumType) -> String,
    filter: (searchedForText: String, list: List<AlbumGridState.Album.Single>) -> List<AlbumGridState.Album.Single>,
    onDismiss: () -> Unit
) {
    val state = rememberLazyListState()

    val originalAlbumsList by albumGridState.singleAlbums.collectAsStateWithLifecycle()
    var albumsList by remember { mutableStateOf(originalAlbumsList) }
    var searchedForText by remember { mutableStateOf("") }

    LaunchedEffect(searchedForText, originalAlbumsList) {
        albumsList = filter(searchedForText, originalAlbumsList)

        if (albumsList.isNotEmpty()) state.scrollToItem(0)
    }

    ModalBottomSheet(
        sheetState = sheetState,
        tonalElevation = 16.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = false
        ),
        containerColor = MaterialTheme.colorScheme.background,
        onDismissRequest = onDismiss,
        contentWindowInsets = {
            WindowInsets.safeContent.add(WindowInsets(bottom = 8.dp))
        },
        modifier = Modifier
            .statusBarsPadding()
    ) {
        val coroutineScope = rememberCoroutineScope()
        BackHandler(
            enabled = !WindowInsets.isImeVisible
        ) {
            coroutineScope.launch {
                sheetState.hide()
                onDismiss()
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
                .fillMaxWidth()
        ) {
            ClearableTextField(
                value = searchedForText,
                onValueChange = { searchedForText = it },
                placeholder = stringResource(id = R.string.albums_search_for),
                icon = R.drawable.search,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                onClear = {
                    searchedForText = ""
                },
                onConfirm = {}
            )
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
                    .fillMaxSize()
                    .padding(start = 16.dp, top = 8.dp, end = 16.dp)
                    .clip(RoundedCornerShape(size = 24.dp)),
                verticalArrangement = Arrangement.spacedBy(
                    space = 2.dp,
                    alignment = Alignment.Top
                ),
                horizontalAlignment = Alignment.Start
            ) {
                items(
                    count = albumsList.size,
                    key = {
                        albumsList[it].id
                    }
                ) { index ->
                    val album = key(albumsList[index].info.album)

                    CheckableAlbumItem(
                        album = albumsList[index],
                        selected = { selectedAlbums.contains(album) },
                        position =
                            when {
                                albumsList.size == 1 -> RowPosition.Single
                                index == 0 -> RowPosition.Top
                                index == albumsList.size - 1 -> RowPosition.Bottom
                                else -> RowPosition.Middle
                            },
                        onCheckedChange = {
                            if (selectedAlbums.contains(album)) {
                                selectedAlbums.remove(album)
                            } else {
                                selectedAlbums.add(album)
                            }
                        }
                    )
                }
            }
        }
    }
}