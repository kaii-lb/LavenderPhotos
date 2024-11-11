package com.kaii.photos.compose.single_photo

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import com.kaii.photos.R
import com.kaii.photos.compose.BottomAppBarItem
import com.kaii.photos.compose.ConfirmationDialog
import com.kaii.photos.compose.SetEditingViewDrawableTextBottomSheet
import com.kaii.photos.helpers.ColorIndicator
import com.kaii.photos.helpers.CustomMaterialTheme
import com.kaii.photos.helpers.DrawableItem
import com.kaii.photos.helpers.DrawablePath
import com.kaii.photos.helpers.DrawableText
import com.kaii.photos.helpers.DrawingColors
import com.kaii.photos.helpers.DrawingPaints
import com.kaii.photos.helpers.ExtendedPaint
import com.kaii.photos.helpers.PaintType
import com.kaii.photos.helpers.savePathListToBitmap
import com.kaii.photos.helpers.toOffset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

@Composable
fun EditingView(
    navController: NavHostController,
    absolutePath: String,
    uri: Uri
) {
    val showCloseDialog = remember { mutableStateOf(false) }
    val showBackClickCloseDialog = remember { mutableStateOf(false) }
    val changesSize = remember { mutableIntStateOf(0) }
    val oldChangesSize = remember { mutableIntStateOf(changesSize.intValue) }

    ConfirmationDialog(
        showDialog = showCloseDialog,
        dialogTitle = "Discard ongoing edits?",
        confirmButtonLabel = "Discard"
    ) {
        navController.popBackStack()
    }

    ConfirmationDialog(
        showDialog = showBackClickCloseDialog,
        dialogTitle = "Exit editing view?",
        confirmButtonLabel = "Exit"
    ) {
        navController.popBackStack()
    }

    BackHandler {
        if (changesSize.intValue != oldChangesSize.intValue) {
            showCloseDialog.value = true
        } else {
            oldChangesSize.intValue = changesSize.intValue
            showBackClickCloseDialog.value = true
        }
    }

    val pagerState = rememberPagerState { 4 }

    val modifications = remember { mutableStateListOf<DrawableItem>() }
    val paint = remember { mutableStateOf(DrawingPaints.Pencil) }

    val context = LocalContext.current
    val inputStream = remember { context.contentResolver.openInputStream(uri) }

    val originalImage = remember { BitmapFactory.decodeStream(inputStream) }
    var image by remember { mutableStateOf(originalImage.asImageBitmap()) }
    inputStream?.close()

    val rotationMultiplier = remember { mutableIntStateOf(0) }
    val rotation by animateFloatAsState(
        targetValue = -90f * rotationMultiplier.intValue,
        label = "Animate rotation"
    )

    var maxSize by remember { mutableStateOf(Size.Unspecified) }
    val coroutineScope = rememberCoroutineScope()

    val textMeasurer = rememberTextMeasurer()
    val localDensity = LocalDensity.current

    Scaffold(
        topBar = {
            EditingViewTopBar(
                showCloseDialog = showCloseDialog,
                changesSize = changesSize,
                oldChangesSize = oldChangesSize,
                saveImage = {
                    coroutineScope.launch {
                        savePathListToBitmap(
                            modifications = modifications,
                            absolutePath = absolutePath,
                            image = image,
                            maxSize = maxSize,
                            rotation = rotation,
                            textMeasurer = textMeasurer
                        )
                    }
                },
                popBackStack = {
                    navController.popBackStack()
                }
            )
        },
        bottomBar = {
            EditingViewBottomBar(
                pagerState = pagerState,
                paint = paint,
                rotationMultiplier = rotationMultiplier,
                changesSize = changesSize,
                resetCropping = {
                    rotationMultiplier.intValue = 0
                    image = originalImage.asImageBitmap()
                }
            )
        },
        modifier = Modifier
            .fillMaxSize(1f)
    ) { innerPadding ->
        var lastSize by remember { mutableIntStateOf(modifications.size) }
        val showEditTextBottomSheet = remember { mutableStateOf(false) }
        LaunchedEffect(modifications.size) {
            val last = modifications.lastOrNull()

            if (last is DrawableText && modifications.size > lastSize) {
                showEditTextBottomSheet.value = true
            }

            lastSize = modifications.size
        }

        SetEditingViewDrawableTextBottomSheet(
            showBottomSheet = showEditTextBottomSheet,
            modifications = modifications,
            textMeasurer
        )

        val isLandscape =
            LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize(1f)
                .padding(if (isLandscape) PaddingValues(0.dp) else innerPadding)
        ) {
            val isDrawing = remember { mutableStateOf(false) }

            var initialLoad by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(500)
                initialLoad = true
            }

            val size by remember {
                derivedStateOf {
                    with(localDensity) {
                        val xRatio = maxWidth.toPx() / image.width
                        val yRatio = maxHeight.toPx() / image.height
                        val ratio = min(xRatio, yRatio)

                        val width = image.width * ratio
                        val height = image.height * ratio
                        maxSize = Size(width, height)

                        IntSize(width.toInt(), height.toInt())
                    }
                }
            }

            val dpSize by remember {
                derivedStateOf {
                    with(localDensity) {
                        val width = size.width.toDp()
                        val height = size.height.toDp()

                        DpSize(width, height)
                    }
                }
            }

            val isVertical by remember { derivedStateOf { rotation % 180f == 0f }}
            val isHorizontal by remember { derivedStateOf { abs(rotation % 180f) == 90f }}

            var lastScale by remember { mutableFloatStateOf(1f) }
            val maxScale by remember {
            	derivedStateOf {
	                with(localDensity) {
	                    if (isVertical) {
	                        lastScale =
	                            if (isLandscape) {
	                                maxHeight.toPx() / size.height
	                            } else {
	                                min(maxWidth.toPx() / size.width, maxHeight.toPx() / size.height)
	                            }
	                        lastScale
	                    } else if (isHorizontal) {
	                        lastScale =
	                            if (isLandscape) {
	                                maxHeight.toPx() / size.width
	                            } else {
	                                min(maxWidth.toPx() / size.height, maxHeight.toPx() / size.height)
	                            }
	                        lastScale
	                    } else lastScale
	                }
            	}
            }
            val minScale by remember { derivedStateOf { 0.8f * maxScale }}

            val animatedSize by animateFloatAsState(
                targetValue = if (pagerState.currentPage == 0 && initialLoad) minScale else maxScale,
                label = "Animate size of preview image in crop mode"
            )

            val defaultTextStyle = DrawableText.Styles.Default.style
            val canDraw = remember {
                derivedStateOf {
                    pagerState.currentPage == 3
                }
            }

            Canvas(
                modifier = Modifier
                    .size(dpSize)
                    .align(Alignment.Center)
                    .graphicsLayer {
                        scaleX = animatedSize
                        scaleY = animatedSize
                        rotationZ = rotation
                    }
                    .clipToBounds()
                    .makeDrawCanvas(
                        allowedToDraw = canDraw,
                        modifications = modifications,
                        paint = paint,
                        isDrawing = isDrawing,
                        changesSize = changesSize,
                        rotationMultiplier = rotationMultiplier
                    )
            ) {
                drawImage(
                    image = image,
                    dstSize = size
                )

                modifications.forEach { modification ->
                    if (modification is DrawablePath) {
                        val (path, pathPaint) = modification

                        drawPath(
                            path = path,
                            style = Stroke(
                                width = pathPaint.strokeWidth,
                                cap = pathPaint.strokeCap,
                                join = pathPaint.strokeJoin,
                                miter = pathPaint.strokeMiterLimit,
                                pathEffect = pathPaint.pathEffect
                            ),
                            blendMode = pathPaint.blendMode,
                            color = pathPaint.color,
                            alpha = pathPaint.alpha
                        )
                    } else if (modification is DrawableText) {
                        val (text, textPosition, textPaint, textRotation, textSize) = modification

                        val textLayout = textMeasurer.measure(
                            text = text,
                            style = TextStyle(
                                color = textPaint.color,
                                fontSize = TextUnit(textPaint.strokeWidth, TextUnitType.Sp),
                                textAlign = defaultTextStyle.textAlign,
                                platformStyle = defaultTextStyle.platformStyle,
                                lineHeightStyle = defaultTextStyle.lineHeightStyle,
                                baselineShift = defaultTextStyle.baselineShift
                            )
                        )

                        rotate(textRotation, textPosition + textSize.toOffset() / 2f) {
                            translate(textPosition.x, textPosition.y) {
                                drawText(
                                    textLayoutResult = textLayout,
                                    color = textPaint.color,
                                    alpha = textPaint.alpha,
                                    blendMode = textPaint.blendMode
                                )
                            }
                        }
                    }
                }
            }

            var topLeftOffset by remember {
                mutableStateOf(
                    IntOffset(
                        0,
                        0
                    )
                )
            }
            var topRightOffset by remember {
                mutableStateOf(
                    IntOffset(
                        size.width,
                        0
                    )
                )
            }
            var bottomLeftOffset by remember {
                mutableStateOf(
                    IntOffset(
                        0,
                        size.height
                    )
                )
            }
            var bottomRightOffset by remember {
                mutableStateOf(
                    IntOffset(
                        size.width,
                        size.height
                    )
                )
            }

            LaunchedEffect(image) {
	            topLeftOffset = IntOffset.Zero
	            topRightOffset = IntOffset(size.width, 0)
	            bottomLeftOffset = IntOffset(0, size.height)
	            bottomRightOffset = IntOffset(size.width, size.height)
            }

            AnimatedVisibility(
                visible = topRightOffset.x - topLeftOffset.x != size.width || bottomLeftOffset.y - topLeftOffset.y != size.height,
                enter = slideInVertically { height -> height } + fadeIn(),
                exit = slideOutVertically { height -> height } + fadeOut(),
                modifier = Modifier
                    .width(80.dp)
                    .align(Alignment.BottomCenter)
                    .padding(0.dp, 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .height(40.dp)
                        .width(80.dp)
                        .clip(CircleShape)
                        .background(
                            CustomMaterialTheme.colorScheme.surfaceVariant
                        )
                        .align(Alignment.BottomCenter)
                        .clickable {
                            val ratio = 1f / min(
                                size.width.toFloat() / image.width.toFloat(),
                                size.height.toFloat() / image.height.toFloat()
                            )

                            val x = (topLeftOffset.x * ratio).toInt()
                            val y = (topLeftOffset.y * ratio).toInt()
                            val width = ((topRightOffset.x - topLeftOffset.x) * ratio).toInt().coerceIn(0, image.width) - 2 // 2 magic number?
                            val height = ((bottomLeftOffset.y - topLeftOffset.y) * ratio).toInt().coerceIn(0, image.height) - 2 // 2 magic number?

							println("POS $x $y $width $height ${image.width} ${image.height}")

							if (x + width <= image.width && y + height <= image.height && width > 0 && height > 0) {
	                            image = Bitmap
	                                .createBitmap(
	                                    image.asAndroidBitmap(),
	                                    x,
	                                    y,
	                                    width,
	                                    height
	                                )
	                                .asImageBitmap()

		                            topLeftOffset = IntOffset.Zero
		                            topRightOffset = IntOffset(size.width, 0)
		                            bottomLeftOffset = IntOffset(0, size.height)
		                            bottomRightOffset = IntOffset(size.width, size.height)

		                            changesSize.intValue += 1
							} else {
								// TODO: show can't crop snackbar
							}
                        },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Confirm",
                        fontSize = TextUnit(14f, TextUnitType.Sp),
                        color = CustomMaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .wrapContentSize()
                    )
                }
            }

            Box(
                modifier = Modifier
                	.align(Alignment.Center)
                    .size(
                        dpSize.width,
                        dpSize.height
                    )
                    .graphicsLayer {
                        scaleX = animatedSize
                        scaleY = animatedSize
                        rotationZ = rotation
                    }
                    .drawWithCache {
                        onDrawWithContent {
                            clipRect(
                                left = topLeftOffset.x.toFloat(),
                                top = topLeftOffset.y.toFloat(),
                                right = topRightOffset.x.toFloat(),
                                bottom = bottomRightOffset.y.toFloat(),
                                clipOp = ClipOp.Difference
                            ) {
                                drawRect(
                                    color = DrawingColors.Black,
                                    alpha = 0.5f,
                                    blendMode = BlendMode.SrcOver,
                                    topLeft = Offset.Zero,
                                    size = size.toSize()
                                )
                            }

                            drawLine(
                                color = DrawingColors.White,
                                start = topLeftOffset.toOffset(),
                                end = topRightOffset.toOffset(),
                                strokeWidth = 4.dp.toPx(),
                                blendMode = BlendMode.SrcOver,
                            )

                            drawLine(
                                color = DrawingColors.White,
                                start = topLeftOffset.toOffset(),
                                end = bottomLeftOffset.toOffset(),
                                strokeWidth = 4.dp.toPx(),
                                blendMode = BlendMode.SrcOver,
                            )

                            drawLine(
                                color = DrawingColors.White,
                                start = topRightOffset.toOffset(),
                                end = bottomRightOffset.toOffset(),
                                strokeWidth = 4.dp.toPx(),
                                blendMode = BlendMode.SrcOver,
                            )

                            drawLine(
                                color = DrawingColors.White,
                                start = bottomLeftOffset.toOffset(),
                                end = bottomRightOffset.toOffset(),
                                strokeWidth = 4.dp.toPx(),
                                blendMode = BlendMode.SrcOver,
                            )

                            drawContent()
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            val isInCropRect = checkIfInRect(
                                change.position,
                                with(localDensity) { 24.dp.toPx() },
                                bottomLeftOffset.x,
                                topLeftOffset.y,
                                topRightOffset.x,
                                bottomRightOffset.y
                            )

                            if (isInCropRect) {
                                val width = topRightOffset.x - topLeftOffset.x
                                val height = bottomRightOffset.y - topRightOffset.y

                                topLeftOffset = (topLeftOffset + dragAmount)
                                    .coerceIn(
                                        minX = 0,
                                        minY = 0,
                                        maxX = size.width - width,
                                        maxY = size.height - height
                                    )

                                topRightOffset =
                                    IntOffset(topLeftOffset.x + width, topLeftOffset.y)

                                bottomLeftOffset =
                                    IntOffset(topLeftOffset.x, topLeftOffset.y + height)

                                bottomRightOffset =
                                    IntOffset(topLeftOffset.x + width, topLeftOffset.y + height)
                            }
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .offset {
                            topLeftOffset - IntOffset(
                                12.dp
                                    .toPx()
                                    .toInt(),
                                12.dp
                                    .toPx()
                                    .toInt()
                            )
                        }
                        .size(24.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()

                                val topLeftOffsetPos = (topLeftOffset + dragAmount)
                                    .coerceIn(
                                        minX = 0,
                                        minY = 0,
                                        maxX = (topRightOffset.x - 56.dp.toPx().toInt()).coerceAtLeast(0),
                                        maxY = (bottomLeftOffset.y - 56.dp.toPx().toInt()).coerceAtLeast(0)
                                    )

								//if (freeform crop)
								topLeftOffset = topLeftOffsetPos
                                topRightOffset =
                                    IntOffset(
                                        topRightOffset.x,
                                        topLeftOffset.y
                                    )

                                bottomLeftOffset =
                                    IntOffset(
                                        topLeftOffset.x,
                                        bottomLeftOffset.y
                                    )

								// if not freeform
								// check where to multiply by ratio
// 								val width = topRightOffset.x - topLeftOffsetPos.x
// 								val height = bottomRightOffset.y - topLeftOffsetPos.y
// 								val needed = min(width, height)
// 
// 								topLeftOffset =
// 									IntOffset(
// 										topRightOffset.x - needed,
// 										bottomRightOffset.y - needed
// 									)
// 
//                                 bottomLeftOffset =
//                                     IntOffset(
//                                         topLeftOffset.x,
//                                         bottomLeftOffset.y
//                                     )
// 
//                                 topRightOffset =
//                                     IntOffset(
//                                         topRightOffset.x,
//                                         topLeftOffset.y
//                                     )
                            }
                        }
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(DrawingColors.White)
                )

                Box(
                    modifier = Modifier
                        .offset {
                            topRightOffset - IntOffset(
                                12.dp
                                    .toPx()
                                    .toInt(),
                                12.dp
                                    .toPx()
                                    .toInt()
                            )
                        }
                        .size(24.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()

                                topRightOffset = (topRightOffset + dragAmount)
                                    .coerceIn(
                                        minX = (topLeftOffset.x + 56.dp.toPx().toInt()).coerceAtMost(size.width),
                                        minY = 0,
                                        maxX = size.width,
                                        maxY = (bottomRightOffset.y - 56.dp.toPx().toInt()).coerceAtLeast(0)
                                    )

                                bottomRightOffset =
                                    IntOffset(
                                        topRightOffset.x,
                                        bottomRightOffset.y
                                    )

                                topLeftOffset =
                                    IntOffset(
                                        topLeftOffset.x,
                                        topRightOffset.y
                                    )
                            }
                        }
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(DrawingColors.White)
                )

                Box(
                    modifier = Modifier
                        .offset {
                            bottomLeftOffset - IntOffset(
                                12.dp
                                    .toPx()
                                    .toInt(),
                                12.dp
                                    .toPx()
                                    .toInt()
                            )
                        }
                        .size(24.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()

                                bottomLeftOffset = (bottomLeftOffset + dragAmount)
                                    .coerceIn(
                                        minX = 0,
                                        minY = (topLeftOffset.y + 56.dp.toPx().toInt()).coerceAtMost(size.height),
                                        maxX = (bottomRightOffset.x - 56.dp.toPx().toInt()).coerceAtLeast(0),
                                        maxY = size.height
                                    )

                                topLeftOffset =
                                    IntOffset(
                                        bottomLeftOffset.x,
                                        topLeftOffset.y
                                    )

                                bottomRightOffset =
                                    IntOffset(
                                        bottomRightOffset.x,
                                        bottomLeftOffset.y
                                    )
                            }
                        }
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(DrawingColors.White)
                )

                Box(
                    modifier = Modifier
                        .offset {
                            bottomRightOffset - IntOffset(
                                12.dp
                                    .toPx()
                                    .toInt(),
                                12.dp
                                    .toPx()
                                    .toInt()
                            )
                        }
                        .size(24.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()

                                bottomRightOffset = (bottomRightOffset + dragAmount)
                                    .coerceIn(
                                        minX = (bottomLeftOffset.x + 56.dp.toPx().toInt()).coerceAtMost(size.width),
                                        minY = (topRightOffset.y + 56.dp.toPx().toInt()).coerceAtMost(size.height),
                                        maxX = size.width,
                                        maxY = size.height
                                    )

                                topRightOffset =
                                    IntOffset(
                                        bottomRightOffset.x,
                                        topRightOffset.y
                                    )

                                bottomLeftOffset =
                                    IntOffset(
                                        bottomLeftOffset.x,
                                        bottomRightOffset.y
                                    )
                            }
                        }
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(DrawingColors.White)
                )
            }

            DrawingControls(
                pagerState = pagerState,
                modifications = modifications,
                paint = paint,
                changesSize = changesSize,
                isDrawing = isDrawing
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditingViewTopBar(
    showCloseDialog: MutableState<Boolean>,
    changesSize: MutableIntState,
    oldChangesSize: MutableIntState,
    saveImage: () -> Unit,
    popBackStack: () -> Unit
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var showInLandscape by remember { mutableStateOf(false) }

    val animatedHeight by animateDpAsState(
        targetValue = if (isLandscape && !showInLandscape) (-64).dp else 0.dp,
        animationSpec = tween(
            durationMillis = if (isLandscape) 350 else 0
        ),
        label = "Animate editing view top bar height"
    )

    val animatedRotation by animateFloatAsState(
        targetValue = if (showInLandscape) -90f else 90f,
        animationSpec = tween(
            durationMillis = 350
        ),
        label = "Animate editing view top bar icon rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth(1f)
            .offset {
                IntOffset(
                    0,
                    animatedHeight
                        .toPx()
                        .toInt()
                )
            },
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            navigationIcon = {
                Box(
                    modifier = Modifier
                        .padding(8.dp, 0.dp, 0.dp, 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .height(40.dp)
                            .width(56.dp)
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(CustomMaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                if (changesSize.intValue != oldChangesSize.intValue) {
                                    showCloseDialog.value = true
                                } else {
                                    oldChangesSize.intValue = changesSize.intValue
                                    popBackStack()
                                }
                            },
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.close),
                            contentDescription = "Close editing view",
                            tint = CustomMaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }
                }
            },
            actions = {
                Box(
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 8.dp, 0.dp)
                ) {
                    Button(
                        onClick = {
                            saveImage()
                            oldChangesSize.intValue = changesSize.intValue
                        },
                        shape = CircleShape,
                        enabled = changesSize.intValue != oldChangesSize.intValue
                    ) {
                        Text(
                            text = "Save Copy",
                            fontSize = TextUnit(14f, TextUnitType.Sp),
                            color = CustomMaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            },
            title = {}
        )

        if (isLandscape) {
            val statusBarPadding = WindowInsets.safeContent.asPaddingValues()
                .calculateStartPadding(LocalLayoutDirection.current)

            Box(
                modifier = Modifier
                    .align(Alignment.Start)
                    .offset {
                        IntOffset(
                            statusBarPadding
                                .toPx()
                                .toInt(),
                            0
                        )
                    }
                    .height(28.dp)
                    .width(32.dp)
                    .clip(RoundedCornerShape(0.dp, 0.dp, 1000.dp, 1000.dp))
                    .background(CustomMaterialTheme.colorScheme.surfaceContainer)
                    .clickable {
                        showInLandscape = !showInLandscape
                    }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.other_page_indicator),
                    tint = CustomMaterialTheme.colorScheme.onSurface,
                    contentDescription = "Show editing top bar",
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 0.dp, 4.dp)
                        .rotate(animatedRotation)
                        .align(Alignment.Center)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditingViewBottomBar(
    pagerState: PagerState,
    paint: MutableState<ExtendedPaint>,
    rotationMultiplier: MutableIntState,
    changesSize: MutableIntState,
    resetCropping: () -> Unit
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var showInLandscape by remember { mutableStateOf(false) }

    val animatedHeight by animateDpAsState(
        targetValue = if (isLandscape && !showInLandscape) 120.dp else 0.dp,
        animationSpec = tween(
            durationMillis = if (isLandscape) 350 else 0
        ),
        label = "Animate editing view bottom bar height"
    )

    val animatedRotation by animateFloatAsState(
        targetValue = if (showInLandscape) 90f else -90f,
        animationSpec = tween(
            durationMillis = 350
        ),
        label = "Animate editing view bottom bar height"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth(1f)
            .offset {
                IntOffset(
                    0,
                    animatedHeight
                        .toPx()
                        .toInt()
                )
            },
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLandscape) {
            val statusBarPadding = WindowInsets.safeContent.asPaddingValues()
                .calculateStartPadding(LocalLayoutDirection.current)

            Box(
                modifier = Modifier
                    .align(Alignment.Start)
                    .offset {
                        IntOffset(
                            statusBarPadding
                                .toPx()
                                .toInt(),
                            0
                        )
                    }
                    .height(28.dp)
                    .width(32.dp)
                    .clip(RoundedCornerShape(1000.dp, 1000.dp, 0.dp, 0.dp))
                    .background(CustomMaterialTheme.colorScheme.surfaceContainer)
                    .clickable {
                        showInLandscape = !showInLandscape
                    }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.other_page_indicator),
                    tint = CustomMaterialTheme.colorScheme.onSurface,
                    contentDescription = "Show editing tools",
                    modifier = Modifier
                        .padding(0.dp, 4.dp, 0.dp, 0.dp)
                        .rotate(animatedRotation)
                        .align(Alignment.Center)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth(1f)
                .height(if (isLandscape) 120.dp else 160.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BottomAppBar(
                containerColor = CustomMaterialTheme.colorScheme.surfaceContainer,
                contentColor = CustomMaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .height(160.dp)
            ) {
                val coroutineScope = rememberCoroutineScope()

                Column(
                    modifier = Modifier
                        .fillMaxSize(1f),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CompositionLocalProvider(LocalRippleConfiguration provides NoRippleConfiguration) {
                        TabRow(
                            selectedTabIndex = pagerState.currentPage,
                            divider = {},
                            indicator = { tabPosition ->
                                Box(
                                    modifier = Modifier
                                        .tabIndicatorOffset(tabPosition[pagerState.currentPage])
                                        .padding(4.dp)
                                        .fillMaxHeight(1f)
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(CustomMaterialTheme.colorScheme.primary)
                                        .zIndex(1f)
                                )
                            },
                            containerColor = CustomMaterialTheme.colorScheme.surfaceContainer,
                            contentColor = CustomMaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .clip(RoundedCornerShape(1000.dp))
                                .draggable(
                                    state = rememberDraggableState { delta ->
                                        if (delta < 0) {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(
                                                    (pagerState.currentPage + 1).coerceAtMost(
                                                        3
                                                    )
                                                )
                                            }
                                        } else {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(
                                                    (pagerState.currentPage - 1).coerceAtLeast(
                                                        0
                                                    )
                                                )
                                            }
                                        }
                                    },
                                    orientation = Orientation.Horizontal
                                )
                        ) {
                            Tab(
                                selected = pagerState.currentPage == 0,
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(0)
                                    }
                                },
                                modifier = Modifier
                                    .height(40.dp)
                                    .zIndex(2f)
                                    .clip(RoundedCornerShape(100.dp))
                            ) {
                                Text(
                                    text = "Crop",
                                    color = if (pagerState.currentPage == 0) CustomMaterialTheme.colorScheme.onPrimary else CustomMaterialTheme.colorScheme.onSurface,
                                    fontSize = TextUnit(14f, TextUnitType.Sp)
                                )
                            }

                            Tab(
                                selected = pagerState.currentPage == 1,
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(1)
                                    }
                                },
                                modifier = Modifier
                                    .height(40.dp)
                                    .zIndex(2f)
                                    .clip(RoundedCornerShape(100.dp))
                            ) {
                                Text(
                                    text = "Adjust",
                                    color = if (pagerState.currentPage == 1) CustomMaterialTheme.colorScheme.onPrimary else CustomMaterialTheme.colorScheme.onSurface,
                                    fontSize = TextUnit(14f, TextUnitType.Sp)
                                )
                            }

                            Tab(
                                selected = pagerState.currentPage == 2,
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(2)
                                    }
                                },
                                modifier = Modifier
                                    .height(40.dp)
                                    .zIndex(2f)
                                    .clip(RoundedCornerShape(100.dp))
                            ) {
                                Text(
                                    text = "Filters",
                                    color = if (pagerState.currentPage == 2) CustomMaterialTheme.colorScheme.onPrimary else CustomMaterialTheme.colorScheme.onSurface,
                                    fontSize = TextUnit(14f, TextUnitType.Sp)
                                )
                            }

                            Tab(
                                selected = pagerState.currentPage == 3,
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(3)
                                    }
                                },
                                modifier = Modifier
                                    .height(40.dp)
                                    .zIndex(2f)
                                    .clip(RoundedCornerShape(100.dp))
                            ) {
                                Text(
                                    text = "Draw",
                                    color = if (pagerState.currentPage == 3) CustomMaterialTheme.colorScheme.onPrimary else CustomMaterialTheme.colorScheme.onSurface,
                                    fontSize = TextUnit(14f, TextUnitType.Sp)
                                )
                            }
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = false,
                        snapPosition = SnapPosition.Center,
                        pageSize = PageSize.Fill,
                    ) { index ->
                        when (index) {
                            0 -> {
                                CropTools(
                                    rotationMultiplier = rotationMultiplier,
                                    changesSize = changesSize,
                                    resetCropping = resetCropping
                                )
                            }

                            1 -> {
                                AdjustTools()
                            }

                            2 -> {
                                FiltersTools()
                            }

                            3 -> {
                                DrawTools(paint)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CropTools(
    rotationMultiplier: MutableIntState,
    changesSize: MutableIntState,
    resetCropping: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        EditingViewBottomAppBarItem(text = "Rotate", iconResId = R.drawable.rotate_ccw) {
            rotationMultiplier.intValue += 1
            changesSize.intValue += 1
        }

        EditingViewBottomAppBarItem(text = "Ratio", iconResId = R.drawable.resolution) {

        }

        EditingViewBottomAppBarItem(text = "Reset", iconResId = R.drawable.reset) {
            resetCropping()
            changesSize.intValue += 1
        }
    }
}

@Composable
fun AdjustTools() {
    LazyRow(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxSize(1f)
    ) {
        item {
            EditingViewBottomAppBarItem(text = "Contrast", iconResId = R.drawable.contrast)
        }

        item {
            EditingViewBottomAppBarItem(text = "Brightness", iconResId = R.drawable.palette)
        }

        item {
            EditingViewBottomAppBarItem(text = "Saturation", iconResId = R.drawable.resolution)
        }

        item {
            EditingViewBottomAppBarItem(
                text = "Black Point",
                iconResId = R.drawable.file_is_selected_background
            )
        }

        item {
            EditingViewBottomAppBarItem(
                text = "White Point",
                iconResId = R.drawable.file_not_selected_background
            )
        }

        item {
            EditingViewBottomAppBarItem(text = "Shadows", iconResId = R.drawable.shadow)
        }

        item {
            EditingViewBottomAppBarItem(text = "Warmth", iconResId = R.drawable.skillet)
        }

        item {
            EditingViewBottomAppBarItem(text = "Tint", iconResId = R.drawable.colors)
        }
    }
}

@Composable
fun FiltersTools() {
    LazyRow(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxSize(1f)
    ) {
        item {
            EditingViewBottomAppBarItem(text = "Filter", iconResId = R.drawable.style)
        }

        item {
            EditingViewBottomAppBarItem(text = "Filter", iconResId = R.drawable.style)
        }

        item {
            EditingViewBottomAppBarItem(text = "Filter", iconResId = R.drawable.style)
        }

        item {
            EditingViewBottomAppBarItem(text = "Filter", iconResId = R.drawable.style)
        }

        item {
            EditingViewBottomAppBarItem(text = "Filter", iconResId = R.drawable.style)
        }
    }
}

@Composable
fun DrawTools(
    paint: MutableState<ExtendedPaint>
) {
    Row(
        modifier = Modifier
            .fillMaxSize(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        EditingViewBottomAppBarItem(
            text = "Pencil",
            iconResId = R.drawable.pencil,
            selected = paint.value.type == PaintType.Pencil
        ) {
            paint.value = DrawingPaints.Pencil.copy(
                strokeWidth = paint.value.strokeWidth,
                color = paint.value.color
            )
        }

        EditingViewBottomAppBarItem(
            text = "Highlighter",
            iconResId = R.drawable.highlighter,
            selected = paint.value.type == PaintType.Highlighter
        ) {
            paint.value = DrawingPaints.Highlighter.copy(
                strokeWidth = paint.value.strokeWidth,
                color = paint.value.color
            )
        }

        EditingViewBottomAppBarItem(
            text = "Text",
            iconResId = R.drawable.text,
            selected = paint.value.type == PaintType.Text
        ) {
            paint.value = DrawingPaints.Text.copy(
                strokeWidth = paint.value.strokeWidth,
                color = paint.value.color
            )
        }
    }
}

@Composable
fun EditingViewBottomAppBarItem(
    text: String,
    iconResId: Int,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    BottomAppBarItem(
        text = text,
        iconResId = iconResId,
        buttonWidth = 84.dp,
        buttonHeight = 56.dp,
        cornerRadius = 8.dp,
        color = if (selected) CustomMaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor = if (selected) CustomMaterialTheme.colorScheme.onPrimary else CustomMaterialTheme.colorScheme.onBackground,
        action = onClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
val NoRippleConfiguration = RippleConfiguration(
    color = Color.Transparent,
    rippleAlpha = RippleAlpha(0f, 0f, 0f, 0f)
)

/** @param allowedToDraw no drawing happens if this is false
 * @param modifications a list of [DrawableItem] which is all the new [DrawablePath]s or [DrawableText]s drawn
 * @param paint the paint to draw with
 * @param isDrawing is the user drawing right now? */
@Composable
private fun Modifier.makeDrawCanvas(
    allowedToDraw: State<Boolean>,
    modifications: SnapshotStateList<DrawableItem>,
    paint: MutableState<ExtendedPaint>,
    isDrawing: MutableState<Boolean>,
    changesSize: MutableIntState,
    rotationMultiplier: MutableIntState
): Modifier {
    val textMeasurer = rememberTextMeasurer()
    val localTextStyle = LocalTextStyle.current
    val defaultTextStyle = DrawableText.Styles.Default.style

    val modifier = Modifier
        .pointerInput(Unit) {
            if (allowedToDraw.value) {
                awaitEachGesture {
                    var lastPoint = Offset.Unspecified
                    var lastText: DrawableText? = null
                    var touchOffset = Offset.Zero

                    do {
                        val event = awaitPointerEvent()
                        val canceled = event.changes.any { it.isConsumed } || !allowedToDraw.value

                        if (!canceled && (paint.value.type == PaintType.Pencil || paint.value.type == PaintType.Highlighter)) {
                            when (event.type) {
                                PointerEventType.Press -> {
                                    val offset = event.changes.first().position

                                    val path = Path().apply {
                                        moveTo(offset.x, offset.y)
                                    }

                                    modifications.add(
                                        DrawablePath(
                                            path,
                                            paint.value
                                        )
                                    )

                                    lastPoint = offset
                                    isDrawing.value = true
                                    changesSize.intValue += 1

                                    event.changes.forEach {
                                        it.consume()
                                    }
                                }

                                PointerEventType.Move -> {
                                    val offset = event.changes.first().position
                                    var path =
                                        (modifications.findLast {
                                            it is DrawablePath
                                        } as DrawablePath?)?.path

                                    if (path == null) {
                                        val newPath =
                                            DrawablePath(
                                                Path().apply {
                                                    moveTo(offset.x, offset.y)
                                                },
                                                paint.value
                                            )
                                        modifications.add(newPath)
                                        path = newPath.path
                                    } else {
                                        modifications.remove(
                                            DrawablePath(
                                                path,
                                                paint.value
                                            )
                                        )
                                    }

                                    path.quadraticTo(
                                        lastPoint.x,
                                        lastPoint.y,
                                        (lastPoint.x + offset.x) / 2,
                                        (lastPoint.y + offset.y) / 2
                                    )

                                    modifications.add(
                                        DrawablePath(
                                            path,
                                            paint.value
                                        )
                                    )

                                    lastPoint = offset
                                    isDrawing.value = true
                                    changesSize.intValue += 1

                                    event.changes.forEach {
                                        it.consume()
                                    }
                                }

                                PointerEventType.Release -> {
                                    val offset = event.changes.first().position
                                    val path = (modifications.findLast {
                                        it is DrawablePath
                                    } as DrawablePath).path

                                    path.lineTo(offset.x, offset.y)
                                    lastPoint = offset

                                    isDrawing.value = false
                                    changesSize.intValue += 1

                                    event.changes.forEach {
                                        it.consume()
                                    }
                                }
                            }
                        } else if (!canceled && paint.value.type == PaintType.Text) {
                            when (event.type) {
                                PointerEventType.Press -> {
                                    val position = event.changes.first().position

                                    val tappedOnText =
                                        modifications.filterIsInstance<DrawableText>().firstOrNull {
                                            checkIfClickedOnText(
                                                text = it,
                                                clickPosition = position
                                            )
                                        }

                                    if (tappedOnText == null) {
                                        val textLayout = textMeasurer.measure(
                                            text = "text",
                                            style = localTextStyle.copy(
                                                color = paint.value.color,
                                                fontSize = TextUnit(
                                                    paint.value.strokeWidth,
                                                    TextUnitType.Sp
                                                ),
                                                textAlign = defaultTextStyle.textAlign,
                                                platformStyle = defaultTextStyle.platformStyle,
                                                lineHeightStyle = defaultTextStyle.lineHeightStyle,
                                                baselineShift = defaultTextStyle.baselineShift
                                            )
                                        )

                                        val text = DrawableText(
                                            text = "text",
                                            position = Offset(
                                                position.x - textLayout.size.width / 2f,
                                                position.y - textLayout.size.height / 2f
                                            ),
                                            paint = paint.value,
                                            rotation = 90f * rotationMultiplier.intValue,
                                            size = textLayout.size
                                        )
                                        modifications.add(text)
                                        lastText = text
                                    } else {
                                        lastText = tappedOnText
                                    }

                                    touchOffset = position - lastText.position

                                    isDrawing.value = false
                                    changesSize.intValue += 1
                                    event.changes.forEach {
                                        it.consume()
                                    }
                                }

                                PointerEventType.Move -> {
                                    val offset = event.changes.first().position

                                    if (lastText != null) {
                                        modifications.remove(lastText)

                                        lastText.position += (offset - lastText.position - touchOffset)
                                        modifications.add(lastText)
                                    }

                                    isDrawing.value = true

                                    event.changes.forEach {
                                        it.consume()
                                    }
                                }

                                PointerEventType.Release -> {
                                    lastText = null
                                    isDrawing.value = false

                                    event.changes.forEach {
                                        it.consume()
                                    }
                                }
                            }
                        }
                    } while (!canceled && event.changes.any { it.pressed })
                }
            }
        }


    return this.then(modifier)
}

@Composable
private fun BoxWithConstraintsScope.DrawActionsAndColors(
    modifications: SnapshotStateList<DrawableItem>,
    paint: MutableState<ExtendedPaint>,
    changesSize: MutableIntState,
    landscapeMode: Boolean = false,
) {
    val neededWidth = if (landscapeMode) maxHeight else maxWidth

    var showColorPalette by remember { mutableStateOf(false) }
    val colorPaletteWidth by animateDpAsState(
        targetValue = if (showColorPalette) neededWidth - 24.dp else 0.dp, // - 24.dp to account for padding, stops the sudden width decrease
        animationSpec = tween(
            durationMillis = 350
        ),
        label = "Animate color palette show/hide"
    )

    Box(
        modifier = Modifier
            .padding(16.dp, 4.dp)
    ) {
        AnimatedVisibility(
            visible = !showColorPalette,
            enter = slideInVertically { height -> height } + fadeIn(),
            exit = slideOutVertically { height -> height } + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .height(48.dp)
                    .padding(
                        if (landscapeMode) 40.dp else 0.dp,
                        0.dp
                    ),
            ) {
                if (!landscapeMode) {
                    Column(
                        modifier = Modifier
                            .height(40.dp)
                            .width(64.dp)
                            .clip(CircleShape)
                            .background(
                                CustomMaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = 0.8f
                                )
                            )
                            .align(Alignment.CenterStart)
                            .clickable {
                                modifications.clear()
                                changesSize.intValue += 1
                            },
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Clear",
                            fontSize = TextUnit(14f, TextUnitType.Sp),
                            color = CustomMaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .wrapContentSize()
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                CustomMaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = 0.8f
                                )
                            )
                            .align(Alignment.CenterStart)
                            .clickable {
                                modifications.clear()
                                changesSize.intValue += 1
                            },
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.close),
                            contentDescription = "undo all actions",
                            tint = CustomMaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            CustomMaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.8f
                            )
                        )
                        .align(Alignment.Center)
                        .clickable {
                            modifications.removeLastOrNull()
                            changesSize.intValue += 1
                        },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.back_arrow),
                        contentDescription = "undo last action",
                        tint = CustomMaterialTheme.colorScheme.onSurface
                    )
                }

                ColorIndicator(
                    color = paint.value.color,
                    selected = false,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                ) {
                    showColorPalette = true
                }
            }
        }

        Row(
            modifier = Modifier
                .height(48.dp)
                .width(colorPaletteWidth)
                .align(if (landscapeMode) Alignment.TopEnd else Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            ColorIndicator(
                color = DrawingColors.White,
                selected = paint.value.color == DrawingColors.White
            ) {
                showColorPalette = false
                paint.value = paint.value.copy(color = DrawingColors.White)
            }

            ColorIndicator(
                color = DrawingColors.Black,
                selected = paint.value.color == DrawingColors.Black
            ) {
                showColorPalette = false
                paint.value = paint.value.copy(color = DrawingColors.Black)
            }

            ColorIndicator(
                color = DrawingColors.Red,
                selected = paint.value.color == DrawingColors.Red
            ) {
                showColorPalette = false
                paint.value = paint.value.copy(color = DrawingColors.Red)
            }

            ColorIndicator(
                color = DrawingColors.Yellow,
                selected = paint.value.color == DrawingColors.Yellow
            ) {
                showColorPalette = false
                paint.value = paint.value.copy(color = DrawingColors.Yellow)
            }

            ColorIndicator(
                color = DrawingColors.Green,
                selected = paint.value.color == DrawingColors.Green
            ) {
                showColorPalette = false
                paint.value = paint.value.copy(color = DrawingColors.Green)
            }

            ColorIndicator(
                color = DrawingColors.Blue,
                selected = paint.value.color == DrawingColors.Blue
            ) {
                showColorPalette = false
                paint.value = paint.value.copy(color = DrawingColors.Blue)
            }

            ColorIndicator(
                color = DrawingColors.Purple,
                selected = paint.value.color == DrawingColors.Purple
            ) {
                showColorPalette = false
                paint.value = paint.value.copy(color = DrawingColors.Purple)
            }
        }
    }
}

