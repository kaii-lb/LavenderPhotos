package com.kaii.photos.compose.editing_view

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.core.graphics.scale
import com.bumptech.glide.Glide
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.R
import com.kaii.photos.compose.CroppingRatioBottomSheet
import com.kaii.photos.compose.widgets.shimmerEffect
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.helpers.editing.CroppingAspectRatio
import com.kaii.photos.helpers.editing.DrawableImage
import com.kaii.photos.helpers.editing.DrawingColors
import com.kaii.photos.helpers.editing.DrawingItems
import com.kaii.photos.helpers.editing.DrawingPaintState
import com.kaii.photos.helpers.editing.ImageModification
import com.kaii.photos.helpers.editing.MediaColorFilters
import com.kaii.photos.helpers.editing.SharedModification
import com.kaii.photos.helpers.editing.VideoModification
import com.kaii.photos.mediastore.getMediaStoreDataFromUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SharedEditorFilterContent(
    modifications: List<SharedModification>,
    modifier: Modifier = Modifier,
    saveEffect: (MediaColorFilters) -> Unit
) {
    var original by remember {
        mutableStateOf(
            (modifications.lastOrNull {
                it is SharedModification.Filter
            } as? SharedModification.Filter)?.type ?: MediaColorFilters.None
        )
    }

    val last by remember {
        derivedStateOf {
            (modifications.lastOrNull { it is SharedModification.Filter } as? SharedModification.Filter)?.type ?: MediaColorFilters.None
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize(1f),
        verticalAlignment = Alignment.Companion.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = Alignment.Companion.CenterHorizontally
        )
    ) {
        Button(
            onClick = {
                saveEffect(original)
            },
            shapes = ButtonDefaults.shapes(
                shape = CircleShape,
                pressedShape = CircleShape
            ),
            enabled = original != last,
            modifier = Modifier.Companion
                .size(56.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.reset),
                contentDescription = "reset the chosen filter"
            )
        }

        Button(
            onClick = {
                original = last

                saveEffect(last)
            },
            shapes = ButtonDefaults.shapes(
                shape = CircleShape,
                pressedShape = CircleShape
            ),
            enabled = original != last,
            modifier = Modifier.Companion
                .height(56.dp)
                .width(108.dp)
        ) {
            Text(
                text = stringResource(id = if (original != last) R.string.filter_select else R.string.filter_selected),
                fontSize = TextUnit(TextStylingConstants.Companion.MEDIUM_TEXT_SIZE, TextUnitType.Companion.Sp)
            )
        }
    }
}

@Composable
fun SharedEditorDrawContent(
    drawingPaintState: DrawingPaintState,
    currentTime: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize(1f)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            space = 12.dp,
            alignment = Alignment.CenterHorizontally
        )
    ) {
        var collapsed by remember { mutableStateOf(true) }

        if (collapsed) {
            PaintTypeSelector(
                drawingPaintState = drawingPaintState
            )

            Button(
                onClick = {
                    drawingPaintState.undoModification()
                },
                shape = CircleShape,
                contentPadding = PaddingValues.Companion.Zero,
                modifier = Modifier
                    .size(56.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.undo),
                    contentDescription = "remove the last drawing"
                )
            }
        }

        AnimatedContent(
            targetState = drawingPaintState.paintType
        ) { state ->
            when (state) {
                DrawingItems.Image -> {
                    ImageSelector(
                        drawingPaintState = drawingPaintState
                    )
                }

                else -> {
                    PaintColorSelector(
                        drawingPaintState = drawingPaintState,
                        collapsed = collapsed,
                        currentTime = currentTime
                    ) {
                        collapsed = it
                    }
                }
            }
        }

        if (collapsed) {
            Button(
                onClick = {
                    drawingPaintState.clearModifications()
                },
                shape = CircleShape,
                contentPadding = PaddingValues.Companion.Zero,
                modifier = Modifier
                    .size(56.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.trash),
                    contentDescription = "remove all drawings"
                )
            }
        }
    }
}

