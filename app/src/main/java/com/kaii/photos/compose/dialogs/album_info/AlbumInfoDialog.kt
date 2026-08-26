package com.kaii.photos.compose.dialogs.album_info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.helpers.TextStylingConstants

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlbumInfoDialog(
    albumInfo: () -> AlbumType,
    albums: () -> List<AlbumType>,
    autoDetectAlbums: () -> Boolean,
    sheetState: SheetState,
    modifier: Modifier = Modifier,
    itemCount: () -> Int,
    albumSize: () -> String,
    toggleSelectionMode: () -> Unit,
    editAlbum: (id: String, newInfo: AlbumType) -> Unit,
    renameAlbum: (newName: String) -> Unit,
    removeAlbum: (id: String) -> Unit,
    dismiss: () -> Unit
) {
    // remove (weird) drag handle ripple
    CompositionLocalProvider(
        LocalRippleConfiguration provides null
    ) {
        val isLandscape by rememberDeviceOrientation()

        ModalBottomSheet(
            sheetState = sheetState,
            tonalElevation = 16.dp,
            shape = RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp),
            containerColor = MaterialTheme.colorScheme.background,
            onDismissRequest = dismiss,
            contentWindowInsets = {
                if (!isLandscape) WindowInsets.systemBars
                else WindowInsets()
            },
            modifier = modifier
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // reset ripple for normal buttons
            CompositionLocalProvider(
                LocalRippleConfiguration provides RippleConfiguration()
            ) {
                if (isLandscape) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 0.dp, start = 16.dp, bottom = 16.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(
                            space = 16.dp,
                            alignment = Alignment.CenterHorizontally
                        )
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(
                                space = 12.dp,
                                alignment = Alignment.CenterVertically
                            ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = albumInfo().name,
                                fontSize = TextStylingConstants.MEDIUM_TEXT_SIZE.sp,
                                fontWeight = FontWeight.Bold
                            )

                            IconContentVertical(
                                albumInfo = albumInfo,
                                albums = albums,
                                autoDetectAlbums = autoDetectAlbums,
                                itemCount = itemCount,
                                toggleSelectionMode = toggleSelectionMode,
                                editAlbum = editAlbum,
                                renameAlbum = renameAlbum,
                                removeAlbum = removeAlbum,
                                dismiss = dismiss
                            )
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(
                                space = 12.dp,
                                alignment = Alignment.CenterVertically
                            ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(id = R.string.albums_info),
                                fontSize = TextStylingConstants.MEDIUM_TEXT_SIZE.sp,
                                fontWeight = FontWeight.Bold
                            )

                            if (albumInfo() is AlbumType.Folder || albumInfo() is AlbumType.SAFFolder) {
                                NestedDisplayToggle(
                                    album = { albumInfo() },
                                    editAlbum = editAlbum
                                )
                            }

                            InfoContent(
                                albumInfo = albumInfo,
                                itemCount = itemCount,
                                albumSize = albumSize,
                                modifier = Modifier
                                    .weight(1f)
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 0.dp, start = 16.dp, bottom = 16.dp, end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(
                            space = 16.dp,
                            alignment = Alignment.Top
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = albumInfo().name,
                            fontSize = TextStylingConstants.MEDIUM_TEXT_SIZE.sp,
                            fontWeight = FontWeight.Bold
                        )

                        IconContentHorizontal(
                            albumInfo = albumInfo,
                            albums = albums,
                            autoDetectAlbums = autoDetectAlbums,
                            itemCount = itemCount,
                            toggleSelectionMode = toggleSelectionMode,
                            editAlbum = editAlbum,
                            removeAlbum = removeAlbum,
                            renameAlbum = renameAlbum,
                            dismiss = dismiss
                        )

                        Text(
                            text = stringResource(id = R.string.albums_info),
                            fontSize = TextStylingConstants.MEDIUM_TEXT_SIZE.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (albumInfo() is AlbumType.Folder || albumInfo() is AlbumType.SAFFolder) {
                            NestedDisplayToggle(
                                album = { albumInfo() },
                                editAlbum = editAlbum
                            )
                        }

                        InfoContent(
                            albumInfo = albumInfo,
                            itemCount = itemCount,
                            albumSize = albumSize,
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}