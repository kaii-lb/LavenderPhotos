package com.kaii.photos.compose.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.kaii.photos.R
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.editing.DrawingColors
import com.kaii.photos.helpers.paging.PhotoLibraryUIModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// TODO: awful execution plz fix
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxWithConstraintsScope.ColorRangeSlider(
    sliderValue: MutableFloatState,
    enabled: Boolean = true,
    confirmValue: () -> Unit = {}
) {
    val localDensity = LocalDensity.current
    val max = maxWidth
    val clearAreaWidth = max / 11f - 4.dp
    val thumbRadius = 16.dp

    val resolvedColor by remember {
        derivedStateOf {
            DrawingColors.getColorFromLinearGradientList(
                value = (sliderValue.floatValue * 0.5f + 0.5f).coerceIn(0f, DrawingColors.gradientColorList.size - 1f),
                colorList = DrawingColors.gradientColorList
            )
        }
    }

    val sliderAnimatedValue by animateFloatAsState(
        targetValue = sliderValue.floatValue,
        animationSpec = tween(
            durationMillis = if (sliderValue.floatValue == -1.2f) 200 else 0
        ),
        label = "animate editing adjustment color tint value change"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth(1f)
            .height(18.dp)
            .padding(16.dp, 0.dp)
            .clip(CircleShape)
            .align(Alignment.Center)
            .shadow(
                elevation = 4.dp,
                shape = CircleShape
            )
            .alpha(if (enabled) 1f else 0.6f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .width(clearAreaWidth)
                .height(18.dp)
                .background(MaterialTheme.colorScheme.secondary)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.trash),
                contentDescription = "Clear current color tint",
                tint = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier
                    .size(clearAreaWidth - 4.dp)
                    .align(Alignment.Center)
            )
        }

        @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = DrawingColors.gradientColorList
                        ),
                        blendMode = BlendMode.SrcAtop
                    )
                }
        ) {
            val sliderLeft = sliderValue.floatValue * 0.5f + 0.5f
            val sliderRight = 1f - sliderLeft

            val trackWidth = maxWidth - thumbRadius
            val spaceBetween = 6.dp

            Box(
                modifier = Modifier
                    .width(trackWidth * sliderLeft - spaceBetween)
                    .height(18.dp)
                    .clip(RoundedCornerShape(0.dp, 2.dp, 2.dp, 0.dp))
                    .background(Color.White)
                    .align(Alignment.CenterStart)
            )

            Box(
                modifier = Modifier
                    .width(trackWidth * sliderRight - spaceBetween)
                    .height(18.dp)
                    .clip(
                        if (sliderValue.floatValue != -1.2f) {
                            RoundedCornerShape(2.dp, 0.dp, 0.dp, 2.dp)
                        } else {
                            RoundedCornerShape(0.dp)
                        }
                    )
                    .background(Color.White)
                    .align(Alignment.CenterEnd)
            )
        }

    }

    Row(
        modifier = Modifier
            .align(Alignment.Center)
            .padding(16.dp, 0.dp)
            .clipToBounds(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Slider(
            value = sliderAnimatedValue,
            valueRange = -1.2f..1f,
            onValueChange = {
                sliderValue.floatValue =
                    if (it <= -1.1f) {
                        -1.2f
                    } else {
                        it.coerceAtLeast(-1f)
                    }
            },
            onValueChangeFinished = confirmValue,
            track = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .height(18.dp)
                )
            },
            thumb = {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                with(localDensity) {
                                    if (sliderValue.floatValue == -1.2f) {
                                        (thumbRadius / 2)
                                            .toPx()
                                            .toInt()
                                    } else {
                                        0
                                    }
                                },
                                0
                            )
                        }
                        .size(thumbRadius)
                        .clip(CircleShape)
                        .background(resolvedColor)
                        .border(
                            width = 2.dp,
                            color = Color.White,
                            shape = CircleShape
                        )
                )
            },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxWithConstraintsScope.PopupPillSlider(
    sliderValue: MutableFloatState,
    changesSize: MutableIntState,
    popupPillHeightOffset: Dp = 0.dp,
    range: ClosedFloatingPointRange<Float> = -100f..100f,
    enabled: Boolean = true,
    confirmValue: () -> Unit = {},
    onValueChange: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    var isDraggingSlider by remember { mutableStateOf(false) }

    LaunchedEffect(interactionSource.interactions) {
        interactionSource.interactions.collect { interaction ->
            when {
                interaction is DragInteraction.Start || interaction is PressInteraction.Press -> {
                    isDraggingSlider = true
                    changesSize.intValue += 1
                }

                else -> {
                    isDraggingSlider = false
                }
            }
        }
    }

    val animatedPillHeight by animateDpAsState(
        targetValue = if (isDraggingSlider) 32.dp else 0.dp,
        animationSpec = tween(
            durationMillis = 150,
            delayMillis = if (isDraggingSlider) 0 else 100
        ),
        label = "Animate editing view bottom bar slider pill height"
    )

    val animatedPillHeightOffset by animateDpAsState(
        targetValue = if (isDraggingSlider) 0.dp else 32.dp,
        animationSpec = tween(
            durationMillis = 150,
            delayMillis = if (isDraggingSlider) 0 else 100
        ),
        label = "Animate editing view bottom bar slider pill height"
    )

    val animatedPillWidth by animateDpAsState(
        targetValue = if (isDraggingSlider) 48.dp else 4.dp,
        animationSpec = tween(
            durationMillis = 150,
            delayMillis = if (!isDraggingSlider) 0 else 100
        ),
        label = "Animate editing view bottom bar slider pill width"
    )

    val localDensity = LocalDensity.current
    val multiplier = ((sliderValue.floatValue * 100f) / (range.endInclusive - range.start)) - (range.start / 200)
    val multiplier2 = multiplier * 2 - 1

    val neededOffset = with(localDensity) {
        val position =
            multiplier * maxWidth.toPx() - (24.dp.toPx() * multiplier2) - (animatedPillWidth / 2).toPx() // offset by the opposite of the movement so the pill stays in the same place, then subtract half the width to center it
        position.coerceIn(16.dp.toPx(), (maxWidth - animatedPillWidth - 16.dp).toPx()) // -width - 16.dp because width of pill + padding
    }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    neededOffset.toInt(),
                    with(localDensity) {
                        ((-24).dp + animatedPillHeightOffset - popupPillHeightOffset)
                            .toPx()
                            .toInt()
                    })
            }
            .height(animatedPillHeight)
            .width(animatedPillWidth)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Text(
            text = (sliderValue.floatValue * 100).toInt().toString(),
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = TextUnit(14f, TextUnitType.Sp),
            modifier = Modifier
                .wrapContentSize()
                .align(Alignment.Center)
        )
    }

    Row(
        modifier = Modifier
            .align(Alignment.Center)
            .padding(16.dp, 0.dp)
            .clipToBounds(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Slider(
            value = sliderValue.floatValue * 100f,
            onValueChange = {
                onValueChange()
                sliderValue.floatValue = it / 100f
            },
            onValueChangeFinished = confirmValue,
            valueRange = range,
            steps = 199,
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = interactionSource,
                    enabled = enabled,
                    modifier = Modifier
                        .requiredHeight(28.dp)
                )
            },
            track = { state ->
                SliderDefaults.Track(
                    sliderState = state,
                    drawTick = { _, _ -> },
                    drawStopIndicator = {},
                    enabled = enabled
                )
            },
            interactionSource = interactionSource,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
        )
    }
}

