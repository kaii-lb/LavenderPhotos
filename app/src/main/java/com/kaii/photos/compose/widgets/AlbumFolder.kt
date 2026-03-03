package com.kaii.photos.compose.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.state.AlbumGridState

@Preview
@Composable
private fun AlbumFolderPreview() {
    AlbumFolder(
        albums = listOf(
            AlbumGridState.Album(
                info = AlbumType.Album.Empty,
                thumbnails = emptyList(),
                date = 0L
            ),
            AlbumGridState.Album(
                info = AlbumType.Album.Empty,
                thumbnails = emptyList(),
                date = 0L
            ),
            AlbumGridState.Album(
                info = AlbumType.Album.Empty,
                thumbnails = emptyList(),
                date = 0L
            )
        )
    )
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun AlbumFolder(
    albums: List<AlbumGridState.Album>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(size = 24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = Alignment.Top
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                alignment = Alignment.Start
            )
        ) {
            albums.take(2).forEach { album ->
                GlideImage(
                    model = album.thumbnails,
                    contentDescription = album.info.name,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                alignment = Alignment.Start
            )
        ) {
            val overflow = if (albums.size > 2) {
                albums.takeLast(albums.size - 3) + (0..4 - albums.size).map {
                    AlbumGridState.Album(
                        info = AlbumType.Album.Empty,
                        thumbnails = emptyList(),
                        date = 0L
                    )
                }
            } else emptyList()

            overflow.forEach { album ->
                GlideImage(
                    model = album.thumbnails,
                    contentDescription = album.info.name,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}