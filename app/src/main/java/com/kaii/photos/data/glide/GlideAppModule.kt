package com.kaii.photos.data.glide

import android.content.Context
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator
import com.bumptech.glide.module.AppGlideModule
import com.kaii.photos.mediastore.ImmichInfo
import com.kaii.photos.mediastore.SecureInfo
import java.io.InputStream

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

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.append(
            ImmichInfo::class.java,
            InputStream::class.java,
            ImmichModelLoader.Factory()
        )

        registry.append(
            SecureInfo::class.java,
            InputStream::class.java,
            SecureModelLoader.Factory()
        )
    }
}

