package com.kaii.photos.compose.single_photo

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.Window
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
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
import androidx.compose.runtime.MutableFloatState
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
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
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.BottomAppBarItem
import com.kaii.photos.compose.ColorRangeSlider
import com.kaii.photos.compose.ConfirmationDialog
import com.kaii.photos.compose.CroppingRatioBottomSheet
import com.kaii.photos.compose.getAppBarContentTransition
import com.kaii.photos.compose.SelectableDropDownMenuItem
import com.kaii.photos.compose.PopupPillSlider
import com.kaii.photos.compose.SetEditingViewDrawableTextBottomSheet
import com.kaii.photos.compose.SplitButton
import com.kaii.photos.compose.setBarVisibility
import com.kaii.photos.helpers.ColorIndicator
import com.kaii.photos.helpers.CustomMaterialTheme
import com.kaii.photos.helpers.DrawablePath
import com.kaii.photos.helpers.DrawableText
import com.kaii.photos.helpers.DrawingColors
import com.kaii.photos.helpers.DrawingPaints
import com.kaii.photos.helpers.ExtendedPaint
import com.kaii.photos.helpers.Modification
import com.kaii.photos.helpers.PaintType
import com.kaii.photos.helpers.getColorFromLinearGradientList
import com.kaii.photos.helpers.gradientColorList
import com.kaii.photos.helpers.ColorFiltersMatrices
import com.kaii.photos.helpers.savePathListToBitmap
import com.kaii.photos.helpers.toOffset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

