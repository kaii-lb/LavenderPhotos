package com.kaii.photos.data.glide.svg

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import com.caverock.androidsvg.SVG

class SvgDrawable(private val svg: SVG) : Drawable() {
    override fun draw(canvas: Canvas) {
        if (bounds.isEmpty) return

        canvas.translate(
            (bounds.width() - svg.documentWidth) / 2,
            (bounds.height() - svg.documentHeight) / 2
        )

        svg.renderToCanvas(canvas, RectF(bounds))
    }

    override fun setAlpha(alpha: Int) {}

    override fun setColorFilter(colorFilter: ColorFilter?) {}

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun getIntrinsicWidth(): Int = svg.documentWidth.toInt()

    override fun getIntrinsicHeight(): Int = svg.documentHeight.toInt()
}