package com.kaii.photos.compose.widgets.albums

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.shimmerEffect
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.state.AlbumGridState
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.mediastore.ImmichInfo
import kotlinx.coroutines.delay

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun AlbumGlideImage(
    albumInfo: AlbumGridState.Info,
    info: ImmichBasicInfo
) {
    AnimatedContent(
        targetState = albumInfo.thumbnail.uri.isNotBlank(),
        transitionSpec = {
            fadeIn(
                animationSpec = tween(
                    durationMillis = AnimationConstants.DURATION_EXTRA_LONG
                )
            ).togetherWith(
                fadeOut(
                    animationSpec = tween(
                        durationMillis = AnimationConstants.DURATION_EXTRA_LONG
                    )
                )
            )
        },
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
    ) { state ->
        if (state) {
            GlideImage(
                model =
                    if ((albumInfo.album is AlbumType.Cloud)) ImmichInfo(
                        thumbnail = albumInfo.thumbnail.uri,
                        original = albumInfo.thumbnail.uri,
                        hash = "",
                        accessToken = info.accessToken,
                        useThumbnail = true
                    ) else albumInfo.thumbnail.uri,
                contentDescription = albumInfo.album.name,
                contentScale = ContentScale.Crop,
                failure = placeholder(R.drawable.broken_image),
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            ) {
                it.signature(albumInfo.thumbnail.signature)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
            }
        } else {
            var timedOut by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(5000)
                timedOut = true
            }

            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .then(
                        if (!timedOut) {
                            Modifier.shimmerEffect(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                highlightColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                        } else Modifier.background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    )
            )
        }
    }
}