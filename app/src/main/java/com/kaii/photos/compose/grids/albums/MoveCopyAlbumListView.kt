package com.kaii.photos.compose.grids.albums

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.R
import com.kaii.photos.compose.FolderIsEmpty
import com.kaii.photos.compose.widgets.ClearableTextField
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.state.AlbumGridState
import com.kaii.photos.di.appModule
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.grid_management.SelectionManager
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
                    placeholder = stringResource(id = R.string.albums_search_for),
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
                        MoveCopyAlbumsListItem(
                            album = albumsList[index],
                            position =
                                when {
                                    index == albumsList.size - 1 && albumsList.size != 1 -> RowPosition.Bottom

                                    albumsList.size == 1 -> RowPosition.Single

                                    index == 0 -> RowPosition.Top

                                    else -> RowPosition.Middle
                                },
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

