package com.kaii.photos.compose.grids.media

import android.util.Log
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.ShowSelectedState
import com.kaii.photos.helpers.EncryptionManager
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.helpers.formatLikeANormalPerson
import com.kaii.photos.helpers.getSecuredCacheImageForFile
import com.kaii.photos.helpers.paging.PhotoLibraryUIModel
import com.kaii.photos.mediastore.ImmichInfo
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.getThumbnailIv
import com.kaii.photos.mediastore.isRawImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

private const val TAG = "com.kaii.photos.compose.grids.media.MediaItem"

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
@Composable
fun MediaItem(
    item: PhotoLibraryUIModel.MediaImpl,
    isSecureMedia: Boolean,
    isSelecting: () -> Boolean,
    thumbnailSettings: () -> Pair<Boolean, Int>,
    isDragSelecting: MutableState<Boolean>,
    isMediaPicker: Boolean,
    useRoundedCorners: () -> Boolean,
    selected: () -> Boolean,
    toggleSelection: () -> Unit,
    navigateToItem: () -> Unit
) {
    val animatedItemCornerRadius by animateDpAsState(
        targetValue = if (selected()) 16.dp else 0.dp,
        animationSpec = tween(
            durationMillis = 150,
        ),
        label = "animate corner radius of selected item"
    )
    val animatedItemScale by animateFloatAsState(
        targetValue = if (selected()) 0.8f else 1f,
        animationSpec = tween(
            durationMillis = 150
        ),
        label = "animate scale of selected item"
    )

    Box(
        modifier = Modifier
            .semantics {
                testTagsAsResourceId = true
            }
            .testTag("main_lazy_column_item")
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(if (useRoundedCorners()) 8.dp else 0.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            .clickable(
                enabled = !isDragSelecting.value
            ) {
                if (isMediaPicker) {
                    isDragSelecting.value = true

                    toggleSelection()
                } else {
                    if (isSelecting()) {
                        toggleSelection()
                    } else {
                        navigateToItem()
                    }
                }
            }
    ) {
        // TODO: possibly move to a less messy and horrible decrypting implementation
        var model by remember { mutableStateOf<Any?>(null) }
        val context = LocalContext.current

        LaunchedEffect(isSecureMedia) {
            if (!isSecureMedia || model != null) return@LaunchedEffect

            item as PhotoLibraryUIModel.SecuredMedia

            model =
                withContext(Dispatchers.IO) {
                    try {
                        val thumbnailIv =
                            item.bytes!!.getThumbnailIv() // get thumbnail iv from video

                        EncryptionManager.decryptBytes(
                            bytes = getSecuredCacheImageForFile(
                                fileName = item.item.displayName,
                                context = context
                            ).readBytes(),
                            iv = thumbnailIv
                        )
                    } catch (e: Throwable) {
                        Log.d(TAG, e.toString())
                        e.printStackTrace()

                        item.item.uri.toUri().path
                    }
                }
        }

        GlideImage(
            model = when {
                isSecureMedia -> model

                item.item.immichUrl != null -> ImmichInfo(
                    thumbnail = item.item.immichThumbnail!!,
                    original = item.item.immichUrl!!,
                    hash = item.item.hash!!,
                    accessToken = item.accessToken!!,
                    useThumbnail = true
                )

                else -> item.item.uri
            },
            contentDescription = item.item.displayName,
            contentScale = ContentScale.Crop,
            failure = placeholder(R.drawable.broken_image),
            modifier = Modifier
                .fillMaxSize(1f)
                .align(Alignment.Center)
                .scale(animatedItemScale)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(animatedItemCornerRadius))
        ) {
            if (isSecureMedia) {
                it.signature(item.signature())
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
            } else if (thumbnailSettings().second == 0) {
                it.signature(item.signature())
                    .diskCacheStrategy(
                        if (thumbnailSettings().first) DiskCacheStrategy.ALL
                        else DiskCacheStrategy.NONE
                    )
            } else {
                it.signature(item.signature())
                    .diskCacheStrategy(
                        if (thumbnailSettings().first) DiskCacheStrategy.ALL
                        else DiskCacheStrategy.NONE
                    )
                    .override(thumbnailSettings().second)
            }
        }

        if (item.item.type == MediaType.Video) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 2.dp, y = (-2).dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(start = 4.dp, top = 0.dp, end = 6.dp, bottom = 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    space = 4.dp,
                    alignment = Alignment.Start
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.movie_filled),
                    contentDescription = stringResource(id = R.string.file_is_a_video),
                    tint = Color.White,
                    modifier = Modifier
                        .size(20.dp)
                )

                if (item.item.duration != null) {
                    // show video duration text on the thumbnail
                    Text(
                        text = (item.item.duration as Long).seconds.formatLikeANormalPerson().first,
                        color = Color.White,
                        fontSize = TextStylingConstants.EXTRA_SMALL_TEXT_SIZE.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .offset(y = 1.dp)
                    )
                }
            }
        }

        if (item.item.isRawImage()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(2.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.raw_on),
                    contentDescription = stringResource(id = R.string.media_is_raw),
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.Center)
                )
            }
        }

        ShowSelectedState(
            isSelected = selected,
            showIcon = isSelecting(),
            modifier = Modifier
                .align(Alignment.TopEnd)
        )
    }
}