package com.kaii.photos.gallery_model

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule

/** Ensures that Glide's generated API is created for the Gallery sample.  */
@GlideModule
class GalleryModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        super.applyOptions(context, builder)

		val cacheDir = context.cacheDir
		val cacheSize = 1024 * 1024 * 1000L
		
        builder.setIsActiveResourceRetentionAllowed(true)
        //builder.setDiskCache(DiskLruCacheFactory(cacheDir, cacheSize))
    }
}