fun checkIfClickedOnText(text: DrawableText, clickPosition: Offset): Boolean {
    val textPosition = text.position
    val textSize = text.size

    val textRight = textPosition.x + textSize.width
    val textLeft = textPosition.x
    val textTop = textPosition.y
    val textBottom = textPosition.y + textSize.height

    val isInTextWidth = clickPosition.x in textLeft..textRight
    val isInTextHeight = clickPosition.y in textTop..textBottom

    return isInTextWidth && isInTextHeight
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxWithConstraintsScope.DrawingControls(
    pagerState: PagerState,
    isDrawing: MutableState<Boolean>,
    paint: MutableState<ExtendedPaint>,
    modifications: SnapshotStateList<DrawableItem>,
    changesSize: MutableIntState
) {
    val localConfiguration = LocalConfiguration.current
    val isLandscape by remember {
        derivedStateOf {
            localConfiguration.orientation == Configuration.ORIENTATION_LANDSCAPE
        }
    }

    var shouldShowDrawOptions by remember { mutableStateOf(false) }
    var lastPage by remember { mutableIntStateOf(pagerState.currentPage) }
    LaunchedEffect(isDrawing.value, pagerState.currentPage) {
        if (isDrawing.value) {
            shouldShowDrawOptions = false
        } else {
            if (lastPage != 3 && pagerState.currentPage == 3) {
                shouldShowDrawOptions = true
            } else {
                delay(500)
                shouldShowDrawOptions = true
            }
        }
        lastPage = pagerState.currentPage
    }

    val statusBarPadding = WindowInsets.safeContent.asPaddingValues()
        .calculateStartPadding(LocalLayoutDirection.current)
    AnimatedVisibility(
        visible = pagerState.currentPage == 3 && shouldShowDrawOptions,
        enter = slideInHorizontally { width -> -width } + fadeIn(),
        exit = slideOutHorizontally { width -> -width } + fadeOut(),
        modifier = Modifier
            .fillMaxHeight(1f)
            .padding(statusBarPadding, 16.dp),
    ) {
        var slideVal by remember { mutableFloatStateOf(paint.value.strokeWidth / 128f) }

        Slider(
            value = slideVal,
            onValueChange = { newVal ->
                slideVal = newVal
                paint.value = paint.value.copy(
                    strokeWidth = newVal * 128f
                )
            },
            steps = 16,
            valueRange = 0f..1f,
            thumb = {
                Box(
                    modifier = Modifier
                        .height(16.dp)
                        .width(8.dp)
                        .clip(CircleShape)
                        .background(CustomMaterialTheme.colorScheme.primary)
                )
            },
            modifier = Modifier
                .graphicsLayer {
                    rotationZ = 270f
                    translationX = 16.dp.toPx()
                    transformOrigin = TransformOrigin(0f, 0f)
                }
                .layout { measurable, constraints ->
                    val scale = if (isLandscape) 1.25f else 2f
                    val placeable = measurable.measure(
                        Constraints(
                            minWidth = (constraints.minHeight / scale).toInt(),
                            minHeight = constraints.minWidth,
                            maxWidth = (constraints.maxHeight / scale).toInt(),
                            maxHeight = constraints.maxWidth
                        )
                    )

                    layout(placeable.height, placeable.width) {
                        placeable.place(-placeable.width, -placeable.height / 2)
                    }
                }
        )
    }

    if (!isLandscape) {
        AnimatedVisibility(
            visible = pagerState.currentPage == 3 && shouldShowDrawOptions,
            enter = slideInVertically { height -> height } + fadeIn(),
            exit = slideOutVertically { height -> height } + fadeOut(),
            modifier = Modifier
                .fillMaxWidth(1f)
                .align(Alignment.BottomCenter),
        ) {
            DrawActionsAndColors(
                modifications = modifications,
                paint = paint,
                changesSize = changesSize
            )
        }
    } else {
        AnimatedVisibility(
            visible = pagerState.currentPage == 3 && shouldShowDrawOptions,
            enter = slideInVertically { width -> -width } + fadeIn(),
            exit = slideOutVertically { width -> -width } + fadeOut(),
            modifier = Modifier
                .fillMaxWidth(1f)
                .wrapContentHeight()
                .align(Alignment.CenterEnd)
                .graphicsLayer {
                    rotationZ = 90f
                    translationX = -8.dp.toPx()
                    transformOrigin = TransformOrigin(0f, 0f)
                }
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(
                        Constraints(
                            minWidth = constraints.minHeight,
                            minHeight = constraints.minWidth,
                            maxWidth = constraints.maxHeight,
                            maxHeight = constraints.maxWidth
                        )
                    )

                    layout(placeable.height, placeable.width) {
                        placeable.place(0, -placeable.height)
                    }
                }
        ) {
            DrawActionsAndColors(
                modifications = modifications,
                paint = paint,
                changesSize = changesSize,
                landscapeMode = true
            )
        }
    }
}


fun checkIfInRect(
    position: Offset,
    padding: Float,
    left: Int,
    top: Int,
    right: Int,
    bottom: Int
): Boolean {
    val isPastLeft = position.x > left + padding
    val isBeforeRight = position.x < right - padding
    val isBelowTop = position.y > top - padding
    val isAboveBottom = position.y < bottom + padding

    println("POS $position $isPastLeft $isBeforeRight $isBelowTop $isAboveBottom")

    return isPastLeft && isBeforeRight && isBelowTop && isAboveBottom
}

fun IntOffset.coerceIn(
    minX: Int,
    minY: Int,
    maxX: Int,
    maxY: Int
): IntOffset = IntOffset(this.x.coerceIn(minX, maxX), this.y.coerceIn(minY, maxY))

operator fun IntOffset.plus(offset: Offset): IntOffset =
    IntOffset(this.x + offset.x.toInt(), this.y + offset.y.toInt())
