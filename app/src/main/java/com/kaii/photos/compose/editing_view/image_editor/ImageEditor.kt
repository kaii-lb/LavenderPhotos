package com.kaii.photos.compose.editing_view.image_editor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.core.graphics.scale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.ImageEditorBottomBar
import com.kaii.photos.compose.app_bars.ImageEditorTopBar
import com.kaii.photos.compose.dialogs.TextEntryDialog
import com.kaii.photos.compose.editing_view.CropBox
import com.kaii.photos.compose.editing_view.ImageFilterPage
import com.kaii.photos.compose.editing_view.PreviewCanvas
import com.kaii.photos.compose.editing_view.makeVideoDrawCanvas
import com.kaii.photos.compose.widgets.shimmerEffect
import com.kaii.photos.datastore.Editing
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.editing.DrawableText
import com.kaii.photos.helpers.editing.ImageEditorTabs
import com.kaii.photos.helpers.editing.ImageModification
import com.kaii.photos.helpers.editing.MediaAdjustments
import com.kaii.photos.helpers.editing.MediaColorFilters
import com.kaii.photos.helpers.editing.rememberDrawingPaintState
import com.kaii.photos.helpers.editing.rememberImageEditingState
import com.kaii.photos.helpers.editing.saveImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ImageEditor(
    uri: Uri,
    absolutePath: String,
    isFromOpenWithView: Boolean
) {
    val imageEditingState = rememberImageEditingState()
    val drawingPaintState = rememberDrawingPaintState(
        isVideo = false
    )

    val lastSavedModCount = remember { mutableIntStateOf(0) }
    val totalModCount = remember { mutableIntStateOf(0) }
    val modifications = remember { mutableStateListOf<ImageModification>() }

    val coroutineScope = rememberCoroutineScope()
    val filterPagerState = rememberPagerState(
        initialPage = MediaColorFilters.entries.indexOf(
            (modifications.lastOrNull {
                it is ImageModification.Filter
            } as? ImageModification.Filter)?.type ?: MediaColorFilters.None
        )
    ) { MediaColorFilters.entries.size }

    val pagerState = rememberPagerState { ImageEditorTabs.entries.size }

    var originalImage by remember { mutableStateOf(ImageBitmap(512, 512)) }
    var moddedImage by remember { mutableStateOf(originalImage) }

    val windowInfo = LocalWindowInfo.current
    val context = LocalContext.current
    LaunchedEffect(uri, absolutePath) {
        withContext(Dispatchers.IO) {
            val bitmap = if (absolutePath.endsWith(".avif")) {  // avif won't load on some android distros, so use glide for that
                Glide.with(context)
                    .asBitmap()
                    .load(uri)
                    .submit()
                    .get()
            } else {
                context.contentResolver.openInputStream(uri).use { inputStream ->
                    BitmapFactory.decodeStream(inputStream).also {
                        inputStream?.close()
                    }
                }
            }

            var inSampleSize = 1

            val reqWidth = windowInfo.containerSize.width
            val reqHeight = windowInfo.containerSize.height

            if (bitmap.height > reqHeight || bitmap.width > reqWidth) {
                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while (bitmap.height / inSampleSize >= reqHeight || bitmap.width / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            originalImage = bitmap
                .scale(
                    width = (bitmap.width.toFloat() / inSampleSize).toInt(),
                    height = (bitmap.height.toFloat() / inSampleSize).toInt(),
                    filter = true
                )
                .asImageBitmap()

            moddedImage = originalImage
        }
    }

    LaunchedEffect(originalImage, totalModCount.intValue) {
        withContext(Dispatchers.IO) {
            val androidBitmap = originalImage.asAndroidBitmap()
            val bitmap = androidBitmap.copy(Bitmap.Config.ARGB_8888, true).asImageBitmap()
            val canvas = Canvas(bitmap)
            val drawScope = CanvasDrawScope()

            val mods = modifications + drawingPaintState.modifications
            val sorted = mods.sortedBy { mod ->
                if (mod is ImageModification.Adjustment) {
                    MediaAdjustments.entries.indexOf(mod.type)
                } else {
                    MediaAdjustments.entries.size + 1
                }
            }

            sorted.forEach { mod ->
                drawScope.draw(
                    density = Density(1f),
                    layoutDirection = LayoutDirection.Ltr,
                    canvas = canvas,
                    size = Size(
                        width = originalImage.width.toFloat(),
                        height = originalImage.height.toFloat()
                    )
                ) {
                    if (mod is ImageModification.Filter) {
                        drawImage(
                            image = bitmap,
                            colorFilter = ColorFilter.colorMatrix(mod.type.matrix)
                        )
                    } else if (mod is ImageModification.Adjustment) {
                        drawImage(
                            image = bitmap,
                            colorFilter = ColorFilter.colorMatrix(ColorMatrix(mod.type.getMatrix(mod.value)))
                        )
                    }
                }
            }

            moddedImage = bitmap
        }
    }

    var actualStarts by remember { mutableStateOf(Pair(0f, 0f)) }
    var containerDimens by remember { mutableStateOf(Size.Zero) }

    Scaffold(
        topBar = {
            val textMeasurer = rememberTextMeasurer()
            val mainViewModel = LocalMainViewModel.current

            val overwriteByDefault by mainViewModel.settings.Editing.getOverwriteByDefault().collectAsStateWithLifecycle(initialValue = false)
            var overwrite by remember { mutableStateOf(false) }

            LaunchedEffect(overwriteByDefault) {
                overwrite = overwriteByDefault
            }

            ImageEditorTopBar(
                modifications = imageEditingState.modificationList,
                lastSavedModCount = lastSavedModCount,
                overwrite = overwrite,
                setOverwrite = {
                    overwrite = it
                },
                saveImage = {
                    saveImage(
                        context = context,
                        image = originalImage,
                        containerDimens = containerDimens,
                        absolutePath = absolutePath,
                        drawingPaintState = drawingPaintState,
                        imageEditingState = imageEditingState,
                        modifications = modifications,
                        textMeasurer = textMeasurer,
                        actualLeft = actualStarts.first,
                        actualTop = actualStarts.second,
                        overwrite = overwrite,
                        isFromOpenWithView = isFromOpenWithView
                    )
                }
            )
        },
        bottomBar = {
            ImageEditorBottomBar(
                modifications = modifications,
                originalAspectRatio = originalImage.width.toFloat() / originalImage.height,
                imageEditingState = imageEditingState,
                drawingPaintState = drawingPaintState,
                pagerState = pagerState,
                increaseModCount = {
                    totalModCount.intValue += 1
                },
                saveEffect = { filter ->
                    imageEditingState.removeModifications {
                        it is ImageModification.Filter
                    }

                    imageEditingState.addModification(
                        ImageModification.Filter(
                            type = filter
                        )
                    )

                    totalModCount.intValue += 1

                    coroutineScope.launch {
                        filterPagerState.animateScrollToPage(
                            MediaColorFilters.entries.indexOf(filter),
                            animationSpec = tween(
                                durationMillis = AnimationConstants.DURATION
                            )
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val animatedScale by animateFloatAsState(
                targetValue = imageEditingState.scale,
                animationSpec = tween(
                    durationMillis = AnimationConstants.DURATION_SHORT
                )
            )
            val animatedOffset by animateOffsetAsState(
                targetValue = imageEditingState.offset,
                animationSpec = tween(
                    durationMillis = AnimationConstants.DURATION_SHORT
                )
            )

            val animatedRotation by animateFloatAsState(
                targetValue = imageEditingState.rotation
            )

            val latestCrop by remember {
                derivedStateOf {
                    imageEditingState.modificationList.lastOrNull {
                        it is ImageModification.Crop
                    } as? ImageModification.Crop ?: ImageModification.Crop(0f, 0f, 0f, 0f)
                }
            }

            // find the top left of the actual video area
            var originalCrop by remember { mutableStateOf(ImageModification.Crop(0f, 0f, 0f, 0f)) }
            LaunchedEffect(imageEditingState.modificationList.lastOrNull()) {
                if (originalCrop.width == 0f && originalCrop.height == 0f && moddedImage.width != 512 && moddedImage.height != 512) {
                    originalCrop = imageEditingState.modificationList.lastOrNull {
                        it is ImageModification.Crop
                    } as? ImageModification.Crop ?: ImageModification.Crop(0f, 0f, 0f, 0f)
                }
            }

            val cropScale by animateFloatAsState(
                targetValue =
                    if (pagerState.currentPage == ImageEditorTabs.entries.indexOf(ImageEditorTabs.Crop)) 0.9f else 1f
            )

            val height = this@BoxWithConstraints.maxHeight
            val width = this@BoxWithConstraints.maxWidth
            val localDensity = LocalDensity.current

            val imageSize by remember {
                derivedStateOf {
                    with(localDensity) {
                        val xRatio = width.toPx() / moddedImage.width
                        val yRatio = height.toPx() / moddedImage.height
                        val ratio = min(xRatio, yRatio)

                        val width = moddedImage.width * ratio
                        val height = moddedImage.height * ratio

                        DpSize(
                            width = width.toDp(),
                            height = height.toDp()
                        )
                    }
                }
            }

            val rotationScale by animateFloatAsState(
                targetValue =
                    with (localDensity) {
                        if (imageEditingState.rotation % 180f == 0f) {
                            min(
                                width.toPx() / imageSize.width.toPx(),
                                height.toPx() / imageSize.height.toPx()
                            )
                        } else {
                            min(
                                width.toPx() / imageSize.height.toPx(),
                                height.toPx() / imageSize.height.toPx()
                            )
                        }
                    }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize(1f)
                    .rotate(animatedRotation)
                    .align(Alignment.Center)
                    .scale(cropScale * rotationScale)
            ) {
                val isInFilterPage by remember {
                    derivedStateOf {
                        pagerState.currentPage == ImageEditorTabs.entries.indexOf(ImageEditorTabs.Filters)
                    }
                }

                AnimatedContent(
                    targetState = isInFilterPage,
                    transitionSpec = {
                        (slideInHorizontally {
                            if (isInFilterPage) it
                            else -it
                        } + fadeIn()
                                ).togetherWith(
                                (slideOutHorizontally {
                                    if (isInFilterPage) -it
                                    else it
                                } + fadeOut())
                            )
                    },
                    modifier = Modifier
                        .fillMaxSize(1f)
                ) { state ->
                    if (state) {
                        Box(
                            modifier = Modifier
                                .padding(16.dp)
                        ) {
                            ImageFilterPage(
                                image = originalImage,
                                modifications = drawingPaintState.modifications,
                                pagerState = filterPagerState
                            )
                        }
                    } else {
                        val textMeasurer = rememberTextMeasurer()
                        var showTextDialog by remember { mutableStateOf(false) }
                        var tapPosition by remember { mutableStateOf(Offset.Zero) }

                        if (showTextDialog) {
                            TextEntryDialog(
                                title = stringResource(id = R.string.editing_text),
                                placeholder = stringResource(id = R.string.bottom_sheets_enter_text),
                                onValueChange = { input ->
                                    input.isNotBlank()
                                },
                                onConfirm = { input ->
                                    if (input.isNotBlank()) {
                                        val size = textMeasurer.measure(
                                            text = input,
                                            style = DrawableText.Styles.Default.copy(
                                                color = drawingPaintState.paint.color,
                                                fontSize = TextUnit(drawingPaintState.paint.strokeWidth, TextUnitType.Sp)
                                            )
                                        ).size

                                        val newText = ImageModification.DrawingText(
                                            text = DrawableText(
                                                text = input,
                                                position = Offset(tapPosition.x, tapPosition.y),
                                                paint = drawingPaintState.paint,
                                                rotation = 0f,
                                                size = size
                                            )
                                        )

                                        drawingPaintState.modifications.add(newText)
                                        totalModCount.intValue += 1

                                        showTextDialog = false
                                        true
                                    } else {
                                        false
                                    }
                                },
                                onDismiss = {
                                    showTextDialog = false
                                }
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize(1f)
                                .graphicsLayer {
                                    translationX = animatedOffset.x
                                    translationY = animatedOffset.y
                                    scaleX = animatedScale
                                    scaleY = animatedScale
                                }
                                .makeVideoDrawCanvas(
                                    drawingPaintState = drawingPaintState,
                                    textMeasurer = textMeasurer,
                                    currentVideoPosition = remember { mutableFloatStateOf(0f) },
                                    enabled = pagerState.currentPage == ImageEditorTabs.entries.indexOf(ImageEditorTabs.Draw),
                                    addText = { position ->
                                        tapPosition = position
                                        showTextDialog = true
                                    }
                                )
                        ) {
                            Image(
                                bitmap = moddedImage,
                                contentDescription = "Preview the image",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(imageSize)
                                    .background(MaterialTheme.colorScheme.background)
                                    .align(Alignment.Center)
                            )

                            val actualTop by remember {
                                derivedStateOf {
                                    with(localDensity) {
                                        (height.toPx() - imageSize.height.toPx()) / 2
                                    }
                                }
                            }

                            val actualLeft by remember {
                                derivedStateOf {
                                    with(localDensity) {
                                        (width.toPx() - imageSize.width.toPx()) / 2
                                    }
                                }
                            }

                            LaunchedEffect(actualLeft, actualTop) {
                                actualStarts = Pair(actualLeft, actualTop)
                            }

                            PreviewCanvas(
                                drawingPaintState = drawingPaintState,
                                actualLeft = actualLeft,
                                actualTop = actualTop,
                                latestCrop = latestCrop,
                                originalCrop = originalCrop,
                                pagerState = pagerState,
                                width = width,
                                height = height
                            )

                            AnimatedContent(
                                targetState = originalImage.asAndroidBitmap().allocationByteCount == 0,
                                transitionSpec = {
                                    fadeIn(
                                        animationSpec = tween(
                                            durationMillis = AnimationConstants.DURATION_LONG
                                        )
                                    ).togetherWith(
                                        fadeOut(
                                            animationSpec = tween(
                                                durationMillis = AnimationConstants.DURATION_LONG
                                            )
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .width(width)
                                    .height(height)
                                    .align(Alignment.Center)
                            ) { state ->
                                if (state) {
                                    Box(
                                        modifier = Modifier
                                            .requiredSize(
                                                width = width,
                                                height = height
                                            )
                                            .clip(RoundedCornerShape(16.dp))
                                            .align(Alignment.Center)
                                            .shimmerEffect(
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                                highlightColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                            )
                                    )
                                } else {
                                    // just fill the size so it doesn't scale down
                                    Box(
                                        modifier = Modifier
                                            .requiredSize(
                                                width = width,
                                                height = height
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = pagerState.currentPage == ImageEditorTabs.entries.indexOf(ImageEditorTabs.Crop) && moddedImage.asAndroidBitmap().allocationByteCount > 0,
                    enter = fadeIn(
                        animationSpec = AnimationConstants.expressiveTween(
                            durationMillis = AnimationConstants.DURATION
                        )
                    ),
                    exit = fadeOut(
                        animationSpec = AnimationConstants.expressiveTween(
                            durationMillis = AnimationConstants.DURATION
                        )
                    ),
                    modifier = Modifier
                        .requiredSize(
                            width = width + 32.dp, // so the CropBox handles don't clip
                            height = height + 32.dp
                        )
                ) {
                    val localDensity = LocalDensity.current

                    CropBox(
                        containerWidth = with(localDensity) { width.toPx() },
                        containerHeight = with(localDensity) { height.toPx() },
                        mediaAspectRatio = moddedImage.width.toFloat() / moddedImage.height,
                        editingState = imageEditingState,
                        scale = animatedScale,
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = animatedScale * 16.dp.toPx() + animatedOffset.x
                                translationY = animatedScale * 16.dp.toPx() + animatedOffset.y
                                scaleX = animatedScale
                                scaleY = animatedScale
                            },
                        onAreaChanged = { area, original ->
                            imageEditingState.removeModifications { it is ImageModification.Crop } // because Crop gets called a million times each movement
                            imageEditingState.addModification(
                                ImageModification.Crop(
                                    top = area.top,
                                    left = area.left,
                                    width = area.width,
                                    height = area.height
                                )
                            )

                            containerDimens = original
                        },
                        onCropDone = {
                            val actualWidth = with(localDensity) { (containerDimens.width - 56.dp.toPx()) } // subtract spacing of handles
                            val actualHeight = with(localDensity) { (containerDimens.height - 56.dp.toPx()) } // to not clip them
                            val targetX = actualWidth / latestCrop.width
                            val targetY = actualHeight / latestCrop.height

                            val scale = max(1f, min(targetX, targetY))
                            imageEditingState.setScale(scale)

                            imageEditingState.setOffset(
                                Offset(
                                    x = with(localDensity) {
                                        scale * (-latestCrop.left + (containerDimens.width - latestCrop.width) / 2)
                                    },
                                    y = with(localDensity) {
                                        scale * (-latestCrop.top + (containerDimens.height - latestCrop.height) / 2)
                                    }
                                )
                            )
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = pagerState.currentPage == ImageEditorTabs.entries.indexOf(ImageEditorTabs.Adjust)
                        || pagerState.currentPage == ImageEditorTabs.entries.indexOf(ImageEditorTabs.Draw),
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier
                    .offset(y = 4.dp)
                    .fillMaxWidth(1f)
                    .align(Alignment.BottomCenter)
            ) {
                ImageEditorAdjustmentTools(
                    drawingPaintState = drawingPaintState,
                    modifications = modifications,
                    currentEditorPage = pagerState.currentPage,
                    totalModCount = totalModCount,
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .wrapContentHeight(),
                    addModification = imageEditingState::addModification
                )
            }
        }
    }
}