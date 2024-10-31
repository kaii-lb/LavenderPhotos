package com.kaii.photos.compose.single_photo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import com.kaii.photos.R
import com.kaii.photos.compose.BottomAppBarItem
import com.kaii.photos.compose.ConfirmationDialog
import com.kaii.photos.helpers.ColorIndicator
import com.kaii.photos.helpers.CustomMaterialTheme
import com.kaii.photos.helpers.DrawingColors
import com.kaii.photos.helpers.DrawingPaints
import com.kaii.photos.helpers.ExtendedPaint
import com.kaii.photos.helpers.PathWithPaint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditingView(navController: NavHostController, absolutePath: String, uri: Uri) {
    val showCloseDialog = remember { mutableStateOf(false) }

    ConfirmationDialog(
        showDialog = showCloseDialog,
        dialogTitle = "Discard ongoing edits?",
        confirmButtonLabel = "Discard"
    ) {
        navController.popBackStack()
    }

    BackHandler {
        // TODO: check if saved last step
        showCloseDialog.value = true
    }

    val pagerState = rememberPagerState { 4 }

    val paths = remember { mutableStateListOf<PathWithPaint>() }
    val paint = remember { mutableStateOf(DrawingPaints.Pencil) }

    val context = LocalContext.current
    val inputStream = remember { context.contentResolver.openInputStream(uri) }
    val image = remember { BitmapFactory.decodeStream(inputStream).asImageBitmap() }
    inputStream?.close()

    val rotationMultiplier = remember { mutableIntStateOf(0) }
    val rotation by animateFloatAsState(
        targetValue = -90f * rotationMultiplier.intValue,
        label = "Animate rotation"
    )

    var maxSize by remember { mutableStateOf(Size.Unspecified) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            EditingViewTopBar(
                showCloseDialog = showCloseDialog
            ) {
                coroutineScope.launch(Dispatchers.IO) {
					val pathList = emptyList<PathWithPaint>().toMutableList()
					paths.forEach {
						pathList.add(it)
					}

					val rotationMatrix = android.graphics.Matrix().apply {
						postRotate(rotation)
					}

                    val savedImage = Bitmap.createBitmap(image.asAndroidBitmap(), 0, 0, image.width, image.height)
                    					.copy(Bitmap.Config.ARGB_8888, true).asImageBitmap()
                    val size = IntSize(
                        savedImage.width,
                        savedImage.height
                    )

                    val ratio = 1 / min(maxSize.width / size.width, maxSize.height / size.height)
                    val drawScope = CanvasDrawScope()
					val canvas = androidx.compose.ui.graphics.Canvas(savedImage)

                    drawScope.draw(
                        Density(1f),
                        LayoutDirection.Ltr,
                        canvas,
                        size.toSize()
                    ) {
						pathList.toList().forEach { (path, paint) ->
							scale(ratio, Offset(0.5f, 0.5f)) {
								drawPath(
									path = path,
									style = Stroke(
										width = paint.strokeWidth,
										cap = paint.strokeCap,
										join = paint.strokeJoin,
										miter = paint.strokeMiterLimit,
										pathEffect = paint.pathEffect
									),
									blendMode = paint.blendMode,
									color = paint.color,
									alpha = paint.alpha
								)
							}
						}
                    }

					val rotatedImage = Bitmap.createBitmap(savedImage.asAndroidBitmap(), 0, 0, image.width, image.height, rotationMatrix, false)
                    					.copy(Bitmap.Config.ARGB_8888, true).asImageBitmap()

                    val original = File(absolutePath)
                    // change the "edited at" thing to make more sense, like copy(1) copy(2) or something
                    val newPath = original.absolutePath.replace(
                        original.name,
                        original.nameWithoutExtension + "-edited-at-" + System.currentTimeMillis() + ".png"
                    )

                    val fileOutputStream = FileOutputStream(File(newPath))
                    rotatedImage.asAndroidBitmap()
                        .compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                    fileOutputStream.close()
                }
            }
        },
        bottomBar = {
            EditingViewBottomBar(
                pagerState = pagerState,
                paint = paint,
                rotationMultiplier = rotationMultiplier
            )
        },
        modifier = Modifier
            .fillMaxSize(1f)
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(1f)
        ) {
            val shouldClearPaths = remember { mutableStateOf(false) }
            val isDrawing = remember { mutableStateOf(false) }

            var initialLoad by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(500)
                initialLoad = true
            }

            val animatedSize by animateFloatAsState(
                targetValue = if (pagerState.currentPage == 0 && initialLoad) 0.8f else 1f,
                label = "Animate size of preview image in crop mode"
            )

            val localDensity = LocalDensity.current

            val scaledSize = remember {
                with(localDensity) {
                    val xRatio = maxWidth.toPx() / image.width
                    val yRatio = maxHeight.toPx() / image.height
                    val ratio = min(xRatio, yRatio)

                    maxSize = Size(maxWidth.toPx(), maxHeight.toPx())

                    IntSize((image.width * ratio).toInt(), (image.height * ratio).toInt())
                }
            }

            val size by remember {
                derivedStateOf {
                    IntSize(
                        (scaledSize.width * animatedSize).toInt(),
                        (scaledSize.height * animatedSize).toInt()
                    )
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

            Canvas(
                modifier = Modifier
                    .size(dpSize)
                    .align(Alignment.Center)
                    .rotate(
                        if (rotation == 360f) {
                            rotationMultiplier.intValue = 0
                            0f
                        } else {
                            rotation
                        }
                    )
                    .clipToBounds()
                    .makeDrawCanvas(
                        allowedToDraw = pagerState.currentPage == 3,
                        paths = paths,
                        paint = paint,
                        isDrawing = isDrawing
                    )
            ) {
                drawImage(
                    image = image,
                    dstSize = size
                )

                scale(animatedSize) {
                    paths.forEach { (path, paint) ->
                        val center = path.getBounds().centerLeft

                        translate(
                            center.x * animatedSize / center.x,
                            center.y * animatedSize / center.y
                        ) {
                            drawPath(
                                path = path,
                                style = Stroke(
                                    width = paint.strokeWidth,
                                    cap = paint.strokeCap,
                                    join = paint.strokeJoin,
                                    miter = paint.strokeMiterLimit,
                                    pathEffect = paint.pathEffect
                                ),
                                blendMode = paint.blendMode,
                                color = paint.color,
                                alpha = paint.alpha
                            )
                        }
                    }
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
                        delay(1500)
                        shouldShowDrawOptions = true
                    }
                }
                lastPage = pagerState.currentPage
            }

            AnimatedVisibility(
                visible = pagerState.currentPage == 3 && shouldShowDrawOptions,
                enter = slideInHorizontally { width -> -width } + fadeIn(),
                exit = slideOutHorizontally { width -> -width } + fadeOut(),
                modifier = Modifier
                    .fillMaxHeight(1f)
                    .padding(0.dp, 16.dp),
            ) {
                var slideVal by remember { mutableFloatStateOf(paint.value.strokeWidth / 128f) }

                Slider(
                    value = slideVal,
                    onValueChange = { newVal ->
                        slideVal = newVal
                        paint.value = paint.value.copy(strokeWidth = newVal * 128)
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
                            translationX = 50f
                            transformOrigin = TransformOrigin(0f, 0f)
                        }
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(
                                Constraints(
                                    minWidth = constraints.minHeight / 2,
                                    minHeight = constraints.minWidth,
                                    maxWidth = constraints.maxHeight / 2,
                                    maxHeight = constraints.maxWidth
                                )
                            )

                            layout(placeable.height, placeable.width) {
                                placeable.place(-placeable.width, -placeable.height / 2)
                            }
                        }
                )
            }

            AnimatedVisibility(
                visible = pagerState.currentPage == 3 && shouldShowDrawOptions,
                enter = slideInVertically { height -> height } + fadeIn(),
                exit = slideOutVertically { height -> height } + fadeOut(),
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .align(Alignment.BottomCenter),
            ) {
                DrawActionsAndColors(paths, shouldClearPaths, paint)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditingViewTopBar(
    showCloseDialog: MutableState<Boolean>,
    saveImage: () -> Unit
) {
    TopAppBar(
        navigationIcon = {
            Column(
                modifier = Modifier
                    .height(40.dp)
                    .width(56.dp)
                    .clip(CircleShape)
                    .background(CustomMaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        showCloseDialog.value = true
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
        },
        actions = {
            Column(
                modifier = Modifier
                    .height(40.dp)
                    .width(96.dp)
                    .clip(CircleShape)
                    .background(CustomMaterialTheme.colorScheme.primary)
                    .clickable {
                        saveImage()
                    },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Save Copy",
                    fontSize = TextUnit(14f, TextUnitType.Sp),
                    color = CustomMaterialTheme.colorScheme.onPrimary
                )
            }
        },
        title = {},
        modifier = Modifier
            .fillMaxWidth(1f)
            .padding(8.dp, 0.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditingViewBottomBar(
    pagerState: PagerState,
    paint: MutableState<ExtendedPaint>,
    rotationMultiplier: MutableIntState
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(1f)
            .height(160.dp),
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
                                rotationMultiplier = rotationMultiplier
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

@Composable
fun CropTools(
    rotationMultiplier: MutableIntState
) {
    Row(
        modifier = Modifier
            .fillMaxSize(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        EditingViewBottomAppBarItem(text = "Rotate", iconResId = R.drawable.rotate_ccw) {
            rotationMultiplier.intValue += 1
        }

        EditingViewBottomAppBarItem(text = "Ratio", iconResId = R.drawable.resolution) {

        }

        EditingViewBottomAppBarItem(text = "Reset", iconResId = R.drawable.reset) {
            rotationMultiplier.intValue = 0
        }
    }
}

@Composable
fun AdjustTools() {
    LazyRow(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
            selected = paint.value.label == "Pencil"
        ) {
            paint.value = DrawingPaints.Pencil.copy(
                strokeWidth = paint.value.strokeWidth,
                color = paint.value.color
            )
        }

        EditingViewBottomAppBarItem(
            text = "Highlighter",
            iconResId = R.drawable.highlighter,
            selected = paint.value.label == "Highlighter"
        ) {
            paint.value = DrawingPaints.Highlighter.copy(
                strokeWidth = paint.value.strokeWidth,
                color = paint.value.color
            )
        }

        EditingViewBottomAppBarItem(
            text = "Text",
            iconResId = R.drawable.text,
            selected = paint.value.label == "Text"
        ) {
            paint.value = DrawingPaints.Pencil.copy(
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
 * @param paths a list of [PathWithPaint] which is all the new paths drawn
 * @param paint the paint to draw with
 * @param isDrawing is the user drawing right now? */
@Composable
private fun Modifier.makeDrawCanvas(
    allowedToDraw: Boolean,
    paths: SnapshotStateList<PathWithPaint>,
    paint: MutableState<ExtendedPaint>,
    isDrawing: MutableState<Boolean>
): Modifier {
    val modifier = Modifier
        .pointerInput(Unit) {
            if (allowedToDraw) {
                awaitEachGesture {
                    var lastPoint = Offset.Unspecified

                    do {
                        val event = awaitPointerEvent()
                        val canceled = event.changes.any { it.isConsumed }

                        if (!canceled) {
                            when (event.type) {
                                PointerEventType.Press -> {
                                    val offset = event.changes.first().position

                                    paths.add(
                                        PathWithPaint(
                                            Path().apply {
                                                moveTo(offset.x, offset.y)
                                            },
                                            paint.value
                                        )
                                    )

                                    lastPoint = offset
                                    isDrawing.value = true

                                    event.changes.forEach {
                                        it.consume()
                                    }
                                }

                                PointerEventType.Move -> {
                                    val offset = event.changes.first().position
                                    var path = paths.lastOrNull()?.path

                                    if (path == null) {
                                        paths.add(
                                            PathWithPaint(
                                                Path().apply {
                                                    moveTo(offset.x, offset.y)
                                                },
                                                paint.value
                                            )
                                        )
                                        path = paths.last().path
                                    } else {
                                        paths.remove(
                                            PathWithPaint(
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

                                    paths.add(
                                        PathWithPaint(
                                            path,
                                            paint.value
                                        )
                                    )

                                    lastPoint = offset
                                    isDrawing.value = true

                                    event.changes.forEach {
                                        it.consume()
                                    }
                                }

                                PointerEventType.Release -> {
                                    val offset = event.changes.first().position
                                    val path = paths.last().path

                                    path.lineTo(offset.x, offset.y)
                                    lastPoint = offset

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
    paths: SnapshotStateList<PathWithPaint>,
    shouldClearPaths: MutableState<Boolean>,
    paint: MutableState<ExtendedPaint>
) {
    var showColorPalette by remember { mutableStateOf(false) }
    val colorPaletteWidth by animateDpAsState(
        targetValue = if (showColorPalette) maxWidth - 24.dp else 0.dp, // - 24.dp to account for padding, stops the sudden width decrease
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
                    .height(48.dp),
            ) {
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
                            shouldClearPaths.value = true
                            paths.clear()
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
                            paths.removeLastOrNull()
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
                .align(Alignment.CenterEnd),
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
