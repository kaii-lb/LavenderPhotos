package com.kaii.photos.models

import android.content.Context
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Priority
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
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
import com.kaii.photos.helpers.EncryptionManager
import com.kaii.photos.mediastore.ImmichInfo
import com.kaii.photos.mediastore.SecureInfo
import java.io.ByteArrayInputStream
import java.io.File
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

class ImmichModelLoader(
    concreteLoader: ModelLoader<GlideUrl, InputStream>,
    modelCache: ModelCache<ImmichInfo, GlideUrl>?
) : BaseGlideUrlLoader<ImmichInfo>(concreteLoader, modelCache) {
    override fun getUrl(model: ImmichInfo, width: Int, height: Int, options: Options?): String {
        return model.endpoint + if (model.useThumbnail) model.thumbnail else model.original
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

class DecryptedThumbnailFetcher(
    private val item: SecureInfo
) : DataFetcher<InputStream> {
    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        try {
            val encryptedBytes = File(item.absolutePath).readBytes()

            val decryptedBytes = EncryptionManager.decryptBytes(
                bytes = encryptedBytes,
                iv = item.iv
            )

            val inputStream = ByteArrayInputStream(decryptedBytes)

            callback.onDataReady(inputStream)
        } catch (e: Exception) {
            callback.onLoadFailed(e)
        }
    }

    override fun cleanup() {}

    override fun cancel() {}

    override fun getDataClass(): Class<InputStream> = InputStream::class.java

    override fun getDataSource(): DataSource = DataSource.LOCAL
}