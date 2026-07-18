package com.kaii.photos.data.glide

import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.ModelCache
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.load.model.stream.BaseGlideUrlLoader
import com.kaii.photos.mediastore.SecureInfo
import java.io.InputStream

class SecureModelLoader(
    concreteLoader: ModelLoader<GlideUrl, InputStream>,
    modelCache: ModelCache<SecureInfo, GlideUrl>?
) : BaseGlideUrlLoader<SecureInfo>(concreteLoader, modelCache) {
    override fun getUrl(model: SecureInfo, width: Int, height: Int, options: Options?): String {
        return model.absolutePath
    }

    override fun buildLoadData(model: SecureInfo, width: Int, height: Int, options: Options): ModelLoader.LoadData<InputStream?> {
        return ModelLoader.LoadData(
            model.key,
            DecryptedThumbnailFetcher(model)
        )
    }

    override fun handles(model: SecureInfo) = true

    class Factory : ModelLoaderFactory<SecureInfo, InputStream> {
        private val modelCache = ModelCache<SecureInfo, GlideUrl>(500)

        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<SecureInfo, InputStream> {
            return SecureModelLoader(
                multiFactory.build(GlideUrl::class.java, InputStream::class.java),
                modelCache
            )
        }

        override fun teardown() {}
    }
}