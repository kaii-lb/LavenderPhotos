package com.kaii.photos.compose.grids.albums

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.R
import com.kaii.photos.compose.FolderIsEmpty
import com.kaii.photos.compose.widgets.ClearableTextField
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.widgets.popup_chooser_state.MoveCopyAlbumListState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MoveCopyAlbumListView(
    popupChooserState: MoveCopyAlbumListState,
    show: MutableState<Boolean>,
    insetsPadding: WindowInsets,
    dismissInfoDialog: () -> Unit = {},
    clear: () -> Unit,
    onClick: (album: AlbumType) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val albumsList by popupChooserState.albumsList.collectAsStateWithLifecycle()
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    LaunchedEffect(show.value) {
        popupChooserState.clear()
    }

    val query by popupChooserState.query.collectAsStateWithLifecycle()

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
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(
                        space = 8.dp,
                        alignment = Alignment.Top
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ClearableTextField(
                        value = query,
                        onValueChange = popupChooserState::search,
                        placeholder = stringResource(id = R.string.albums_search_for),
                        icon = R.drawable.search,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        onClear = popupChooserState::clear,
                        onConfirm = {}
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(
                                state = rememberScrollState()
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(
                            space = 8.dp,
                            alignment = Alignment.Start
                        )
                    ) {
                        val activeFilter by popupChooserState.filter.collectAsStateWithLifecycle()

                        MoveCopyAlbumListState.Filters.entries.forEach { filter ->
                            ElevatedFilterChip(
                                selected = activeFilter == filter,
                                onClick = {
                                    popupChooserState.applyFilter(filter)
                                },
                                label = {
                                    Text(
                                        text = stringResource(id = filter.label),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                shapes = FilterChipDefaults.shapes()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (albumsList.isEmpty()) {
                FolderIsEmpty(
                    emptyText = stringResource(id = R.string.albums_non_existent),
                    emptyIconResId = R.drawable.error,
                    backgroundColor = Color.Transparent
                )
            } else {
                val info by popupChooserState.immichInfo.collectAsStateWithLifecycle(initialValue = ImmichBasicInfo.Empty)

                LazyColumn(
                    state = popupChooserState.state,
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
                            info = { info },
                            show = show,
                            dismissInfoDialog = dismissInfoDialog,
                            clear = clear,
                            modifier = Modifier
                                .fillParentMaxWidth()
                                .padding(horizontal = 8.dp)
                                .animateItem(),
                            onClick = {
                                albumsList.getOrNull(index)?.info?.album?.let {
                                    onClick(it)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