@SuppressLint("ResourceType")
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun PaintTypeSelector(
    drawingPaintState: DrawingPaintState,
    modifier: Modifier = Modifier
) {
    var collapsed by remember { mutableStateOf(true) }
    val itemList = remember { mutableStateListOf<DrawingItems>().apply { addAll(DrawingItems.entries.toList()) } }
    val listState = rememberLazyListState()

    SharedTransitionLayout {
        AnimatedContent(
            targetState = collapsed,
            transitionSpec = {
                (expandHorizontally(expandFrom = Alignment.Start) + fadeIn()).togetherWith(
                    shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut()
                )
            },
            modifier = Modifier
                .wrapContentSize()
        ) { state ->
            if (state) {
                Button(
                    onClick = {
                        collapsed = false
                    },
                    shape = CircleShape,
                    contentPadding = PaddingValues.Zero,
                    modifier = Modifier
                        .size(56.dp)
                        .sharedElement(
                            sharedContentState = rememberSharedContentState(drawingPaintState.paintType),
                            animatedVisibilityScope = this@AnimatedContent
                        )
                ) {
                    Icon(
                        painter = painterResource(id = drawingPaintState.paintType.icon),
                        contentDescription = "set the paint type"
                    )
                }
            } else {
                LazyRow(
                    state = listState,
                    modifier = modifier
                        .animateContentSize()
                        .height(56.dp)
                        .wrapContentWidth()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
                ) {
                    items(
                        count = itemList.size
                    ) { index ->
                        val item = itemList[index]

                        ToggleButton(
                            checked = drawingPaintState.paintType == item,
                            onCheckedChange = { _ ->
                                drawingPaintState.setPaintType(item)
                                drawingPaintState.setSelectedItem(null)
                                collapsed = true
                            },
                            shapes = ToggleButtonDefaults.shapes(
                                checkedShape = CircleShape
                            ),
                            contentPadding = PaddingValues.Zero,
                            colors = ToggleButtonDefaults.toggleButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier
                                .size(56.dp)
                                .sharedElement(
                                    sharedContentState = rememberSharedContentState(item),
                                    animatedVisibilityScope = this@AnimatedContent
                                )
                        ) {
                            Icon(
                                painter = painterResource(id = item.icon),
                                contentDescription = "set the paint type"
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PaintColorSelector(
    drawingPaintState: DrawingPaintState,
    collapsed: Boolean,
    currentTime: Float,
    modifier: Modifier = Modifier,
    setCollapsed: (Boolean) -> Unit
) {
    SharedTransitionLayout {
        AnimatedContent(
            targetState = collapsed,
            transitionSpec = {
                (scaleIn(transformOrigin = TransformOrigin(0f, 0.5f))
                        + expandHorizontally(expandFrom = Alignment.Start)
                        + fadeIn()).togetherWith(
                    scaleOut(transformOrigin = TransformOrigin(0.5f, 0.5f))
                            + shrinkHorizontally(shrinkTowards = Alignment.CenterHorizontally)
                            + fadeOut()
                )
            },
            modifier = modifier
                .height(56.dp)
        ) { state ->
            if (state) {
                Button(
                    onClick = {
                        setCollapsed(false)
                    },
                    shape = CircleShape,
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier
                        .width(96.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(1f)
                            .clip(CircleShape)
                            .background(drawingPaintState.color)
                            .sharedElement(
                                sharedContentState = rememberSharedContentState(drawingPaintState.color),
                                animatedVisibilityScope = this@AnimatedContent
                            )
                    )
                }
            } else {
                LazyRow(
                    modifier = Modifier
                        .fillMaxSize(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(
                        space = 8.dp,
                        alignment = Alignment.CenterHorizontally
                    )
                ) {
                    items(
                        count = DrawingColors.colorList.size
                    ) { index ->
                        val color = DrawingColors.colorList[index]
                        val animatedCornerRadius by animateDpAsState(
                            targetValue = if (drawingPaintState.color == color) 100.dp else 0.dp
                        )

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp + animatedCornerRadius))
                                .background(if (drawingPaintState.color == color) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceBright)
                                .sharedElement(
                                    sharedContentState = rememberSharedContentState(color),
                                    animatedVisibilityScope = this@AnimatedContent
                                )
                                .clickable {
                                    drawingPaintState.setColor(color, currentTime)
                                    setCollapsed(true)
                                }
                                .padding(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(1f)
                                    .clip(RoundedCornerShape(4.dp + animatedCornerRadius))
                                    .background(color)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageSelector(
    drawingPaintState: DrawingPaintState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val windowInfo = LocalWindowInfo.current
    val coroutineScope = rememberCoroutineScope()

    var image: ImageBitmap? by remember { mutableStateOf(null) }
    var imageUri: Uri? by remember { mutableStateOf(null) }
    var wantedUri: Uri? by remember { mutableStateOf(null) }

    LaunchedEffect(drawingPaintState.selectedItem) {
        withContext(Dispatchers.IO) {
            if (drawingPaintState.selectedItem is SharedModification.DrawingImage
                && imageUri != (drawingPaintState.selectedItem as SharedModification.DrawingImage).image.bitmapUri
            ) {
                val drawingImage = (drawingPaintState.selectedItem as SharedModification.DrawingImage)
                imageUri = drawingImage.image.bitmapUri

                val bitmap = if (drawingImage.image.isAvif) {  // avif won't load on some android distros, so use glide for that
                    Glide.with(context)
                        .asBitmap()
                        .load(drawingImage.image.bitmapUri)
                        .submit()
                        .get()
                } else {
                    context.contentResolver.openInputStream(drawingImage.image.bitmapUri).use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }
                }

                image = bitmap
                    .scale(
                        width = windowInfo.containerSize.width / 4,
                        height = ((windowInfo.containerSize.width / 4) * (bitmap.width.toFloat() / bitmap.height)).toInt()
                    )
                    .asImageBitmap()

                wantedUri = imageUri
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            wantedUri = uri

            val size = context.contentResolver.openInputStream(uri).use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(
                    inputStream,
                    null,
                    options
                )

                IntSize(options.outWidth, options.outHeight)
            }

            val isAvif = context.contentResolver.getMediaStoreDataFromUri(uri)?.absolutePath?.endsWith(".avif") == true

            drawingPaintState.setSelectedItem(
                if (drawingPaintState.isVideo) {
                    VideoModification.DrawingImage(
                        image = DrawableImage(
                            bitmapUri = uri,
                            paint = drawingPaintState.paint,
                            rotation = 0f,
                            position = Offset.Zero,
                            size = size,
                            isAvif = isAvif
                        )
                    )
                } else {
                    ImageModification.DrawingImage(
                        image = DrawableImage(
                            bitmapUri = uri,
                            paint = drawingPaintState.paint,
                            rotation = 0f,
                            position = Offset.Zero,
                            size = size,
                            isAvif = isAvif
                        )
                    )
                }
            )
        } else {
            coroutineScope.launch {
                LavenderSnackbarController.pushEvent(
                    LavenderSnackbarEvents.MessageEvent(
                        message = resources.getString(R.string.editing_upload_image_failed),
                        icon = R.drawable.broken_image,
                        duration = SnackbarDuration.Short
                    )
                )
            }
        }
    }

    Button(
        onClick = {
            if (drawingPaintState.selectedItem != null) {
                drawingPaintState.setSelectedItem(null)
            } else {
                launcher.launch(
                    PickVisualMediaRequest(
                        mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            }
        },
        shape = CircleShape,
        contentPadding = PaddingValues(
            horizontal = if (drawingPaintState.selectedItem != null && image != null) 8.dp else 20.dp,
            vertical = 8.dp
        ),
        modifier = modifier
            .animateContentSize()
            .height(56.dp)
            .wrapContentWidth()
    ) {
        AnimatedContent(
            targetState = drawingPaintState.selectedItem != null && image != null
        ) { state ->
            if (state) {
                AnimatedContent(
                    targetState = wantedUri == imageUri,
                    transitionSpec = {
                        fadeIn(
                            animationSpec = tween(
                                durationMillis = AnimationConstants.DURATION
                            )
                        ).togetherWith(
                            fadeOut(
                                animationSpec = tween(
                                    durationMillis = AnimationConstants.DURATION
                                )
                            )
                        )
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .height(56.dp)
                        .width(80.dp),
                    contentAlignment = Alignment.Center
                ) { state ->
                    if (state) {
                        Image(
                            bitmap = image!!,
                            contentDescription = "preview selected image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(80.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(56.dp)
                                .shimmerEffect(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                    highlightColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .wrapContentWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.image_arrow_up),
                        contentDescription = "upload an image"
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = stringResource(id = R.string.editing_upload_image),
                        fontSize = TextUnit(TextStylingConstants.MEDIUM_TEXT_SIZE, TextUnitType.Sp)
                    )
                }
            }
        }
    }
}

@Composable
fun SharedEditorCropContent(
    imageAspectRatio: Float,
    croppingAspectRatio: CroppingAspectRatio,
    rotation: Float,
    setCroppingAspectRatio: (CroppingAspectRatio) -> Unit,
    setRotation: (Float) -> Unit,
    resetCrop: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        EditingViewBottomAppBarItem(
            text = stringResource(id = R.string.editing_rotate),
            icon = R.drawable.rotate_ccw,
            onClick = {
                setRotation(rotation - 90f)
            }
        )

        val showSheet = remember { mutableStateOf(false) }

        CroppingRatioBottomSheet(
            show = showSheet,
            ratio = croppingAspectRatio,
            originalImageRatio = imageAspectRatio,
            onSetCroppingRatio = setCroppingAspectRatio
        )

        EditingViewBottomAppBarItem(
            text = stringResource(id = R.string.editing_ratio),
            icon = R.drawable.resolution,
            onClick = {
                showSheet.value = true
            }
        )

        EditingViewBottomAppBarItem(
            text = stringResource(id = R.string.editing_reset),
            icon = R.drawable.reset,
            onClick = {
                resetCrop()
            }
        )
    }
}