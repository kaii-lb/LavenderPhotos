package com.kaii.photos.models.multi_album

import android.content.Context
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.Headers
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.load.model.ModelCache
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.load.model.stream.BaseGlideUrlLoader
import com.bumptech.glide.module.AppGlideModule
import com.kaii.photos.mediastore.ImmichInfo
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
    }
}

class ImmichModelLoader(
    concreteLoader: ModelLoader<GlideUrl, InputStream>,
    modelCache: ModelCache<ImmichInfo, GlideUrl>?
) : BaseGlideUrlLoader<ImmichInfo>(concreteLoader, modelCache) {

    override fun getUrl(model: ImmichInfo, width: Int, height: Int, options: Options?): String {
        return model.thumbnail
    }

    override fun getHeaders(model: ImmichInfo, width: Int, height: Int, options: Options?): Headers {
        return LazyHeaders.Builder()
            .addHeader("Authorization", "Bearer ${model.accessToken}")
            .build()
    }

    override fun handles(model: ImmichInfo): Boolean = true

    // Factory to register the loader
    class Factory : ModelLoaderFactory<ImmichInfo, InputStream> {
        private val modelCache = ModelCache<ImmichInfo, GlideUrl>(500)

        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<ImmichInfo, InputStream> {
            return ImmichModelLoader(
                multiFactory.build(GlideUrl::class.java, InputStream::class.java),
                modelCache
            )
        }

        override fun teardown() {}
    }
}