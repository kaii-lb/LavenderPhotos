package com.kaii.photos.data.glide.svg

import com.bumptech.glide.load.Options
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.resource.SimpleResource
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder
import com.caverock.androidsvg.SVG


class SvgDrawableTranscoder : ResourceTranscoder<SVG, SvgDrawable> {
    override fun transcode(
        toTranscode: Resource<SVG?>,
        options: Options
    ): Resource<SvgDrawable?> = SimpleResource(SvgDrawable(toTranscode.get()))
}