@Composable
fun FloatingScrollbar(
    gridState: LazyGridState,
    spacerHeight: State<Dp>,
    pagingItems: LazyPagingItems<PhotoLibraryUIModel>,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    val totalItems by remember {
        derivedStateOf {
            gridState.layoutInfo.totalItemsCount
        }
    }

    val isScrolling by gridState.interactionSource.collectIsDraggedAsState()
    var isDraggingHandle by remember { mutableStateOf(false) }
    var targetIndex by remember { mutableIntStateOf(0) }

    var showHandle by remember { mutableStateOf(false) }
    LaunchedEffect(isScrolling, isDraggingHandle) {
        if (isScrolling || isDraggingHandle) {
            showHandle = true
        } else {
            delay(AnimationConstants.DURATION_EXTRA_LONG * 2L)
            showHandle = false
        }
    }

    var totalDrag by remember { mutableFloatStateOf(targetIndex.toFloat() / totalItems) }
    var maxHeight by remember { mutableIntStateOf(0) }
    val localDensity = LocalDensity.current

    val firstVisibleItemIndex by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex
        }
    }

    LaunchedEffect(firstVisibleItemIndex, totalItems) {
        if (!isDraggingHandle) {
            // update totalDrag for when the user scrolls the grid not the scrollbar
            // this prevents "jumping" of the scrollbar
            with(localDensity) {
                val scrollbarHeight = maxHeight - spacerHeight.value.toPx()
                totalDrag = (firstVisibleItemIndex.toFloat() / totalItems) * scrollbarHeight
            }
        }
    }

    AnimatedVisibility(
        visible = showHandle,
        enter = slideInHorizontally { width -> width },
        exit = slideOutHorizontally { width -> width }
    ) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxHeight(1f)
                .onGloballyPositioned {
                    maxHeight = it.size.height
                }
        ) {
            val thumbHeight = 48.dp
            val thumbPosition by remember(spacerHeight) {
                derivedStateOf {
                    (this@BoxWithConstraints.maxHeight - spacerHeight.value) * (gridState.firstVisibleItemIndex.toFloat() / totalItems)
                }
            }

            @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
            Box(
                modifier = Modifier
                    .fillMaxHeight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(
                            x = 8.dp,
                            y = thumbPosition
                        )
                        .height(thumbHeight)
                        .width(48.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = 1000.dp,
                                bottomStart = 1000.dp,
                                topEnd = 0.dp,
                                bottomEnd = 0.dp
                            )
                        )
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = { _ ->
                                    isDraggingHandle = true
                                },
                                onVerticalDrag = { _, dragAmount ->
                                    val scrollbarHeight = maxHeight - spacerHeight.value.toPx()

                                    totalDrag += dragAmount

                                    val newIndex = (totalDrag / scrollbarHeight) * totalItems

                                    if (!newIndex.isNaN()) {
                                        targetIndex = newIndex.roundToInt().coerceIn(0, pagingItems.itemCount - 1)

                                        coroutineScope.launch {
                                            gridState.scrollToItem(
                                                index = targetIndex,
                                                scrollOffset = 0
                                            )
                                        }
                                    }
                                },
                                onDragEnd = {
                                    isDraggingHandle = false
                                },
                                onDragCancel = {
                                    isDraggingHandle = false
                                }
                            )
                        }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.code),
                        contentDescription = "scrollbar handle",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .offset(x = (-2).dp)
                            .size(24.dp)
                            .rotate(90f)
                            .align(Alignment.Center)
                    )
                }

                val layoutDirection = LocalLayoutDirection.current
                Box(
                    modifier = Modifier
                        .offset(
                            x = 8.dp,
                            y = thumbPosition + 8.dp
                        )
                        .graphicsLayer {
                            translationX =
                                if (layoutDirection == LayoutDirection.Rtl) 220f
                                else -220f
                        }
                ) {
                    AnimatedVisibility(
                        visible = isDraggingHandle,
                        enter =
                            slideInHorizontally { width -> width / 4 } + fadeIn(),
                        exit =
                            slideOutHorizontally { width -> width / 4 } + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .height(32.dp)
                            .wrapContentWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .wrapContentWidth()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(8.dp, 4.dp)
                        ) {
                            // last index to "reach" even the last items
                            val item by remember {
                                derivedStateOf {
                                    pagingItems[targetIndex] ?: PhotoLibraryUIModel.Media(
                                        item = MediaStoreData.dummyItem,
                                        accessToken = null // TODO
                                    )
                                }
                            }

                            val format = remember { SimpleDateFormat("MMM yyyy", Locale.getDefault()) }
                            val formatted = remember(item) {
                                format.format(
                                    Date(
                                        (item as? PhotoLibraryUIModel.MediaImpl)?.item?.let { it.dateTaken * 1000 } ?: 0L
                                ))
                            }

                            Text(
                                text = formatted,
                                fontSize = TextUnit(14f, TextUnitType.Sp),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                            )
                        }
                    }
                }
            }
        }
    }

}