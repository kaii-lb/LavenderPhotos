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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.NativePaint
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.DefaultStrokeLineMiter
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.DpSize
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
import com.kaii.photos.mediastore.MediaStoreData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import java.io.File
import java.io.FileOutputStream

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
    val paint = remember { mutableStateOf(
        ExtendedPaint().apply {
            label = "Pencil"
            strokeWidth = 20f
            strokeCap = StrokeCap.Round
            strokeJoin = StrokeJoin.Round
            strokeMiterLimit = DefaultStrokeLineMiter
            pathEffect = PathEffect.cornerPathEffect(50f)
            blendMode = BlendMode.SrcOver
            color = Color.Red
            alpha = 1f
        }
    )}

    val context = LocalContext.current
    val inputStream = remember { context.contentResolver.openInputStream(uri) }
    val image = remember { BitmapFactory.decodeStream(inputStream).asImageBitmap() }
    inputStream?.close()

    val rotationMultiplier = remember { mutableIntStateOf(0) }
    val rotation by animateFloatAsState(
        targetValue = -90f * rotationMultiplier.intValue,
        label = "Animate rotation"
    )

    Scaffold(
        topBar = {
            EditingViewTopBar(
                showCloseDialog = showCloseDialog
            ) {
            	val isVertical = rotation % 180f == 0f
                val size = IntSize(
                	if (isVertical) image.width else image.height,
                	if (isVertical) image.height else image.width
               	)
                val savedImage = ImageBitmap(size.width, size.height)
                val drawScope = CanvasDrawScope()

                drawScope.draw(
                    Density(1f),
                    LayoutDirection.Ltr,
                    androidx.compose.ui.graphics.Canvas(savedImage),
                    size.toSize()
                ) {
                    rotate(rotation) {
                        drawImage(
                            image = image
                        )

	                    paths.forEach { (path, paint) ->
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

				val original = File(absolutePath)

				// change the "edited at" thing to make more sense, like copy(1) copy(2) or something
				val newPath = original.absolutePath.replace(original.name, original.nameWithoutExtension + "-edited-at-" + System.currentTimeMillis() + "." + original.extension)

				val fileOutputStream = FileOutputStream(File(newPath))
                savedImage.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
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
            		val isVertical = rotation % 180f == 0f
	                val xRatio = if (isVertical) maxWidth.toPx() / image.width else maxHeight.toPx() / image.height
	                val yRatio = if (isVertical) maxHeight.toPx() / image.height else maxWidth.toPx() / image.width
	                val ratio = min(xRatio, yRatio)

	                IntSize((image.width * ratio).toInt(), (image.height * ratio).toInt())
				}
            }

            val size by remember { derivedStateOf {
            	IntSize(
            		(scaledSize.width * animatedSize).toInt(),
            		(scaledSize.height * animatedSize).toInt()
            	)
            }}

            val dpSize by remember{ derivedStateOf {
            	with(localDensity) {
	            	val width = size.width.toDp()
	            	val height = size.height.toDp()

	            	DpSize(width, height)
	            }
            }}

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

                paths.forEach { (path, paint) ->
                    drawPath(
                        path = path.apply {
                            transform(
                                Matrix().apply {
                                    scale(animatedSize, animatedSize, 1f)
                                }
                            )
                        },
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
       		paint.value = paint.value.copy(
       			pathEffect = PathEffect.cornerPathEffect(50f),
       			strokeCap = StrokeCap.Round,
       			strokeJoin = StrokeJoin.Round,
       			blendMode = BlendMode.SrcOver,
                label = "Pencil",
       			alpha = 1f
       		)
       	}

        EditingViewBottomAppBarItem(
        	text = "Highlighter",
        	iconResId = R.drawable.highlighter,
        	selected = paint.value.label == "Highlighter"
       	) {
       		paint.value = paint.value.copy(
       			pathEffect = null,
       			strokeCap = StrokeCap.Square,
       			strokeJoin = StrokeJoin.Miter,
       			blendMode = BlendMode.SrcOver,
                label = "Highlighter",
       			alpha = 0.5f
       		)
       	}

        EditingViewBottomAppBarItem(
        	text = "Text",
        	iconResId = R.drawable.text,
        	selected = paint.value.label == "Text"
       	) {
       		paint.value = paint.value.copy(
       			pathEffect = PathEffect.cornerPathEffect(50f),
       			strokeCap = StrokeCap.Round,
       			strokeJoin = StrokeJoin.Round,
       			blendMode = BlendMode.SrcOver,
                label = "Text",
       			alpha = 1f
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

data class PathWithPaint(
    val path: Path,
    val paint: ExtendedPaint
)

class ExtendedPaint(
    override var alpha: Float = 1.0f,
    override var blendMode: BlendMode = BlendMode.SrcOver,
    override var color: Color = DrawingColors.Black,
    override var colorFilter: ColorFilter? = null,
    override var filterQuality: FilterQuality = FilterQuality.Low,
    override var isAntiAlias: Boolean = true,
    override var pathEffect: PathEffect? = null,
    override var shader: Shader? = null,
    override var strokeCap: StrokeCap = StrokeCap.Round,
    override var strokeJoin: StrokeJoin = StrokeJoin.Round,
    override var strokeMiterLimit: Float = DefaultStrokeLineMiter,
    override var strokeWidth: Float = 20f,
    override var style: PaintingStyle = PaintingStyle.Stroke,
    var label: String = "",
) : Paint {
    fun copy(
        color: Color = this.color,
        strokeCap: StrokeCap = this.strokeCap,
        strokeWidth: Float = this.strokeWidth,
        strokeJoin: StrokeJoin = this.strokeJoin,
        style: PaintingStyle = this.style,
        blendMode: BlendMode = this.blendMode,
        alpha: Float = this.alpha,
        pathEffect: PathEffect? = this.pathEffect,
        label: String = this.label,
        filterQuality: FilterQuality = this.filterQuality,
        isAntiAlias: Boolean = this.isAntiAlias,
        strokeMiterLimit: Float = this.strokeMiterLimit,
        shader: Shader? = this.shader,
        colorFilter: ColorFilter? = this.colorFilter
    ) = ExtendedPaint().also { paint ->
        paint.color = color
        paint.strokeCap = strokeCap
        paint.strokeWidth = strokeWidth
        paint.strokeJoin = strokeJoin
        paint.style = style
        paint.blendMode = blendMode
        paint.alpha = alpha
        paint.pathEffect = pathEffect
        paint.filterQuality = filterQuality
        paint.isAntiAlias = isAntiAlias
        paint.strokeMiterLimit = strokeMiterLimit
        paint.shader = shader
        paint.colorFilter = colorFilter
        paint.label = label
    }

    override fun asFrameworkPaint(): NativePaint {
        return Paint().also { paint ->
            paint.color = color
            paint.strokeCap = strokeCap
            paint.strokeWidth = strokeWidth
            paint.strokeJoin = strokeJoin
            paint.style = style
            paint.blendMode = blendMode
            paint.alpha = alpha
            paint.pathEffect = pathEffect
            paint.filterQuality = filterQuality
            paint.isAntiAlias = isAntiAlias
            paint.strokeMiterLimit = strokeMiterLimit
            paint.shader = shader
            paint.colorFilter = colorFilter
        }.asFrameworkPaint()
    }
}


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
