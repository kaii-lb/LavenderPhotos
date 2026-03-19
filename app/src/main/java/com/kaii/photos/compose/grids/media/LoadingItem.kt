package com.kaii.photos.compose.grids.media

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kaii.photos.compose.widgets.shimmerEffect
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.PhotoGridConstants
import com.kaii.photos.helpers.paging.PhotoLibraryUIModel
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun LoadingItem(
    item: PhotoLibraryUIModel,
    useRoundedCorners: Boolean
) {
    var showColors by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(10) // to avoid a pop-in effect
        showColors = true
    }

    val highlightColor by animateColorAsState(
        targetValue = if (showColors) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f) else Color.Transparent,
        animationSpec = tween(
            durationMillis = 0
        )
    )

    if (item is PhotoLibraryUIModel.Section) {
        Box(
            modifier = Modifier
                .fillMaxWidth(1f)
                .height(56.dp)
                .background(Color.Transparent)
                .padding(16.dp, 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(Random.nextFloat() * 0.5f + 0.5f)
                    .height(24.dp)
                    .clip(CircleShape)
                    .shimmerEffect(
                        containerColor = Color.Transparent,
                        highlightColor = highlightColor,
                        durationMillis = AnimationConstants.DURATION_EXTRA_LONG * 3,
                        delayMillis = -PhotoGridConstants.UPDATE_TIME.toInt() * 2
                    )
            )
        }
    } else {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .padding(2.dp)
                .clip(RoundedCornerShape(if (useRoundedCorners) 8.dp else 0.dp))
                .shimmerEffect(
                    containerColor = Color.Transparent,
                    highlightColor = highlightColor,
                    durationMillis = AnimationConstants.DURATION_EXTRA_LONG * 2
                )
        )
    }
}