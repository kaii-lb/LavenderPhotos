package com.kaii.photos.helpers.grid_management

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.view.View
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

class BitmapUriShadowBuilder(
    view: View,
    private val bitmaps: List<Bitmap>,
    private val count: Int,
    private val density: Density,
) : View.DragShadowBuilder(view) {
    private val offset = with(density) { 8.dp.roundToPx() }

    override fun onProvideShadowMetrics(outShadowSize: Point, outShadowTouchPoint: Point) {
        with(density) {
            val w = 108.dp.roundToPx() + bitmaps.size * offset
            val h = 108.dp.roundToPx() + bitmaps.size * offset

            outShadowSize.set(w, h)
            outShadowTouchPoint.set(w / 2, h / 2)
        }
    }

    override fun onDrawShadow(canvas: Canvas) {
        val paint = Paint().apply {
            alpha = 120
            isAntiAlias = true
            isFilterBitmap = true
        }

        // draw reversed because z index
        val rectOffset = Rect()
        bitmaps.reversed().forEachIndexed { index, bitmap ->
            val invIndex = bitmaps.size - index - 1
            rectOffset.left = offset * invIndex
            rectOffset.top = offset * invIndex
            rectOffset.right = canvas.width + offset * -index
            rectOffset.bottom = canvas.height + offset * -index

            canvas.drawBitmap(bitmap, null, rectOffset, paint)

            paint.alpha = (paint.alpha + 40).coerceAtMost(255)
        }

        val circleRadius = offset * 1.25f
        val circleCenter = circleRadius * 1.25f

        val circlePaint = Paint().apply {
            alpha = 255
            isAntiAlias = true
            color = Color.argb(240, 38, 38, 38)
        }
        canvas.drawCircle(circleCenter, circleCenter, circleRadius, circlePaint)

        val textPaint = Paint().apply {
            alpha = 255
            isAntiAlias = true
            color = Color.WHITE
            textSize = circleRadius * 1.25f
            textAlign = Paint.Align.CENTER
        }

        val textBounds = Rect()
        textPaint.getTextBounds(count.toString(), 0, count.toString().length, textBounds)
        val visualY: Float = circleCenter + (textBounds.height() / 2f) - textBounds.bottom

        canvas.drawText(count.toString(), circleCenter, visualY, textPaint)
    }
}