@Composable
fun EditingView(
    absolutePath: String,
    dateTaken: Long,
    uri: Uri,
    window: Window
) {
    val navController = LocalNavController.current
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

    val modifications = remember { mutableStateListOf<Modification>() }
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

    val croppingRatio = remember { mutableFloatStateOf(0f) }

    val textMeasurer = rememberTextMeasurer()
    val localDensity = LocalDensity.current

    val adjustSliderValue = remember { mutableFloatStateOf(0f) }
    val colorMatrix = remember { mutableStateOf(ColorMatrix()) }
    val currentFilter = remember { mutableStateOf(ColorFiltersMatrices["None"]!!) }

    Scaffold(
        topBar = {
            val overwrite = remember { mutableStateOf(false) }

            EditingViewTopBar(
                showCloseDialog = showCloseDialog,
                changesSize = changesSize,
                oldChangesSize = oldChangesSize,
                overwrite = overwrite,
                saveImage = suspend {
                    savePathListToBitmap(
                        modifications = modifications,
                        adjustmentColorMatrix = colorMatrix.value,
                        absolutePath = absolutePath,
                        dateTaken = dateTaken,
                        uri = uri,
                        image = image,
                        maxSize = maxSize,
                        rotation = rotation,
                        textMeasurer = textMeasurer,
                        overwrite = overwrite.value,
                        context = context
                    )
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
                croppingRatio = croppingRatio,
                originalImageRatio = image.width.toFloat() / image.height.toFloat(),
                adjustSliderValue = adjustSliderValue,
                colorMatrix = colorMatrix,
                currentFilter = currentFilter,
                window = window,
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

        val localConfig = LocalConfiguration.current
        var isLandscape by remember { mutableStateOf(localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) }

        LaunchedEffect(localConfig) {
            isLandscape = localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        }

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

            val isVertical by remember { derivedStateOf { rotation % 180f == 0f } }
            val isHorizontal by remember { derivedStateOf { abs(rotation % 180f) == 90f } }

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
            val minScale by remember { derivedStateOf { 0.8f * maxScale } }

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

            val canvasAppropriateImage by remember {
                derivedStateOf {
                    val imageRatio = image.width.toFloat() / image.height.toFloat()
                    val canvasWidth = size.width * imageRatio
                    val canvasHeight = canvasWidth / imageRatio

                    Bitmap.createScaledBitmap(
                        image.asAndroidBitmap(),
                        canvasWidth.toInt(),
                        canvasHeight.toInt(),
                        true
                    ).asImageBitmap()
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
                    image = canvasAppropriateImage,
                    dstSize = size,
                    colorFilter = ColorFilter.colorMatrix(colorMatrix.value)
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

            if (pagerState.currentPage == 0) {
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

                LaunchedEffect(croppingRatio.floatValue) {
                    if (croppingRatio.floatValue == 0f) return@LaunchedEffect

                    val width = topRightOffset.x - topLeftOffset.x
                    val height = bottomRightOffset.y - topRightOffset.y

                    if (croppingRatio.floatValue >= 1f) {
                        // if not freeform and wide ratio

                        topLeftOffset =
                            IntOffset(
                                topRightOffset.x - width,
                                (bottomRightOffset.y - width / croppingRatio.floatValue).toInt()
                            ).coerceIn(
                                minX = 0,
                                minY = 0,
                                maxX = (topRightOffset.x - (with(localDensity) { 56.dp.toPx() } * croppingRatio.floatValue).toInt()).coerceAtLeast(0),
                                maxY = (bottomLeftOffset.y - with(localDensity) { 56.dp.toPx().toInt() }).coerceAtLeast(0)
                            )
                    } else {
                        // if not freeform and tall ratio

                        topLeftOffset =
                            IntOffset(
                                (topRightOffset.x - height * croppingRatio.floatValue).toInt(),
                                bottomRightOffset.y - height
                            ).coerceIn(
                                minX = 0,
                                minY = 0,
                                maxX = (topRightOffset.x - with(localDensity) { 56.dp.toPx() }.toInt()).coerceAtLeast(0),
                                maxY = (bottomLeftOffset.y - (with(localDensity) { 56.dp.toPx() } / croppingRatio.floatValue).toInt()).coerceAtLeast(0)
                            )
                    }

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
                                var width = ((topRightOffset.x - topLeftOffset.x) * ratio)
                                    .toInt()
                                    .coerceIn(0, image.width)
                                var height = ((bottomLeftOffset.y - topLeftOffset.y) * ratio)
                                    .toInt()
                                    .coerceIn(0, image.height)

                                val widthExtra = (x + width) - image.width
                                val heightExtra = (y + height) - image.height

                                if (widthExtra > 0) width -= widthExtra
                                if (heightExtra > 0) height -= heightExtra

                                if (width > 0 && height > 0) {
                                    val loadable = Bitmap
                                        .createBitmap(
                                            image.asAndroidBitmap(),
                                            x,
                                            y,
                                            width,
                                            height
                                        )

                                    image = loadable
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
                                        alpha = 0.75f,
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
                                    with(localDensity) {
                                        24.dp.toPx() * ((topRightOffset.x - topLeftOffset.x).toFloat() / size.width)
                                    },
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
                                            maxX = (topRightOffset.x - 56.dp
                                                .toPx()
                                                .toInt()).coerceAtLeast(0),
                                            maxY = (bottomLeftOffset.y - 56.dp
                                                .toPx()
                                                .toInt()).coerceAtLeast(0)
                                        )

                                    if (croppingRatio.floatValue == 0f) {
                                        // if freeform crop
                                        topLeftOffset = topLeftOffsetPos
                                    } else if (croppingRatio.floatValue >= 1f) {
                                        // if not freeform and wide ratio
                                        val width = topRightOffset.x - topLeftOffsetPos.x
                                        val height = bottomRightOffset.y - topLeftOffsetPos.y

                                        val positionChange = topLeftOffsetPos - topLeftOffset

                                        if (height + positionChange.y !in height - 5..height + 5) {
                                            topLeftOffset =
                                                IntOffset(
                                                    (topRightOffset.x - height * croppingRatio.floatValue).toInt(),
                                                    bottomRightOffset.y - height
                                                ).coerceIn(
                                                    minX = 0,
                                                    minY = bottomRightOffset.y - (width / croppingRatio.floatValue).toInt(),
                                                    maxX = (topRightOffset.x - 56.dp
                                                        .toPx()
                                                        .toInt()).coerceAtLeast(0),
                                                    maxY = (bottomLeftOffset.y - (56.dp.toPx() / croppingRatio.floatValue).toInt()).coerceAtLeast(0)
                                                )
                                        } else if (width + positionChange.x !in width - 5..width + 5) {
                                            topLeftOffset =
                                                IntOffset(
                                                    topRightOffset.x - width,
                                                    (bottomRightOffset.y - width / croppingRatio.floatValue).toInt()
                                                ).coerceIn(
                                                    minX = 0,
                                                    minY = bottomRightOffset.y - width,
                                                    maxX = (topRightOffset.x - (56.dp.toPx() * croppingRatio.floatValue).toInt()).coerceAtLeast(0),
                                                    maxY = (bottomLeftOffset.y - 56.dp
                                                        .toPx()
                                                        .toInt()).coerceAtLeast(0)
                                                )
                                        }
                                    } else {
                                        // if not freeform and tall ratio
                                        val width = topRightOffset.x - topLeftOffsetPos.x
                                        val height = bottomRightOffset.y - topLeftOffsetPos.y

                                        val positionChange = topLeftOffsetPos - topLeftOffset

                                        if (height + positionChange.y !in height - 5..height + 5) {
                                            topLeftOffset =
                                                IntOffset(
                                                    (topRightOffset.x - height * croppingRatio.floatValue).toInt(),
                                                    bottomRightOffset.y - height
                                                ).coerceIn(
                                                    minX = bottomRightOffset.x - height,
                                                    minY = 0,
                                                    maxX = (topRightOffset.x - 56.dp
                                                        .toPx()
                                                        .toInt()).coerceAtLeast(0),
                                                    maxY = (bottomLeftOffset.y - (56.dp.toPx() / croppingRatio.floatValue).toInt()).coerceAtLeast(0)
                                                )
                                        } else if (width + positionChange.x !in width - 5..width + 5) {
                                            topLeftOffset =
                                                IntOffset(
                                                    topRightOffset.x - width,
                                                    (bottomRightOffset.y - width / croppingRatio.floatValue).toInt()
                                                ).coerceIn(
                                                    minX = bottomRightOffset.x - (height * croppingRatio.floatValue).toInt(),
                                                    minY = 0,
                                                    maxX = (topRightOffset.x - (56.dp.toPx() * croppingRatio.floatValue).toInt()).coerceAtLeast(0),
                                                    maxY = (bottomLeftOffset.y - 56.dp
                                                        .toPx()
                                                        .toInt()).coerceAtLeast(0)
                                                )
                                        }
                                    }

                                    bottomLeftOffset =
                                        IntOffset(
                                            topLeftOffset.x,
                                            bottomLeftOffset.y
                                        )

                                    topRightOffset =
                                        IntOffset(
                                            topRightOffset.x,
                                            topLeftOffset.y
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

                                    val topRightOffsetPos = (topRightOffset + dragAmount)
                                        .coerceIn(
                                            minX = (topLeftOffset.x + 56.dp
                                                .toPx()
                                                .toInt()).coerceAtMost(size.width),
                                            minY = 0,
                                            maxX = size.width,
                                            maxY = (bottomRightOffset.y - 56.dp
                                                .toPx()
                                                .toInt()).coerceAtLeast(0)
                                        )

                                    if (croppingRatio.floatValue == 0f) {
                                        // if freeform crop
                                        topRightOffset = topRightOffsetPos
                                    } else if (croppingRatio.floatValue >= 1f) {
                                        // if not freeform and wide ratio
                                        val width = topRightOffsetPos.x - topLeftOffset.x
                                        val height = bottomRightOffset.y - topRightOffsetPos.y

                                        val positionChange = topRightOffsetPos - topRightOffset

                                        if (height + positionChange.y !in height - 5..height + 5) {
                                            topRightOffset =
                                                IntOffset(
                                                    (topLeftOffset.x + height * croppingRatio.floatValue).toInt(),
                                                    bottomRightOffset.y - height
                                                ).coerceIn(
                                                    minX = (topLeftOffset.x + 56.dp
                                                        .toPx()
                                                        .toInt()),
                                                    minY = bottomRightOffset.y - (width / croppingRatio.floatValue).toInt(),
                                                    maxX = size.width,
                                                    maxY = (bottomRightOffset.y - (56.dp.toPx() / croppingRatio.floatValue).toInt()).coerceAtLeast(0)
                                                )
                                        } else if (width + positionChange.x !in width - 5..width + 5) {
                                            topRightOffset =
                                                IntOffset(
                                                    topLeftOffset.x + width,
                                                    (bottomRightOffset.y - width / croppingRatio.floatValue).toInt()
                                                ).coerceIn(
                                                    minX = (topLeftOffset.x + (56.dp.toPx() * croppingRatio.floatValue).toInt()),
                                                    minY = bottomRightOffset.y - width,
                                                    maxX = size.width,
                                                    maxY = (bottomRightOffset.y - 56.dp
                                                        .toPx()
                                                        .toInt()).coerceAtLeast(0)
                                                )
                                        }
                                    } else {
                                        // if not freeform and tall ratio
                                        val width = topRightOffsetPos.x - topLeftOffset.x
                                        val height = bottomRightOffset.y - topRightOffsetPos.y

                                        val positionChange = topRightOffsetPos - topRightOffset

                                        if (height + positionChange.y !in height - 5..height + 5) {
                                            topRightOffset =
                                                IntOffset(
                                                    topLeftOffset.x + (height * croppingRatio.floatValue).toInt(),
                                                    bottomRightOffset.y - height
                                                ).coerceIn(
                                                    minX = topLeftOffset.x + 56.dp
                                                        .toPx()
                                                        .toInt(),
                                                    minY = 0,
                                                    maxX = size.width,
                                                    maxY = (bottomRightOffset.y - (56.dp.toPx() / croppingRatio.floatValue).toInt()).coerceAtLeast(0)
                                                )
                                        } else if (width + positionChange.x !in width - 5..width + 5) {
                                            topRightOffset =
                                                IntOffset(
                                                    topLeftOffset.x + width,
                                                    (bottomRightOffset.y - width / croppingRatio.floatValue).toInt()
                                                ).coerceIn(
                                                    minX = topLeftOffset.x + 56.dp
                                                        .toPx()
                                                        .toInt(),
                                                    minY = (bottomRightOffset.y - (height / croppingRatio.floatValue).toInt()).coerceAtLeast(0),
                                                    maxX = topLeftOffset.x + (height * croppingRatio.floatValue).toInt(),
                                                    maxY = (bottomRightOffset.y - 56.dp
                                                        .toPx()
                                                        .toInt()).coerceAtLeast(0)
                                                )
                                        }
                                    }

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

                                    val bottomLeftOffsetPos = (bottomLeftOffset + dragAmount)
                                        .coerceIn(
                                            minX = 0,
                                            minY = (topLeftOffset.y + 56.dp
                                                .toPx()
                                                .toInt()).coerceAtMost(size.height),
                                            maxX = (bottomRightOffset.x - 56.dp
                                                .toPx()
                                                .toInt()).coerceAtLeast(0),
                                            maxY = size.height
                                        )

                                    if (croppingRatio.floatValue == 0f) {
                                        // if freeform crop
                                        bottomLeftOffset = bottomLeftOffsetPos
                                    } else if (croppingRatio.floatValue >= 1f) {
                                        // if not freeform and wide ratio
                                        val width = bottomRightOffset.x - bottomLeftOffsetPos.x
                                        val height = bottomLeftOffsetPos.y - topLeftOffset.y

                                        val positionChange = bottomLeftOffsetPos - bottomLeftOffset

                                        if (height + positionChange.y !in height - 5..height + 5) {
                                            bottomLeftOffset =
                                                IntOffset(
                                                    (bottomRightOffset.x - height * croppingRatio.floatValue).toInt(),
                                                    topLeftOffset.y + height
                                                ).coerceIn(
                                                    minX = 0,
                                                    minY = topRightOffset.y + (56.dp.toPx() / croppingRatio.floatValue).toInt(),
                                                    maxX = bottomRightOffset.x - (56.dp.toPx() * croppingRatio.floatValue).toInt(),
                                                    maxY = topLeftOffset.y + (width / croppingRatio.floatValue).toInt()
                                                )
                                        } else if (width + positionChange.x !in width - 5..width + 5) {
                                            bottomLeftOffset =
                                                IntOffset(
                                                    bottomRightOffset.x - width,
                                                    (topLeftOffset.y + width / croppingRatio.floatValue).toInt()
                                                ).coerceIn(
                                                    minX = 0,
                                                    minY = topRightOffset.y + 56.dp
                                                        .toPx()
                                                        .toInt(),
                                                    maxX = bottomRightOffset.x - (56.dp.toPx() * croppingRatio.floatValue).toInt(),
                                                    maxY = topLeftOffset.y + width
                                                )
                                        }
                                    } else {
                                        // if not freeform and tall ratio
                                        val width = bottomRightOffset.x - bottomLeftOffsetPos.x
                                        val height = bottomLeftOffsetPos.y - topLeftOffset.y

                                        val positionChange = bottomLeftOffsetPos - bottomLeftOffset

                                        if (height + positionChange.y !in height - 5..height + 5) {
                                            bottomLeftOffset =
                                                IntOffset(
                                                    (topRightOffset.x - height * croppingRatio.floatValue).toInt(),
                                                    topLeftOffset.y + height
                                                ).coerceIn(
                                                    minX = 0,
                                                    minY = topLeftOffset.y + (56.dp.toPx() / croppingRatio.floatValue).toInt(),
                                                    maxX = bottomRightOffset.x - 56.dp
                                                        .toPx()
                                                        .toInt(),
                                                    maxY = topLeftOffset.y + (width / croppingRatio.floatValue).toInt()
                                                )
                                        } else if (width + positionChange.x !in width - 5..width + 5) {
                                            bottomLeftOffset =
                                                IntOffset(
                                                    topRightOffset.x - width,
                                                    (topLeftOffset.y + width / croppingRatio.floatValue).toInt()
                                                ).coerceIn(
                                                    minX = topRightOffset.x - (height * croppingRatio.floatValue).toInt(),
                                                    minY = topLeftOffset.y + (56.dp.toPx() / croppingRatio.floatValue).toInt(),
                                                    maxX = bottomRightOffset.x - 56.dp
                                                        .toPx()
                                                        .toInt(),
                                                    maxY = (topLeftOffset.y + (width / croppingRatio.floatValue).toInt()).coerceAtMost(size.height)
                                                )
                                        }
                                    }

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

                                    val bottomRightOffsetPos = (bottomRightOffset + dragAmount)
                                        .coerceIn(
                                            minX = (bottomLeftOffset.x + 56.dp
                                                .toPx()
                                                .toInt()).coerceAtMost(size.width),
                                            minY = (topRightOffset.y + 56.dp
                                                .toPx()
                                                .toInt()).coerceAtMost(size.height),
                                            maxX = size.width,
                                            maxY = size.height
                                        )

                                    if (croppingRatio.floatValue == 0f) {
                                        // if freeform crop
                                        bottomRightOffset = bottomRightOffsetPos
                                    } else if (croppingRatio.floatValue >= 1f) {
                                        // if not freeform and wide ratio
                                        val width = bottomRightOffsetPos.x - bottomLeftOffset.x
                                        val height = bottomRightOffsetPos.y - topRightOffset.y

                                        val positionChange = bottomRightOffsetPos - bottomRightOffset

                                        if (height + positionChange.y !in height - 5..height + 5) {
                                            bottomRightOffset =
                                                IntOffset(
                                                    (bottomLeftOffset.x + height * croppingRatio.floatValue).toInt(),
                                                    topRightOffset.y + height
                                                ).coerceIn(
                                                    minX = bottomLeftOffset.x + (56.dp
                                                        .toPx()
                                                        .toInt() * croppingRatio.floatValue).toInt(),
                                                    minY = topRightOffset.y + (56.dp.toPx() / croppingRatio.floatValue).toInt(),
                                                    maxX = (bottomLeftOffset.x + (width * croppingRatio.floatValue).toInt()).coerceAtMost(size.width),
                                                    maxY = topRightOffset.y + (width / croppingRatio.floatValue).toInt()
                                                )
                                        } else if (width + positionChange.x !in width - 5..width + 5) {
                                            bottomRightOffset =
                                                IntOffset(
                                                    bottomLeftOffset.x + width,
                                                    (topRightOffset.y + width / croppingRatio.floatValue).toInt()
                                                ).coerceIn(
                                                    minX = bottomLeftOffset.x + (56.dp.toPx() * croppingRatio.floatValue).toInt(),
                                                    minY = topLeftOffset.y + 56.dp
                                                        .toPx()
                                                        .toInt(),
                                                    maxX = bottomLeftOffset.x + (height * croppingRatio.floatValue).toInt(),
                                                    maxY = topRightOffset.y + height
                                                )
                                        }
                                    } else {
                                        // if not freeform and tall ratio
                                        val width = bottomRightOffsetPos.x - bottomLeftOffset.x
                                        val height = bottomRightOffsetPos.y - topRightOffset.y

                                        val positionChange = bottomRightOffsetPos - bottomLeftOffset

                                        if (height + positionChange.y !in height - 5..height + 5) {
                                            bottomRightOffset =
                                                IntOffset(
                                                    (bottomLeftOffset.x + height * croppingRatio.floatValue).toInt(),
                                                    topRightOffset.y + height
                                                ).coerceIn(
                                                    minX = bottomLeftOffset.x + 56.dp
                                                        .toPx()
                                                        .toInt(),
                                                    minY = topRightOffset.y + (56.dp.toPx() / croppingRatio.floatValue).toInt(),
                                                    maxX = (bottomLeftOffset.x + (height * croppingRatio.floatValue).toInt()).coerceAtMost(size.width),
                                                    maxY = (topRightOffset.y + (width / croppingRatio.floatValue).toInt()).coerceAtMost(size.height)
                                                )
                                        } else if (width + positionChange.x !in width - 5..width + 5) {
                                            bottomRightOffset =
                                                IntOffset(
                                                    bottomLeftOffset.x + width,
                                                    (topRightOffset.y + width / croppingRatio.floatValue).toInt()
                                                ).coerceIn(
                                                    minX = bottomLeftOffset.x + 56.dp
                                                        .toPx()
                                                        .toInt(),
                                                    minY = topLeftOffset.y + (56.dp.toPx() / croppingRatio.floatValue).toInt(),
                                                    maxX = (bottomLeftOffset.x + (height * croppingRatio.floatValue).toInt()).coerceAtMost(size.width),
                                                    maxY = (topRightOffset.y + (width / croppingRatio.floatValue).toInt()).coerceAtMost(size.height)
                                                )
                                        }
                                    }

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
    overwrite: MutableState<Boolean>,
    saveImage: suspend () -> Unit,
    popBackStack: () -> Unit,
) {
    val localConfig = LocalConfiguration.current
    var isLandscape by remember { mutableStateOf(localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) }

    LaunchedEffect(localConfig) {
        isLandscape = localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    var showInLandscape by remember { mutableStateOf(false) }

    var canExit by remember { mutableStateOf(true) }

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

    val coroutineScope = rememberCoroutineScope()

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
                    FilledTonalIconButton(
                        onClick = {
                            if (changesSize.intValue != oldChangesSize.intValue) {
                                showCloseDialog.value = true
                            } else {
                                oldChangesSize.intValue = changesSize.intValue
                                popBackStack()
                            }
                        },
                        enabled = canExit,
                        modifier = Modifier
                            .height(40.dp)
                            .width(56.dp)
                            .align(Alignment.Center)
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
                Row(
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 8.dp, 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (!canExit) {
                        CircularProgressIndicator(
                            color = CustomMaterialTheme.colorScheme.secondary,
                            strokeWidth = 4.dp,
                            strokeCap = StrokeCap.Round,
                            trackColor = Color.Transparent,
                            modifier = Modifier
                                .size(28.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    var saveButtonTitle by remember { mutableStateOf("Save Copy") }
                    val saveAction: () -> Unit = {
                        oldChangesSize.intValue = changesSize.intValue

                        coroutineScope.launch {
                            canExit = false
                            saveImage()

                            delay(500)
                            canExit = true
                        }
                    }

                    var dropDownExpanded by remember { mutableStateOf(false) }
                    SplitButton(
                        enabled = changesSize.intValue != oldChangesSize.intValue,
                        secondaryContentMaxWidth = 40.dp,
                        primaryContent = {
                            Text(
                                text = saveButtonTitle,
                                fontSize = TextUnit(14f, TextUnitType.Sp),
                                color = CustomMaterialTheme.colorScheme.onPrimary
                            )
                        },
                        primaryAction = saveAction,
                        secondaryAction = {
                            dropDownExpanded = true
                        },
                        secondaryContent = {
                            Icon(
                                painter = painterResource(id = R.drawable.drop_down_arrow),
                                contentDescription = "Show save options",
                                modifier = Modifier
                                    .size(32.dp)
                            )
                        }
                    )

                    DropdownMenu(
                        expanded = dropDownExpanded,
                        onDismissRequest = {
                            dropDownExpanded = false
                        },
                        shape = RoundedCornerShape(24.dp),
                        properties = PopupProperties(
                            dismissOnClickOutside = true,
                            dismissOnBackPress = true
                        ),
                        containerColor = CustomMaterialTheme.colorScheme.primaryContainer,
                        shadowElevation = 8.dp
                    ) {
                        SelectableDropDownMenuItem(
                            text = "Overwrite Original",
                            iconResId = R.drawable.file_is_selected_foreground,
                            isSelected = overwrite.value
                        ) {
                            dropDownExpanded = false
                            overwrite.value = true
                            saveButtonTitle = "Save"
                            saveAction()
                        }

                        SelectableDropDownMenuItem(
                            text = "Save Copy",
                            iconResId = R.drawable.file_is_selected_foreground,
                            isSelected = !overwrite.value
                        ) {
                            dropDownExpanded = false
                            overwrite.value = false
                            saveButtonTitle = "Save Copy"
                            saveAction()
                        }
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
    croppingRatio: MutableFloatState,
    originalImageRatio: Float,
    adjustSliderValue: MutableFloatState,
    colorMatrix: MutableState<ColorMatrix>,
    currentFilter: MutableState<ColorMatrix>,
    window: Window,
    resetCropping: () -> Unit
) {
    val localConfig = LocalConfiguration.current
    var isLandscape by remember { mutableStateOf(localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) }

    LaunchedEffect(localConfig) {
        isLandscape = localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            setBarVisibility(
                visible = false,
                window = window
            ) { _ -> }
        }
    }

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

        val animatedSliderHeight by animateDpAsState(
            targetValue = if (pagerState.currentPage == 1) 48.dp else 0.dp,
            animationSpec = tween(
                durationMillis = 350
            ),
            label = "Animate editing view bottom bar slider height"
        )

        val selectedProperty = remember { mutableStateOf(SelectedImageProperties.Contrast) }
		AnimatedContent(
			targetState = selectedProperty.value != SelectedImageProperties.ColorTint,
			transitionSpec = {
				getAppBarContentTransition(selectedProperty.value != SelectedImageProperties.ColorTint)
			},
			modifier = Modifier
				.fillMaxWidth(1f)
				.height(animatedSliderHeight)
		) { isNotColorSlider ->
			BoxWithConstraints(
			    modifier = Modifier
			        .fillMaxWidth(1f)
			        .height(animatedSliderHeight)
			        .background(
			            if (isLandscape) {
			                CustomMaterialTheme.colorScheme.surfaceContainer
			            } else {
			                Color.Transparent
			            }
			        )
			) {
				if (isNotColorSlider) {
					PopupPillSlider(sliderValue = adjustSliderValue, changesSize = changesSize)
				} else {
					ColorRangeSlider(sliderValue = adjustSliderValue)
				}
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

					var adjustmentsResult by remember { mutableStateOf(emptyList<Float>()) }

					LaunchedEffect(currentFilter.value.values, adjustmentsResult) {
						if (adjustmentsResult.isEmpty()) return@LaunchedEffect

				        val floatArray = emptyList<Float>().toMutableList()

						val filter = currentFilter.value.values
				        for (i in filter.indices) {
				            var item = adjustmentsResult[i]

				            if (i == 4 || i == 9 || i == 14) {
				            	item += filter[i]
				            } else {
				            	if (item == 0f) item = filter[i]
				            	else item *= filter[i]
				            }

				            floatArray.add(item)
				        }

						colorMatrix.value = ColorMatrix(floatArray.toTypedArray().toFloatArray())
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
                                    croppingRatio = croppingRatio,
                                    originalImageRatio = originalImageRatio,
                                    resetCropping = resetCropping
                                )
                            }

                            1 -> {
								AdjustTools(
									sliderValue = adjustSliderValue,
									colorMatrix = colorMatrix,
									selectedProperty = selectedProperty
								) {
									adjustmentsResult = it
								}
                            }

                            2 -> {
			                    FiltersTools(
			                    	colorMatrix = colorMatrix,
			                    	currentFilter = currentFilter,
			                    	changesSize = changesSize
			                    )
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
    croppingRatio: MutableFloatState,
    originalImageRatio: Float,
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

        val showBottomSheet = remember { mutableStateOf(false) }
        CroppingRatioBottomSheet(
            showBottomSheet = showBottomSheet,
            originalImageRatio = originalImageRatio,
            onSetCroppingRatio = { ratio ->
                croppingRatio.floatValue = ratio
            }
        )
        EditingViewBottomAppBarItem(text = "Ratio", iconResId = R.drawable.resolution) {
            showBottomSheet.value = true
        }

        EditingViewBottomAppBarItem(text = "Reset", iconResId = R.drawable.reset) {
            resetCropping()
            changesSize.intValue += 1
        }
    }
}

enum class SelectedImageProperties {
    Contrast,
    Brightness,
    Saturation,
    BlackPoint,
    WhitePoint,
    Shadows,
    Warmth,
    ColorTint,
    Highlights
}

@Composable
fun AdjustTools(
    sliderValue: MutableFloatState,
    colorMatrix: MutableState<ColorMatrix>,
    selectedProperty: MutableState<SelectedImageProperties>,
    onAdjustmentsDone: (List<Float>) -> Unit
) {
    var contrastValue by rememberSaveable { mutableFloatStateOf(0f) }
    var brightnessValue by rememberSaveable { mutableFloatStateOf(0f) }
    var saturationValue by rememberSaveable { mutableFloatStateOf(0f) }
    var blackPointValue by rememberSaveable { mutableFloatStateOf(0f) }
    var whitePointValue by rememberSaveable { mutableFloatStateOf(0f) }
    var warmthValue by rememberSaveable { mutableFloatStateOf(0f) }
    var colorTintValue by rememberSaveable { mutableFloatStateOf(-1.2f) }
    var highlightsValue by rememberSaveable { mutableFloatStateOf(0f) }

    val additiveEmptyArray = floatArrayOf(
        0f, 0f, 0f, 0f, 0f,
        0f, 0f, 0f, 0f, 0f,
        0f, 0f, 0f, 0f, 0f,
        0f, 0f, 0f, 0f, 0f
    )

    val multiplicativeEmptyArray = floatArrayOf(
        1f, 1f, 1f, 0f, 1f,
        1f, 1f, 1f, 0f, 1f,
        1f, 1f, 1f, 0f, 1f,
        0f, 0f, 0f, 1f, 0f
    )

    var contrastMatrix by rememberSaveable { mutableStateOf(multiplicativeEmptyArray) }
    var brightnessMatrix by rememberSaveable { mutableStateOf(additiveEmptyArray) }

    var saturationMatrix by rememberSaveable {
        mutableStateOf(
            ColorMatrix().apply {
                setToSaturation(1f)
                set(0, 4, 1f)
                set(1, 4, 1f)
                set(2, 4, 1f)
            }.values
        )
    }

    var blackPointMatrix by rememberSaveable { mutableStateOf(additiveEmptyArray) }
    var whitePointMatrix by rememberSaveable { mutableStateOf(multiplicativeEmptyArray) }
    var warmthMatrix by rememberSaveable { mutableStateOf(multiplicativeEmptyArray) }
    var highlightsMatrix by rememberSaveable { mutableStateOf(multiplicativeEmptyArray) }
    var colorTintMatrix1 by rememberSaveable { mutableStateOf(multiplicativeEmptyArray) }
    var colorTintMatrix2 by rememberSaveable { mutableStateOf(additiveEmptyArray) }

    LaunchedEffect(brightnessMatrix, contrastMatrix, saturationMatrix, blackPointMatrix, warmthMatrix, whitePointMatrix, colorTintMatrix2, highlightsMatrix) {
        val floatArray = emptyList<Float>().toMutableList()

        for (i in contrastMatrix.indices) {
            val multiply = contrastMatrix[i] * saturationMatrix[i] * warmthMatrix[i] * whitePointMatrix[i] * highlightsMatrix[i] * colorTintMatrix1[i]
            val add = brightnessMatrix[i] + blackPointMatrix[i] + colorTintMatrix2[i]

            floatArray.add(multiply + add)
        }

        onAdjustmentsDone(floatArray)
    }

    LaunchedEffect(sliderValue.floatValue) {
        when (selectedProperty.value) {
            SelectedImageProperties.Contrast -> run {
                if (sliderValue.floatValue == contrastValue) return@run

                val contrast = sliderValue.floatValue + 1f
                val offset = 0.5f * (1f - contrast) * 255f

                val floatArray = floatArrayOf(
                    contrast, contrast, contrast, 0f, offset,
                    contrast, contrast, contrast, 0f, offset,
                    contrast, contrast, contrast, 0f, offset,
                    0f, 0f, 0f, 1f, 0f
                )

                contrastMatrix = floatArray

                contrastValue = sliderValue.floatValue
            }

            SelectedImageProperties.Brightness -> run {
                if (sliderValue.floatValue == brightnessValue) return@run

                val brightness = sliderValue.floatValue
                val offset = brightness * 127f

                val floatArray = floatArrayOf(
                    0f, 0f, 0f, 0f, offset,
                    0f, 0f, 0f, 0f, offset,
                    0f, 0f, 0f, 0f, offset,
                    0f, 0f, 0f, 0f, 0f
                )

                brightnessMatrix = floatArray

                brightnessValue = sliderValue.floatValue
            }

            SelectedImageProperties.Saturation -> run {
                if (sliderValue.floatValue == saturationValue) return@run

                val saturation = sliderValue.floatValue + 1f

                val newMatrix = ColorMatrix()
                newMatrix.setToSaturation(saturation)
                newMatrix[0, 4] = 1f
                newMatrix[1, 4] = 1f
                newMatrix[2, 4] = 1f

                saturationMatrix = newMatrix.values

                saturationValue = sliderValue.floatValue
            }

            SelectedImageProperties.BlackPoint -> run {
                if (sliderValue.floatValue == blackPointValue) return@run

                val blackPoint = 150f * -sliderValue.floatValue
                val floatArray = floatArrayOf(
                    0f, 0f, 0f, 0f, blackPoint,
                    0f, 0f, 0f, 0f, blackPoint,
                    0f, 0f, 0f, 0f, blackPoint,
                    0f, 0f, 0f, 0f, 0f
                )

                blackPointMatrix = floatArray

                blackPointValue = sliderValue.floatValue
            }

            SelectedImageProperties.WhitePoint -> run {
                if (sliderValue.floatValue == whitePointValue) return@run

                val whitePoint = sliderValue.floatValue + 1f

                val floatArray = floatArrayOf(
                    whitePoint, 1f, 1f, 0f, 1f,
                    1f, whitePoint, 1f, 0f, 1f,
                    1f, 1f, whitePoint, 0f, 1f,
                    0f, 0f, 0f, 1f, 0f
                )

                whitePointMatrix = floatArray

                whitePointValue = sliderValue.floatValue
            }

            SelectedImageProperties.Warmth -> run {
                if (sliderValue.floatValue == warmthValue) return@run

                // linear equation y = ax + b
                // shifts input by 0.65f
                val slider = (-sliderValue.floatValue * 0.4375f + 0.65f)

                // taken from https://tannerhelland.com/2012/09/18/convert-temperature-rgb-algorithm-code.html and modified for brighter blues
                // values rounded because idk if the quality difference is enough to warrant casting 700 things to float and double
                val warmth = slider * 100f
                var red: Float
                var green: Float
                var blue: Float

                if (warmth <= 66f) {
                    red = 255f
                } else {
                    red = warmth - 60f
                    red = 329.69873f * red.pow(-0.13320476f)
                    red = red.coerceIn(0f, 255f)
                }

                if (warmth <= 66) {
                    green = ln(warmth) * 99.4708f
                    green -= 161.11957f
                    green = green.coerceIn(0f, 255f)
                } else {
                    green = warmth - 60f
                    green = 288.12216f * green.pow(-0.075514846f)
                    green = green.coerceIn(0f, 255f)
                }

                if (warmth <= 19f) {
                    blue = 0f
                } else {
                    blue = warmth
                    blue = 138.51773f * ln(blue) - 305.0448f

                    blue = max(blue, blue * (1.25f * slider))
                    blue = blue.coerceAtLeast(0f)
                }

                red /= 255f
                green /= 255f
                blue /= 255f

                val floatArray = floatArrayOf(
                    red, green, blue, 0f, 1f,
                    red, green, blue, 0f, 1f,
                    red, green, blue, 0f, 1f,
                    0f, 0f, 0f, 1f, 0f
                )

                warmthMatrix = floatArray

                warmthValue = sliderValue.floatValue
            }

            SelectedImageProperties.Highlights -> run {
                if (sliderValue.floatValue == highlightsValue) return@run

                val highlight = -sliderValue.floatValue

                val floatArray = floatArrayOf(
                    1 - highlight, 1f, 1f, 0f, 1f,
                    1f, 1 - highlight, 1f, 0f, 1f,
                    1f, 1f, 1 - highlight, 0f, 1f,
                    0f, 0f, 0f, 1f, 0f
                )

                highlightsMatrix = floatArray

                highlightsValue = sliderValue.floatValue
            }

            SelectedImageProperties.ColorTint -> run {
                if (sliderValue.floatValue == colorTintValue) {
                	return@run
               	}
				else if (sliderValue.floatValue == -1.2f) {
					colorTintMatrix1 = multiplicativeEmptyArray
					colorTintMatrix2 = additiveEmptyArray
					colorTintValue = sliderValue.floatValue

					return@run
				}

                val tint = (sliderValue.floatValue * 0.5f + 0.5f).coerceIn(0f, gradientColorList.size - 1f)

                val resolvedColor = getColorFromLinearGradientList(tint, gradientColorList)

                val floatArray = floatArrayOf(
                    0.6f, 1f, 1f, 0f, 1f,
                    1f, 0.6f, 1f, 0f, 1f,
                    1f, 1f, 0.6f, 0f, 1f,
                    0f, 0f, 0f, 1f, 0f
                )

                val floatArray2 = floatArrayOf(
                    0f, 0f, 0f, 0f, 0.2f * resolvedColor.red * 255f,
                    0f, 0f, 0f, 0f, 0.2f * resolvedColor.green * 255f,
                    0f, 0f, 0f, 0f, 0.2f * resolvedColor.blue * 255f,
                    0f, 0f, 0f, 0f, 0f
                )

                colorTintMatrix1 = floatArray
                colorTintMatrix2 = floatArray2

                colorTintValue = sliderValue.floatValue
            }

            else -> {}
        }
    }

    LazyRow(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxSize(1f)
    ) {
        item {
            EditingViewBottomAppBarItem(
                text = "Contrast",
                iconResId = R.drawable.contrast,
                selected = selectedProperty.value == SelectedImageProperties.Contrast
            ) {
                selectedProperty.value = SelectedImageProperties.Contrast

                sliderValue.floatValue = contrastValue
            }
        }

        item {
            EditingViewBottomAppBarItem(
                text = "Brightness",
                iconResId = R.drawable.palette,
                selected = selectedProperty.value == SelectedImageProperties.Brightness
            ) {
                selectedProperty.value = SelectedImageProperties.Brightness

                sliderValue.floatValue = brightnessValue
            }
        }

        item {
            EditingViewBottomAppBarItem(
                text = "Saturation",
                iconResId = R.drawable.resolution,
                selected = selectedProperty.value == SelectedImageProperties.Saturation
            ) {
                selectedProperty.value = SelectedImageProperties.Saturation

                sliderValue.floatValue = saturationValue
            }
        }

        item {
            EditingViewBottomAppBarItem(
                text = "Black Point",
                iconResId = R.drawable.file_is_selected_background,
                selected = selectedProperty.value == SelectedImageProperties.BlackPoint
            ) {
                selectedProperty.value = SelectedImageProperties.BlackPoint

                sliderValue.floatValue = blackPointValue
            }
        }

        item {
            EditingViewBottomAppBarItem(
                text = "White Point",
                iconResId = R.drawable.file_not_selected_background,
                selected = selectedProperty.value == SelectedImageProperties.WhitePoint
            ) {
                selectedProperty.value = SelectedImageProperties.WhitePoint

                sliderValue.floatValue = whitePointValue
            }
        }

        item {
            EditingViewBottomAppBarItem(
                text = "Shadows",
                iconResId = R.drawable.shadow,
                selected = selectedProperty.value == SelectedImageProperties.Shadows
            )
        }

        item {
            EditingViewBottomAppBarItem(
                text = "Warmth",
                iconResId = R.drawable.skillet,
                selected = selectedProperty.value == SelectedImageProperties.Warmth
            ) {
                selectedProperty.value = SelectedImageProperties.Warmth

                sliderValue.floatValue = warmthValue
            }
        }

        item {
            EditingViewBottomAppBarItem(
                text = "Color Tint",
                iconResId = R.drawable.colors,
                selected = selectedProperty.value == SelectedImageProperties.ColorTint
            ) {
                selectedProperty.value = SelectedImageProperties.ColorTint

                sliderValue.floatValue = colorTintValue
            }
        }

        item {
            EditingViewBottomAppBarItem(
                text = "Highlights",
                iconResId = R.drawable.highlights,
                selected = selectedProperty.value == SelectedImageProperties.Highlights
            ) {
                selectedProperty.value = SelectedImageProperties.Highlights

                sliderValue.floatValue = highlightsValue
            }
        }
    }
}

@Composable
fun FiltersTools(
	colorMatrix: MutableState<ColorMatrix>,
	currentFilter: MutableState<ColorMatrix>,
	changesSize: MutableIntState
) {
    LazyRow(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxSize(1f)
    ) {
        items(
        	count = ColorFiltersMatrices.keys.toList().size
        ) { index ->
        	val key = ColorFiltersMatrices.keys.toList()[index]
        	val matrix = ColorFiltersMatrices[key]

        	if (matrix != null) {
	            EditingViewBottomAppBarItem(text = key, iconResId = R.drawable.style, selected = currentFilter.value == matrix) {
	            	currentFilter.value = matrix

	            	changesSize.intValue += 1
	            }
        	}
        }
    }
}

fun ColorMatrix.multiplyOrSet(row: Int, column: Int, value: Float) : ColorMatrix {
	val local = this
	if (local[row, column] != 0f) {
		local[row, column] *= value
	} else {
		local[row, column] = value
	}

	return local
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
 * @param modifications a list of [Modification] which is all the new [DrawablePath]s or [DrawableText]s drawn
 * @param paint the paint to draw with
 * @param isDrawing is the user drawing right now? */
@Composable
private fun Modifier.makeDrawCanvas(
    allowedToDraw: State<Boolean>,
    modifications: SnapshotStateList<Modification>,
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

                        if (!canceled && (paint.value.type == PaintType.Pencil || paint.value.type == PaintType.Highlighter) && event.changes.size == 1) {
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
                        } else if (!canceled && paint.value.type == PaintType.Text && event.changes.size == 1 && event.calculateZoom() == 1f && event.calculateRotation() == 0f) {
                            when (event.type) {
                                PointerEventType.Press -> {
                                    val position = event.changes.first().position

                                    val tappedOnText =
                                        modifications.filterIsInstance<DrawableText>().firstOrNull {
                                            checkIfClickedOnText(
                                                text = it,
                                                clickPosition = position,
                                                extraPadding = if (it.paint.strokeWidth <= 40f) 60f else 0f
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
                        } else if (!canceled && paint.value.type == PaintType.Text && event.changes.size == 2 && event.calculateZoom() != 1f) {
                            when (event.type) {
                                PointerEventType.Press, PointerEventType.Move -> {
                                    val positionFirst = event.changes.first().position
                                    val positionSecond = event.changes[1].position

                                    val tappedOnText =
                                        modifications.filterIsInstance<DrawableText>().firstOrNull {
                                            val matchesFirstDown = checkIfClickedOnText(
                                                text = it,
                                                clickPosition = positionFirst,
                                                extraPadding = if (it.paint.strokeWidth <= 40f) 60f else 0f
                                            )

                                            val matchesSecondDown = checkIfClickedOnText(
                                                text = it,
                                                clickPosition = positionSecond,
                                                extraPadding = if (it.paint.strokeWidth <= 40f) 60f else 0f
                                            )

                                            matchesFirstDown && matchesSecondDown
                                        }

                                    tappedOnText?.let { text ->
                                        val zoom = event.calculateZoom()
                                        val rotation = event.calculateRotation()

                                        modifications.remove(text)

                                        // move topLeft of textbox to the text's position
                                        // basically de-centers the text so we can center it to that position with the new size
                                        val oldPosition = text.position + (text.size.toOffset() / 2f)
                                        val newWidth = text.paint.strokeWidth * zoom

                                        val textLayout = textMeasurer.measure(
                                            text = text.text,
                                            style = localTextStyle.copy(
                                                color = paint.value.color,
                                                fontSize = TextUnit(
                                                    newWidth,
                                                    TextUnitType.Sp
                                                ),
                                                textAlign = defaultTextStyle.textAlign,
                                                platformStyle = defaultTextStyle.platformStyle,
                                                lineHeightStyle = defaultTextStyle.lineHeightStyle,
                                                baselineShift = defaultTextStyle.baselineShift
                                            )
                                        )
                                        val zoomedText = text.copy(
                                            paint = text.paint.copy(
                                                strokeWidth = newWidth
                                            ),
                                            size = textLayout.size,
                                            position = oldPosition - (textLayout.size.toOffset() / 2f), // move from old topLeft to new center
                                            rotation = if (zoom != 1f) text.rotation + rotation else text.rotation
                                        )

                                        modifications.add(zoomedText)
                                    }

                                    isDrawing.value = true
                                    event.changes.forEach {
                                        it.consume()
                                    }
                                }

                                PointerEventType.Release -> {
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
    modifications: SnapshotStateList<Modification>,
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

fun checkIfClickedOnText(
    text: DrawableText,
    clickPosition: Offset,
    extraPadding: Float = 0f
): Boolean {
    val textPosition = text.position
    val textSize = text.size

    val textRight = textPosition.x + textSize.width
    val textLeft = textPosition.x
    val textTop = textPosition.y
    val textBottom = textPosition.y + textSize.height

    val isInTextWidth = clickPosition.x in textLeft - extraPadding..textRight + extraPadding
    val isInTextHeight = clickPosition.y in textTop - extraPadding..textBottom + extraPadding

    return isInTextWidth && isInTextHeight
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxWithConstraintsScope.DrawingControls(
    pagerState: PagerState,
    isDrawing: MutableState<Boolean>,
    paint: MutableState<ExtendedPaint>,
    modifications: SnapshotStateList<Modification>,
    changesSize: MutableIntState
) {
    val localConfig = LocalConfiguration.current
    var isLandscape by remember { mutableStateOf(localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) }

    LaunchedEffect(localConfig) {
        isLandscape = localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
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
): IntOffset {
    val maxXLimited = if (maxX <= minX) minX else maxX
    val maxYLimited = if (maxY <= minY) minY else maxY

    return IntOffset(this.x.coerceIn(minX, maxXLimited), this.y.coerceIn(minY, maxYLimited))
}

operator fun IntOffset.plus(offset: Offset): IntOffset =
    IntOffset(this.x + offset.x.toInt(), this.y + offset.y.toInt())
