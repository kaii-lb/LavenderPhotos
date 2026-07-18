package com.kaii.photos.data.glide.svg

import android.util.Log
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.resource.SimpleResource
import com.caverock.androidsvg.SVG
import java.io.InputStream


class SvgDecoder : ResourceDecoder<InputStream, SVG> {
    override fun handles(source: InputStream, options: Options): Boolean = true

    override fun decode(
        source: InputStream,
        width: Int,
        height: Int,
        options: Options
    ): Resource<SVG?>? = try {
        val svg = SVG.getFromInputStream(source)

        svg.documentWidth = width.toFloat()
        svg.documentHeight = height.toFloat()

        SimpleResource(svg)
    } catch (e: Exception) {
        Log.d(SvgDecoder::class.qualifiedName, "Failed to decode SVG! ${e.message}")
        null
    }
}