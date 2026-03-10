package com.kaii.photos.compose.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kaii.photos.helpers.toPascalCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.datetime.Month
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.time.Duration.Companion.milliseconds

@Preview
@Composable
private fun CircularSelectingListPreview() {
    CircularSelectingList(
        initialItemIndex = 0,
        items = { Month.entries.map { it.name.toPascalCase() } },
        modifier = Modifier
            .width(128.dp),
        setSelectedIndex = {}
    )
}

@OptIn(FlowPreview::class)
@Composable
fun CircularSelectingList(
    initialItemIndex: Int,
    items: () -> List<String>,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(
        initialFirstVisibleItemIndex = items().size * 100 / 2 - 2 + initialItemIndex,
        initialFirstVisibleItemScrollOffset =
            with(LocalDensity.current) { 24.dp.roundToPx() }
    ),
    setSelectedIndex: (index: Int) -> Unit
) {
    LaunchedEffect(listState, items()) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .debounce(200.milliseconds)
            .collectLatest { index ->
            setSelectedIndex((index + 2) % items().size)
        }
    }

    Box(
        modifier = modifier
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .height(48.dp * 4)
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            flingBehavior = rememberSnapFlingBehavior(
                lazyListState = listState
            )
        ) {
            items(
                count = items().size * 100
            ) { index ->
                val normalizedDistance by remember {
                    derivedStateOf {
                        val item = listState.layoutInfo.visibleItemsInfo.find { it.index == index }

                        if (item != null) {
                            val viewportCenter = listState.layoutInfo.viewportEndOffset / 2f
                            val itemCenter = item.offset + (item.size / 2f)

                            val distance = viewportCenter - itemCenter
                            val normalizedDistance = (distance / viewportCenter).coerceIn(-1f, 1f)
                            normalizedDistance
                        } else 1f
                    }
                }

                ListItem(
                    text = items()[index % items().size],
                    scale = { 1f - (normalizedDistance.absoluteValue * 0.3f) },
                    modifier = Modifier
                        .graphicsLayer {
                            val curve = normalizedDistance.absoluteValue.pow(2)
                            translationY = (normalizedDistance * (size.height / 2.25f)) * curve

                            val scale = 1f - (curve * 0.3f)
                            scaleX = scale
                            scaleY = scale
                            alpha = lerp(start = 0.6f, stop = 1f, fraction = 1f - curve)
                        }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .offset(y = 18.dp * 4)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.secondary)
                    .align(Alignment.TopCenter)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.secondary)
                    .align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun ListItem(
    text: String,
    scale: () -> Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight((500 * scale()).toInt())
        )
    }
}