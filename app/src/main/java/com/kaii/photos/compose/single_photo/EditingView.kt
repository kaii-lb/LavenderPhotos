package com.kaii.photos.compose.single_photo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.kaii.photos.R
import com.kaii.photos.compose.BottomAppBarItem
import com.kaii.photos.compose.ConfirmationDialog
import com.kaii.photos.compose.CustomMaterialTheme
import com.kaii.photos.helpers.brightenColor
import com.kaii.photos.helpers.darkenColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun EditingView(navController: NavHostController, uri: Uri) {
    val showCloseDialog = remember { mutableStateOf(false) }
    ConfirmationDialog(showDialog = showCloseDialog, dialogTitle = "Discard ongoing edits?", confirmButtonLabel = "Discard") {
        navController.popBackStack()
    }

    BackHandler {
        // TODO: check if saved last step
        showCloseDialog.value = true
    }

    val pagerState = rememberPagerState { 4 }

    Scaffold(
        topBar = {
            EditingViewTopBar(showCloseDialog)
        },
        bottomBar = {
            EditingViewBottomBar(
                pagerState
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
            var initialLoad by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(500)
                initialLoad = true
            }

            val animatedSize by animateFloatAsState(
                targetValue = if (pagerState.currentPage == 0 && initialLoad) 0.8f else 1f,
                label = "Animate size of preview image in crop mode"
            )

            GlideImage(
                model = uri,
                contentDescription = "Image to be edited",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .height(maxHeight * animatedSize)
                    .width(maxWidth * animatedSize)
                    .align(Alignment.Center)
            )

           AnimatedVisibility(
               visible = pagerState.currentPage == 3,
               enter = slideInHorizontally { width -> -width } + fadeIn(),
               exit = slideOutHorizontally { width -> -width } + fadeOut(),
               modifier = Modifier
					.fillMaxHeight(1f)
					.padding(0.dp, 16.dp),
           ) {
               var slideVal by remember { mutableFloatStateOf(0.25f) }

               Slider(
                   value = slideVal,
                   onValueChange = { newVal ->
                       slideVal = newVal
                       // paint.value = paint.value.copy(strokeWidth = slideVal)
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
				visible = pagerState.currentPage == 3,
				enter = slideInVertically { height -> height } + fadeIn(),
				exit = slideOutVertically { height -> height } + fadeOut(),
				modifier = Modifier
					.fillMaxWidth(1f)
					.align(Alignment.BottomCenter),
			) {
	   			var showColorPalette by remember { mutableStateOf(false) }
				val colorPaletteWidth by animateDpAsState(
					targetValue = if (showColorPalette) maxWidth - 24.dp else 0.dp, // - 24.dp to account for padding, stops the sudden width decrease
					animationSpec = tween(
						durationMillis = 350
					),
					label = "Animate color palette show/hide"
				)

				Box (
					modifier = Modifier
						.padding(16.dp, 4.dp)
				) {
					AnimatedVisibility(
						visible = !showColorPalette,
						enter = slideInVertically { height -> height } + fadeIn(),
						exit = slideOutVertically { height -> height } + fadeOut()
					) {
						Box (
							modifier = Modifier
								.fillMaxWidth(1f)
								.height(48.dp),
						) {
							Column (
								modifier = Modifier
									.height(40.dp)
									.width(64.dp)
									.clip(CircleShape)
									.background(CustomMaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
									.align(Alignment.CenterStart)
									.clickable {

									},
								verticalArrangement = Arrangement.Center,
								horizontalAlignment = Alignment.CenterHorizontally
							) {
								Text (
									text = "Clear",
									fontSize = TextUnit(14f, TextUnitType.Sp),
									color = CustomMaterialTheme.colorScheme.onSurface,
									modifier = Modifier
										.wrapContentSize()
								)
							}

							Column (
								modifier = Modifier
									.size(40.dp)
									.clip(CircleShape)
									.background(CustomMaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
									.align(Alignment.Center)
									.clickable {

									},
								verticalArrangement = Arrangement.Center,
								horizontalAlignment = Alignment.CenterHorizontally
							) {
								Icon (
									painter = painterResource(id = R.drawable.back_arrow),
									contentDescription = "undo last action",
									tint = CustomMaterialTheme.colorScheme.onSurface
								)
							}

							ColorIndicator(
								color = Color(red = 227, green = 83, blue = 53),
								modifier = Modifier
									.align(Alignment.CenterEnd)
							) {
								showColorPalette = true
							}
						}
					}

					Row (
						modifier = Modifier
							.height(48.dp)
							.width(colorPaletteWidth)
							.align(Alignment.CenterEnd),
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.SpaceAround
					) {
						ColorIndicator(color = Color.White) {
							showColorPalette = false
						}

						ColorIndicator(Color.Black) {
							showColorPalette = false
						}

						// poppy red
						ColorIndicator(Color(red = 227, green = 83, blue = 53)) {
							showColorPalette = false
						}

						// lemon yellow
						ColorIndicator(Color(red = 250, green = 250, blue = 51)) {
							showColorPalette = false
						}

						// emerald green
						ColorIndicator(Color(red = 80, green = 200, blue = 120)) {
							showColorPalette = false
						}

						// bright blue
						ColorIndicator(Color(red = 0, green = 150, blue = 255)) {
							showColorPalette = false
						}

						// orchid purple
						ColorIndicator(Color(red = 218, green = 112, blue = 214)) {
							showColorPalette = false
						}
					}
				}
			}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditingViewTopBar(showCloseDialog: MutableState<Boolean>) {
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
    pagerState: PagerState
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
                            CropTools()
                        }

                        1 -> {
                            AdjustTools()
                        }

                        2 -> {
                            FiltersTools()
                        }

                        3 -> {
                            DrawTools()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CropTools() {
    Row(
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
fun DrawTools() {
    Row(
        modifier = Modifier
            .fillMaxSize(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        EditingViewBottomAppBarItem(text = "Pencil", iconResId = R.drawable.pencil)

        EditingViewBottomAppBarItem(text = "Highlighter", iconResId = R.drawable.highlighter)

        EditingViewBottomAppBarItem(text = "Text", iconResId = R.drawable.text)
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
    val paint: Paint
)

private fun Paint.copy(
    color: Color = this.color,
    strokeCap: StrokeCap = this.strokeCap,
    strokeWidth: Float = this.strokeWidth,
    strokeJoin: StrokeJoin = this.strokeJoin,
    style: PaintingStyle = this.style,
    blendMode: BlendMode = this.blendMode,
    alpha: Float = this.alpha
): Paint {
    val paint = Paint()

    paint.color = color
    paint.strokeCap = strokeCap
    paint.strokeWidth = strokeWidth
    paint.strokeJoin = strokeJoin
    paint.style = style
    paint.blendMode = blendMode
    paint.alpha = alpha

    return paint
}

/** @param allowedToDraw no drawing happens if this is false
 * @param paint the paint to draw with
 * @param onDrag emits two values, a list of [PathWithPaint] which is all the new paths drawn, and a [Boolean] representing the dragging state */
@Composable
private fun Modifier.makeDrawCanvas(
    allowedToDraw: Boolean,
    paint: MutableState<Paint>,
    shouldClear: MutableState<Boolean>,
    onDrag: (List<PathWithPaint>, Boolean) -> Unit
) : Modifier {
    var currentPath by remember { mutableStateOf(Path()) }
    var lastPosition by remember { mutableStateOf(Offset.Unspecified) }

    val paths = remember { mutableStateListOf<PathWithPaint>() }

	LaunchedEffect(shouldClear.value) {
		if (shouldClear.value) {
			paths.clear()
			shouldClear.value = false
		}
	}

    val modifier = Modifier
        .pointerInput(Unit) {
            if (allowedToDraw) {
                detectTapGestures(
                    onTap = { offset ->
                        lastPosition = offset

                        currentPath.moveTo(lastPosition.x, lastPosition.y)
                        currentPath.lineTo(lastPosition.x + 1f, lastPosition.y + 1f)

                        paths.add(
                            PathWithPaint(
                                path = currentPath,
                                paint = paint.value
                            )
                        )

                        onDrag(paths, false)
                    }
                )
            }
        }
        .pointerInput(Unit) {
            if (allowedToDraw) {
//                 awaitEachGesture {
// 
//                 }
                detectDragGestures(
                    onDragStart = { offset ->
                        lastPosition = offset

                        currentPath.moveTo(lastPosition.x, lastPosition.y)

                        paths.add(
                            PathWithPaint(
                                path = currentPath,
                                paint = paint.value
                            )
                        )

                        onDrag(paths, true)
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

                        onDrag(paths, true)
                    },

                    onDragEnd = {
                        currentPath = Path()

                        onDrag(paths, false)
                    },

                    onDragCancel = {
                        currentPath = Path()

                        onDrag(paths, false)
                    }
                )
            }
        }

    return this.then(modifier)
}


@Composable
private fun ColorIndicator(
	color: Color,
	modifier: Modifier = Modifier,
	onClick: () -> Unit
) {
	Box (
		modifier = Modifier
			.size(40.dp)
			.clip(CircleShape)
			.background(CustomMaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
			.clickable {
				onClick()
			}
			.then(modifier)
	) {
		Box (
			modifier = Modifier
				.size(32.dp)
				.clip(CircleShape)
				.background(color)
				.align(Alignment.Center)
		)
	}
}
