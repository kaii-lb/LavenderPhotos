package com.kaii.photos.compose.editing_view.video_editor

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import com.kaii.photos.helpers.editing.DrawableText
import com.kaii.photos.helpers.editing.DrawingPaintState
import com.kaii.photos.helpers.editing.SharedModification
import com.kaii.photos.helpers.editing.VideoEditorTabs
import com.kaii.photos.helpers.editing.toOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun BoxScope.PreviewCanvas(
    drawingPaintState: DrawingPaintState,
    actualLeft: Float,
    actualTop: Float,
    latestCrop: SharedModification.Crop,
    originalCrop: SharedModification.Crop,
    pagerState: PagerState,
    width: Dp,
    height: Dp
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val textMeasurer = rememberTextMeasurer()

    val context = LocalContext.current
    var images by remember { mutableStateOf(emptyList<Pair<Uri, ImageBitmap>>()) }

    LaunchedEffect(drawingPaintState.modifications.lastOrNull()) {
        withContext(Dispatchers.IO) {
            val new = drawingPaintState.modifications
                .fastMapNotNull { mod ->
                    mod as? SharedModification.DrawingImage
                }
                .fastMap { mod ->
                    context.contentResolver.openInputStream(mod.image.bitmapUri).use { inputStream ->
                        Pair(mod.image.bitmapUri, BitmapFactory.decodeStream(inputStream).asImageBitmap())
                    }
                }

            images = new
        }
    }

    Canvas(
        modifier = Modifier
            .requiredSize(
                width = width,
                height = height
            )
            .align(Alignment.Center)
    ) {
        clipRect(
            left = actualLeft + latestCrop.left,
            top = actualTop + latestCrop.top,
            right = actualLeft + latestCrop.right,
            bottom = actualTop + latestCrop.bottom
        ) {
            drawingPaintState.modifications.forEach { modification ->
                when (modification) {
                    is SharedModification.DrawingPath -> {
                        val path = modification.path

                        drawPath(
                            path = path.path,
                            style = Stroke(
                                width = path.paint.strokeWidth,
                                cap = path.paint.strokeCap,
                                join = path.paint.strokeJoin,
                                miter = path.paint.strokeMiterLimit,
                                pathEffect = path.paint.pathEffect
                            ),
                            blendMode = path.paint.blendMode,
                            color = path.paint.color,
                            alpha = path.paint.alpha
                        )
                    }

                    is SharedModification.DrawingText -> {
                        val text = modification.text

                        rotate(text.rotation, text.position + text.size.toOffset() / 2f) {
                            translate(text.position.x, text.position.y) {
                                val textLayout = textMeasurer.measure(
                                    text = text.text,
                                    style = DrawableText.Styles.Default.copy(
                                        color = text.paint.color,
                                        fontSize = text.paint.strokeWidth.sp
                                    ),
                                    softWrap = false
                                )

                                drawText(
                                    textLayoutResult = textLayout,
                                    blendMode = text.paint.blendMode
                                )

                                if (drawingPaintState.selectedItem == modification) {
                                    drawRoundRect(
                                        color = text.paint.color,
                                        topLeft = text.size.toOffset().copy(y = 0f) * -0.1f / 2f,
                                        cornerRadius = CornerRadius(16.dp.toPx() * text.paint.strokeWidth / 128f),
                                        size = text.size.toSize() * 1.1f,
                                        style = Stroke(
                                            width = text.paint.strokeWidth / 2,
                                            cap = StrokeCap.Round
                                        )
                                    )
                                }
                            }
                        }
                    }

                    is SharedModification.DrawingImage -> {
                        val image = modification.image

                        rotate(image.rotation, image.position + image.size.toOffset() / 2f) {
                            translate(image.position.x, image.position.y) {
                                drawImage(
                                    image = images.firstOrNull { it.first == image.bitmapUri }?.second ?: ImageBitmap(512, 512),
                                    dstSize = image.size,
                                    filterQuality = FilterQuality.Medium,
                                    blendMode = image.paint.blendMode
                                )

                                if (drawingPaintState.selectedItem == modification) {
                                    drawRoundRect(
                                        color = image.paint.color,
                                        cornerRadius = CornerRadius(16.dp.toPx() * image.paint.strokeWidth / 128f),
                                        size = image.size.toSize(),
                                        style = Stroke(
                                            width = image.paint.strokeWidth / 2,
                                            cap = StrokeCap.Round
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        clipRect(
            left = 0f,
            top = 0f,
            right = actualLeft * 2 + originalCrop.right,
            bottom = actualTop * 2 + originalCrop.bottom
        ) {
            if (pagerState.currentPage == VideoEditorTabs.entries.indexOf(VideoEditorTabs.Crop)) {
                // mask exoplayer's failure to set the background color
                clipRect(
                    left = actualLeft + originalCrop.left,
                    top = actualTop + originalCrop.top,
                    right = actualLeft + originalCrop.right,
                    bottom = actualTop + originalCrop.bottom,
                    clipOp = ClipOp.Difference
                ) {
                    drawRect(
                        color = backgroundColor,
                        size = Size(width.toPx(), height.toPx())
                    )
                }
            } else {
                // mask exoplayer's failure to set the background color
                // and the "unwanted" area of the cropped video
                // add some extra pixels to avoid "white lines" on the very edges
                clipRect(
                    left = actualLeft + latestCrop.left - 5,
                    top = actualTop + latestCrop.top - 5,
                    right = actualLeft + latestCrop.right + 5,
                    bottom = actualTop + latestCrop.bottom + 5,
                    clipOp = ClipOp.Difference
                ) {
                    drawRect(
                        color = backgroundColor,
                        size = Size(width.toPx() + 10, height.toPx() + 10)
                    )
                }
            }
        }
    }
}