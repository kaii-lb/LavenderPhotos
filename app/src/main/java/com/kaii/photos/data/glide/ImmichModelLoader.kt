package com.kaii.photos.data.glide

import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.Headers
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.load.model.ModelCache
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.load.model.stream.BaseGlideUrlLoader
import com.kaii.photos.mediastore.ImmichInfo
import java.io.InputStream

class ImmichModelLoader(
    concreteLoader: ModelLoader<GlideUrl, InputStream>,
    modelCache: ModelCache<ImmichInfo, GlideUrl>?
) : BaseGlideUrlLoader<ImmichInfo>(concreteLoader, modelCache) {
    override fun getUrl(model: ImmichInfo, width: Int, height: Int, options: Options?): String {
        return model.endpoint + if (model.useThumbnail) model.thumbnail else model.original
    }

    override fun getHeaders(model: ImmichInfo, width: Int, height: Int, options: Options?): Headers =
        LazyHeaders.Builder()
            .apply {
                model.auth.headers.keys.firstOrNull()?.let { headerName ->
                    val headerValue = model.auth.headers[headerName]!!
                    addHeader(headerName, headerValue)
                }
            }
            .build()


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