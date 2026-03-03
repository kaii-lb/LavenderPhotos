package com.kaii.photos.compose.widgets.albums

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.state.AlbumGridState
import com.kaii.photos.helpers.TextStylingConstants

@Preview
@Composable
private fun AlbumFolderPreview() {
    AlbumFolder(
        name = "Pets",
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
        ),
        isSelected = false,
        info = ImmichBasicInfo.Empty,
        onClick = {}
    )
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun AlbumFolder(
    name: String,
    albums: List<AlbumGridState.Album>,
    isSelected: Boolean,
    info: ImmichBasicInfo,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 0.9f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .scale(animatedScale)
            .padding(6.dp)
            .clip(RoundedCornerShape(size = 24.dp))
            .background(backgroundColor)
            .clickable {
                if (!isSelected) onClick()
            }
            .padding(8.dp, 8.dp, 8.dp, 4.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .aspectRatio(1f),
            verticalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                alignment = Alignment.Top
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    space = 8.dp,
                    alignment = Alignment.CenterHorizontally
                )
            ) {
                albums.take(2).forEach { album ->
                    AlbumGlideImage(
                        album = album,
                        info = info
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
                    alignment = Alignment.CenterHorizontally
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
                    AlbumGlideImage(
                        album = album,
                        info = info
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                alignment = Alignment.Start
            )
        ) {
            Text(
                text = name,
                fontSize = TextStylingConstants.SMALL_TEXT_SIZE.sp
            )
        }
    }
}