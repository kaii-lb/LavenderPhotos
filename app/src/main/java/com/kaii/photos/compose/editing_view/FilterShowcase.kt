package com.kaii.photos.compose.editing_view

import android.media.MediaMetadataRetriever
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.kaii.photos.compose.widgets.shimmerEffect
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.helpers.editing.DrawingPaintState
import com.kaii.photos.helpers.editing.ImageModification
import com.kaii.photos.helpers.editing.MediaAdjustments
import com.kaii.photos.helpers.editing.MediaColorFilters
import com.kaii.photos.helpers.editing.SharedModification
import com.kaii.photos.helpers.editing.VideoModification
import com.kaii.photos.helpers.editing.withColorFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun FilterShowcase(
    image: ImageBitmap?,
    filter: MediaColorFilters,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedContent(
            targetState = image != null,
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
                .weight(1f)
                .fillMaxWidth(1f)
                .clip(RoundedCornerShape(12.dp))
        ) { state ->
            if (state) {
                Image(
                    bitmap = image!!,
                    contentDescription = stringResource(id = filter.title),
                    colorFilter = ColorFilter.colorMatrix(filter.matrix),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(1f)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .shimmerEffect(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            highlightColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(id = filter.title),
            fontSize = TextUnit(TextStylingConstants.EXTRA_LARGE_TEXT_SIZE, TextUnitType.Sp),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth(1f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(id = filter.tag),
                fontSize = TextUnit(TextStylingConstants.SMALL_TEXT_SIZE, TextUnitType.Sp),
                textAlign = TextAlign.Start
            )

            Text(
                text = stringResource(id = filter.description),
                fontSize = TextUnit(TextStylingConstants.SMALL_TEXT_SIZE, TextUnitType.Sp),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun VideoFilterPage(
    drawingPaintState: DrawingPaintState,
    currentVideoPosition: MutableFloatState,
    absolutePath: String,
    allowedToRefresh: Boolean,
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {
    var bitmap: ImageBitmap? by remember { mutableStateOf(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(currentVideoPosition.floatValue, allowedToRefresh) {
        if (allowedToRefresh) return@LaunchedEffect

        coroutineScope.launch(Dispatchers.IO) {
            val metadata = MediaMetadataRetriever()
            metadata.setDataSource(absolutePath)

            // just so the image doesn't flash a million times and we don't cause a million recompositions
            var localBitmap = ImageBitmap(8, 8)

            metadata.getFrameAtTime((currentVideoPosition.floatValue * 1000 * 1000).toLong())?.let {
                localBitmap = it.asImageBitmap()
            }

            // sort by index since order of application is very important
            val adjustments = drawingPaintState.modifications
                .mapNotNull { it as? VideoModification.Adjustment }
                .sortedBy {
                    MediaAdjustments.entries.indexOf(it.type)
                }

            // apply color filters in order
            adjustments.forEach {
                localBitmap = localBitmap.withColorFilter(ColorMatrix(it.type.getMatrix(it.value)))
            }
            bitmap = localBitmap
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        drawingPaintState.modifications.removeAll {
            it is VideoModification.Filter
        }
        drawingPaintState.modifications.add(
            VideoModification.Filter(
                type = MediaColorFilters.entries[pagerState.currentPage]
            )
        )
    }

    FilterPager(
        bitmap = bitmap,
        pagerState = pagerState,
        modifier = modifier
    )
}

@Composable
fun ImageFilterPage(
    image: ImageBitmap,
    modifications: SnapshotStateList<SharedModification>,
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(pagerState.currentPage) {
        modifications.removeAll {
            it is ImageModification.Filter
        }
        modifications.add(
            ImageModification.Filter(
                type = MediaColorFilters.entries[pagerState.currentPage]
            )
        )
    }

    FilterPager(
        bitmap = image,
        pagerState = pagerState,
        modifier = modifier
    )
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun FilterPager(
    bitmap: ImageBitmap?,
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {
    HorizontalPager(
        state = pagerState,
        pageSpacing = 12.dp,
        flingBehavior = PagerDefaults.flingBehavior(
            state = pagerState,
            pagerSnapDistance = PagerSnapDistance.atMost(6)
        ),
        modifier = modifier
    ) { index ->
        FilterShowcase(
            image = bitmap,
            filter = MediaColorFilters.entries[index]
        )
    }
}