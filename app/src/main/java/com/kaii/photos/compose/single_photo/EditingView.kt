package com.kaii.photos.compose.single_photo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.kaii.photos.R
import com.kaii.photos.compose.BottomAppBarItem
import com.kaii.photos.compose.CustomMaterialTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditingView(uri: Uri, showCloseDialog: MutableState<Boolean> = remember { mutableStateOf(false) }) {
    BackHandler (
        enabled = !showCloseDialog.value
    ) {
        // TODO: check if saved last step
        showCloseDialog.value = false
    }

    val pagerState = rememberPagerState { 4 }

	val showDrawSizeSelector = remember { mutableStateOf(false) }
	val paths = remember { mutableStateListOf<PathWithPaint>() }

    val paint = remember { mutableStateOf(
        Paint().apply {
            color = Color.Red
            strokeCap = StrokeCap.Round
            strokeWidth = 20f
            strokeJoin = StrokeJoin.Round
            style = PaintingStyle.Stroke
            blendMode = BlendMode.SrcOver
        }
    )}

    Scaffold (
        topBar = {
        	EditingViewTopBar(showCloseDialog)
        },
        bottomBar = {
            EditingViewBottomBar(
                pagerState,
                paint,
                showDrawSizeSelector,
                paths
            )
        },
        modifier = Modifier
            .fillMaxSize(1f)
    ) { innerPadding ->
        BoxWithConstraints (
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(1f)
        ) {
        	var initialLoad by remember { mutableStateOf(false) }
			LaunchedEffect(Unit) {
				delay(500)
				initialLoad = true
			}

            val animatedSize by animateFloatAsState(
            	targetValue = if (pagerState.currentPage == 0 && initialLoad) 0.8f else 1f,
                label = "Animate size of preview image in crop mode"
            )

            val inputStream = LocalContext.current.contentResolver.openInputStream(uri)
            val image = BitmapFactory.decodeStream(inputStream).copy(Bitmap.Config.ARGB_8888, true).asImageBitmap()
			inputStream?.close()

            var lastPosition by remember { mutableStateOf(Offset.Unspecified) }
            var currentPath by remember { mutableStateOf(Path()) }

           	val neededSize = with(LocalDensity.current) {
                val width = maxWidth.toPx() / image.width
                val height = maxHeight.toPx() / image.height
                val min = min(width, height)

           		IntSize((image.width * min).toInt(), (image.height * min).toInt())
           	}

			val neededHeightDp = with(LocalDensity.current) {
				(neededSize.height / density).dp
			}
			val neededWidthDp = with(LocalDensity.current) {
				(neededSize.width / density).dp
			}

            Canvas (
                modifier = Modifier
                    .height(neededHeightDp * animatedSize)
                    .width(neededWidthDp * animatedSize)
                    .align(Alignment.Center)
                    .clipToBounds()
                    .background(Color.Blue)
                    .pointerInput(Unit) {
                        if (pagerState.currentPage == 3) {
                            detectDragGestures(
                                onDragStart = { startOffset ->
                                    currentPath.moveTo(startOffset.x, startOffset.y)
                                    paths.add(
                                        PathWithPaint(
                                            currentPath,
                                            paint.value
                                        )
                                    )
                                    lastPosition = startOffset
                                },

                                onDrag = { change, _ ->
                                    paths.remove(
                                        paths.find { path ->
                                            path ==
                                                PathWithPaint(
                                                    currentPath,
                                                    paint.value
                                                )
                                        }
                                    )
                                    currentPath.quadraticTo(
                                        lastPosition.x,
                                        lastPosition.y,
                                        (lastPosition.x + change.position.x) / 2,
                                        (lastPosition.y + change.position.y) / 2
                                    )
                                    paths.add(
                                        PathWithPaint(
                                            currentPath,
                                            paint.value
                                        )
                                    )
                                    lastPosition = change.position
                                },

                                onDragEnd = {
                                    currentPath.lineTo(lastPosition.x, lastPosition.y)
                                    currentPath = Path()
                                },

                                onDragCancel = {
                                    currentPath.lineTo(lastPosition.x, lastPosition.y)
                                    currentPath = Path()
                                }
                            )
                        }
                    }
            ) {
                drawImage(image, dstSize = neededSize)

                paths.forEach { pathWithPaint ->
                    drawPath(
                        path = pathWithPaint.path,
                        style = Stroke(width = pathWithPaint.paint.strokeWidth, cap = pathWithPaint.paint.strokeCap, join = pathWithPaint.paint.strokeJoin),
                        blendMode = pathWithPaint.paint.blendMode,
                        color = pathWithPaint.paint.color
                    )
                }
            }

            if (showDrawSizeSelector.value) {
                var slideVal by remember { mutableFloatStateOf(0f) }
                val density = LocalDensity.current
                
                Slider (
                    value = slideVal,
                    onValueChange = { newVal ->
                        slideVal = (newVal / 4f).roundToInt() * 4f
                        paint.value = Paint().apply {
                            color = paint.value.color
                            strokeCap = paint.value.strokeCap
                            strokeWidth = slideVal
                            style = paint.value.style
                            blendMode = paint.value.blendMode
                            alpha = paint.value.alpha
                        }
                    },
                    steps = 16,
                    valueRange = 8f..64f,
                    thumb = {
                        Box (
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(CustomMaterialTheme.colorScheme.primary)
                        )
                    },
                    modifier = Modifier
                    	.fillMaxWidth(1f)
                        .graphicsLayer {
                            rotationZ = 90f
                            translationX = 50f
                            translationY = with(density) { maxHeight.toPx() / 4 }
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
                                placeable.place(0, -placeable.height / 2)
                            }
                        }
                        .width(256.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditingViewTopBar(showCloseDialog: MutableState<Boolean>) {
    TopAppBar (
    	colors = TopAppBarDefaults.topAppBarColors(
			containerColor = CustomMaterialTheme.colorScheme.surfaceContainer
		),
        navigationIcon = {
            Column(
                modifier = Modifier
                    .height(40.dp)
                    .width(64.dp)
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
            Column	(
                modifier = Modifier
                    .height(40.dp)
                    .width(96.dp)
                    .clip(CircleShape)
                    .background(CustomMaterialTheme.colorScheme.primary)
                    .clickable {

                    },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text (
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
    paint: MutableState<Paint>,
    showDrawSizeSelector: MutableState<Boolean>,
    paths: SnapshotStateList<PathWithPaint>
) {
	Column (
		modifier = Modifier
			.fillMaxWidth(1f)
			.height(if (showDrawSizeSelector.value) 208.dp else 160.dp),
		verticalArrangement = Arrangement.Top,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		if (showDrawSizeSelector.value) {
		    Box (
		       modifier = Modifier
		           .fillMaxWidth(1f)
		           .height(48.dp)
		           .padding(8.dp, 0.dp, 16.dp, 0.dp)
		    ) {
		        Column	(
		            modifier = Modifier
		                .height(40.dp)
		                .width(64.dp)
		                .clip(CircleShape)
		                .background(CustomMaterialTheme.colorScheme.surfaceVariant)
		                .clickable {
		                    paths.clear()
		                }
		                .align(Alignment.CenterStart),
		            verticalArrangement = Arrangement.Center,
		            horizontalAlignment = Alignment.CenterHorizontally
		        ) {
		            Text (
		                text = "Clear",
		                fontSize = TextUnit(14f, TextUnitType.Sp),
		                color = CustomMaterialTheme.colorScheme.onSurface
		            )
		        }

		        Row (
		            modifier = Modifier
		                .wrapContentSize()
		                .align(Alignment.Center)
		        ) {
		            var lastRemoved: PathWithPaint? by remember { mutableStateOf(null) }

		            Column	(
		                modifier = Modifier
		                    .size(40.dp)
		                    .clip(CircleShape)
		                    .background(CustomMaterialTheme.colorScheme.surfaceVariant)
		                    .clickable {
		                        if (paths.size > 0) {
		                            lastRemoved = paths.last()
		                            paths.remove(lastRemoved)
		                        }
		                    },
		                verticalArrangement = Arrangement.Center,
		                horizontalAlignment = Alignment.CenterHorizontally
		            ) {
		                Text (
		                    text = "<",
		                    fontSize = TextUnit(20f, TextUnitType.Sp),
		                    color = CustomMaterialTheme.colorScheme.onSurface
		                )
		            }

					Spacer (modifier = Modifier.width(8.dp))

		            Column	(
		                modifier = Modifier
		                    .size(40.dp)
		                    .clip(CircleShape)
		                    .background(CustomMaterialTheme.colorScheme.surfaceVariant)
		                    .clickable {
		                        if (lastRemoved != null && !paths.contains(lastRemoved!!)) paths.add(lastRemoved!!)
		                    },
		                verticalArrangement = Arrangement.Center,
		                horizontalAlignment = Alignment.CenterHorizontally
		            ) {
		                Text (
		                    text = ">",
		                    fontSize = TextUnit(20f, TextUnitType.Sp),
		                    color = CustomMaterialTheme.colorScheme.onSurface
		                )
		            }
		        }
		        Box	(
		            modifier = Modifier
		                .size(40.dp)
		                .clip(CircleShape)
		                .background(CustomMaterialTheme.colorScheme.primary)
		                .clickable {
		                    // TODO: show color menu, make it take up this whole row, animatedly ofc
		                }
		                .align(Alignment.CenterEnd),
		        ) {
		            Box (
		                modifier = Modifier
		                    .size(32.dp)
		                    .clip(CircleShape)
		                    .background(Color.Red)
		                    .align(Alignment.Center)
		            )
		        }
	        }
	    }

	    Spacer (modifier = Modifier.height(8.dp))

	    BottomAppBar (
	    	containerColor = CustomMaterialTheme.colorScheme.surfaceContainer,
	    	contentColor = CustomMaterialTheme.colorScheme.onSurface,
	        modifier = Modifier
	            .fillMaxWidth(1f)
	            .height(160.dp)
	    ) {
	        val coroutineScope = rememberCoroutineScope()

	        Column (
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
	                        Box (
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
	                                if (delta > 0) {
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
	                        showDrawSizeSelector.value = false
	                        CropTools()
	                    }

	                    1 -> {
	                        showDrawSizeSelector.value = false
	                        AdjustTools()
	                    }

	                    2 -> {
	                        showDrawSizeSelector.value = false
	                        FiltersTools()
	                    }

	                    3 -> {
	                        showDrawSizeSelector.value = true
	                        DrawTools(paint)
	                    }
	                }
	            }
	        }
	    }
	}
}

@Composable
fun CropTools() {
    Row (
       modifier = Modifier
           .fillMaxSize(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        EditingViewBottomAppBarItem(text = "Rotate", iconResId = R.drawable.rotate_ccw)
        EditingViewBottomAppBarItem(text = "Ratio", iconResId = R.drawable.resolution)
        EditingViewBottomAppBarItem(text = "Reset", iconResId = R.drawable.reset)
    }
}

@Composable
fun AdjustTools() {
    LazyRow (
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
            EditingViewBottomAppBarItem(text = "Black Point", iconResId = R.drawable.file_is_selected_background)
        }

        item {
            EditingViewBottomAppBarItem(text = "White Point", iconResId = R.drawable.file_not_selected_background)
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
    LazyRow (
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
fun DrawTools(paint: MutableState<Paint>) {
    Row (
        modifier = Modifier
            .fillMaxSize(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        EditingViewBottomAppBarItem(text = "Pencil", iconResId = R.drawable.pencil) {
			paint.value = Paint().apply {
				color = paint.value.color
				strokeCap = StrokeCap.Round
				strokeWidth = paint.value.strokeWidth
                strokeJoin = StrokeJoin.Round
				style = paint.value.style
				blendMode = BlendMode.SrcOver
				alpha = 1f
			}
        }

        EditingViewBottomAppBarItem(text = "Highlighter", iconResId = R.drawable.highlighter) {
			paint.value = Paint().apply {
				color = paint.value.color
				strokeCap = StrokeCap.Butt
				strokeWidth = paint.value.strokeWidth
                strokeJoin = StrokeJoin.Miter
				style = paint.value.style
				blendMode = BlendMode.Color
				alpha = 0.8f
			}
        }

        EditingViewBottomAppBarItem(text = "Text", iconResId = R.drawable.text) {
            // TODO: do text stuff
        }
    }
}

@Composable
fun EditingViewBottomAppBarItem(
	text: String,
	iconResId: Int,
    onClick: (() -> Unit)? = null
) {
	BottomAppBarItem(
       	text = text,
       	iconResId = iconResId,
       	buttonWidth = 84.dp,
       	buttonHeight = 56.dp,
       	cornerRadius = 8.dp,
        action = onClick
	)
}

@OptIn(ExperimentalMaterial3Api::class)
val NoRippleConfiguration = RippleConfiguration(
    color = Color.Transparent,
    rippleAlpha = RippleAlpha(0f, 0f, 0f, 0f)
)

data class PathWithPaint (
    val path: Path,
    val paint: Paint
)
