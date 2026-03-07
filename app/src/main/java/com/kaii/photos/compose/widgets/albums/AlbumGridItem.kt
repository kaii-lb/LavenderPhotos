package com.kaii.photos.compose.widgets.albums

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.kaii.photos.R
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.state.AlbumGridState
import com.kaii.photos.helpers.TextStylingConstants

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun AlbumGridItem(
    album: AlbumGridState.Album.Single,
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
            .wrapContentHeight()
            .fillMaxWidth()
            .scale(animatedScale)
            .padding(6.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .clickable {
                if (!isSelected) onClick()
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(1f)
                .padding(8.dp, 8.dp, 8.dp, 4.dp)
                .clip(RoundedCornerShape(16.dp)),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AlbumGlideImage(
                albumInfo = album.info,
                info = info
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = " ${album.name}",
                    fontSize = TextStylingConstants.SMALL_TEXT_SIZE.sp,
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (album.info.album !is AlbumType.Folder) { // TODO: introduce icon for immich
                    Icon(
                        painter = painterResource(id = R.drawable.art_track),
                        contentDescription = stringResource(id = R.string.albums_is_custom),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(end = 2.dp)
                    )
                }
            }
        }
    }
}