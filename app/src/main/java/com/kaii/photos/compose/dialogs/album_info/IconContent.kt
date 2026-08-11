package com.kaii.photos.compose.dialogs.album_info

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.kaii.photos.datastore.AlbumType

@Composable
fun IconContentVertical(
    albumInfo: () -> AlbumType,
    albums: () -> List<AlbumType>,
    autoDetectAlbums: () -> Boolean,
    itemCount: () -> Int,
    modifier: Modifier = Modifier,
    toggleSelectionMode: () -> Unit,
    editAlbum: (id: String, newInfo: AlbumType) -> Unit,
    renameAlbum: (newName: String) -> Unit,
    removeAlbum: (id: String) -> Unit,
    dismiss: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(CircleShape)
            .background(
                if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceContainerHigh
                else MaterialTheme.colorScheme.surfaceContainerLow
            )
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(
            space = 12.dp,
            alignment = Alignment.CenterVertically
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconContentImpl(
            albumInfo = albumInfo,
            albums = albums,
            autoDetectAlbums = autoDetectAlbums,
            itemCount = itemCount,
            modifier = Modifier.weight(1f),
            toggleSelectionMode = toggleSelectionMode,
            editAlbum = editAlbum,
            renameAlbum = renameAlbum,
            removeAlbum = removeAlbum,
            dismiss = dismiss
        )
    }
}

@Composable
fun IconContentHorizontal(
    albumInfo: () -> AlbumType,
    albums: () -> List<AlbumType>,
    autoDetectAlbums: () -> Boolean,
    itemCount: () -> Int,
    modifier: Modifier = Modifier,
    toggleSelectionMode: () -> Unit,
    editAlbum: (id: String, newInfo: AlbumType) -> Unit,
    renameAlbum: (newName: String) -> Unit,
    removeAlbum: (id: String) -> Unit,
    dismiss: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(
                if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceContainerHigh
                else MaterialTheme.colorScheme.surfaceContainerLow
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            space = 12.dp,
            alignment = Alignment.CenterHorizontally
        )
    ) {
        IconContentImpl(
            albumInfo = albumInfo,
            albums = albums,
            autoDetectAlbums = autoDetectAlbums,
            itemCount = itemCount,
            modifier = Modifier.weight(1f),
            toggleSelectionMode = toggleSelectionMode,
            editAlbum = editAlbum,
            renameAlbum = renameAlbum,
            removeAlbum = removeAlbum,
            dismiss = dismiss
        )
    }
}