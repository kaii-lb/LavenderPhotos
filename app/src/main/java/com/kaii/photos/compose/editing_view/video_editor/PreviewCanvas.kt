package com.kaii.photos.compose.editing_view.video_editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.kaii.photos.helpers.editing.DrawableText
import com.kaii.photos.helpers.editing.DrawingPaintState
import com.kaii.photos.helpers.editing.VideoEditorTabs
import com.kaii.photos.helpers.editing.VideoModification
import com.kaii.photos.helpers.editing.toOffset

@Composable
fun BoxScope.PreviewCanvas(
    drawingPaintState: DrawingPaintState,
    actualLeft: Float,
    actualTop: Float,
    latestCrop: VideoModification.Crop,
    originalCrop: VideoModification.Crop,
    pagerState: PagerState,
    width: Dp,
    height: Dp
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val textMeasurer = rememberTextMeasurer()

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
                    is VideoModification.DrawingPath -> {
                        val (_, path, _) = modification

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

                    is VideoModification.DrawingText -> {
                        val (_, text, _) = modification

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

                                // if (selectedText.value == modification) {
                                //     drawRoundRect(
                                //         color = textPaint.color,
                                //         topLeft = textSize.toOffset().copy(y = 0f) * -0.05f,
                                //         cornerRadius = CornerRadius(16.dp.toPx() * textPaint.strokeWidth / 128f),
                                //         size = textSize.toSize() * 1.1f,
                                //         style = Stroke(
                                //             width = textPaint.strokeWidth / 2,
                                //             cap = StrokeCap.Round
                                //         )
                                //     )
                                // }
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
                clipRect(
                    left = actualLeft + latestCrop.left,
                    top = actualTop + latestCrop.top,
                    right = actualLeft + latestCrop.right,
                    bottom = actualTop + latestCrop.bottom,
                    clipOp = ClipOp.Difference
                ) {
                    drawRect(
                        color = backgroundColor,
                        size = Size(width.toPx(), height.toPx())
                    )
                }
            }
        }
    }
}