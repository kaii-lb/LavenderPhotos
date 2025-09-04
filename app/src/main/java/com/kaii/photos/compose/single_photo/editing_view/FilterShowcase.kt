package com.kaii.photos.compose.single_photo.editing_view

import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.helpers.editing.MediaColorFilters
import com.kaii.photos.helpers.editing.MediaColorFiltersImpl
import com.kaii.photos.helpers.editing.VideoModification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun FilterShowcase(
    image: ImageBitmap,
    filter: MediaColorFiltersImpl,
    extra: ColorMatrix,
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
        Image(
            bitmap = image,
            contentDescription = stringResource(id = filter.title),
            colorFilter = ColorFilter.colorMatrix(
                ColorMatrix().apply {
                    set(filter.matrix)
                    timesAssign(extra)
                }
            ),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
        )

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

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun FilterPager(
    pagerState: PagerState,
    modifications: SnapshotStateList<VideoModification>,
    currentVideoPosition: MutableFloatState,
    absolutePath: String,
    allowedToRefresh: Boolean,
    modifier: Modifier = Modifier
) {
    var bitmap by remember { mutableStateOf(ImageBitmap(512, 512)) }
    val coroutineScope = rememberCoroutineScope()
    var extra by remember { mutableStateOf(ColorMatrix()) }

    LaunchedEffect(currentVideoPosition.floatValue, allowedToRefresh) {
        if (allowedToRefresh) return@LaunchedEffect

        coroutineScope.launch(Dispatchers.IO) {
            val metadata = MediaMetadataRetriever()
            metadata.setDataSource(absolutePath)

            metadata.getFrameAtTime((currentVideoPosition.floatValue * 1000 * 1000).toLong())?.let {
                bitmap = it.asImageBitmap()
            }

            val adjustments = modifications.mapNotNull { it as? VideoModification.Adjustment }

            adjustments.forEach {
                extra *= ColorMatrix(it.type.getMatrix(it.value))
            }
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        modifications.removeAll {
            it is VideoModification.Filter
        }
        modifications.add(
            VideoModification.Filter(
                type = MediaColorFilters.entries[pagerState.currentPage]
            )
        )
    }

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
            filter = MediaColorFilters.entries[index],
            extra = extra
        )
    }
}