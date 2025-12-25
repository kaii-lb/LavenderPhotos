package com.kaii.photos.models.multi_album

import android.content.Context
import android.util.Log
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator
import com.bumptech.glide.module.AppGlideModule


@Suppress("unused")
@GlideModule
class GlideAppModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        super.applyOptions(context, builder)

        builder.setLogLevel(Log.WARN)

        val calculator = MemorySizeCalculator.Builder(context)
            .setMemoryCacheScreens(3f)
            .build()

        builder.setMemoryCache(LruResourceCache(calculator.memoryCacheSize.toLong()))
    }
}