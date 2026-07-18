package com.kaii.photos.data.glide.svg

import com.bumptech.glide.load.EncodeStrategy
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceEncoder
import com.bumptech.glide.load.engine.Resource
import com.caverock.androidsvg.SVG
import java.io.File

class SvgEncoder : ResourceEncoder<SVG> {
    override fun getEncodeStrategy(options: Options): EncodeStrategy = EncodeStrategy.SOURCE

    override fun encode(
        data: Resource<SVG?>,
        file: File,
        options: Options
    ): Boolean = false
}