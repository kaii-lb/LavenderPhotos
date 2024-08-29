package com.kaii.photos.models.album_grid

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.DiskLruCacheFactory
import com.bumptech.glide.module.AppGlideModule

/** Ensures that Glide's generated API is created for the Gallery sample.  */
@GlideModule
class AlbumsModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        super.applyOptions(context, builder)

  //       val cacheDir = context.cacheDir.path
		// val cacheSize = 1024 * 1024 * 1000L
		
        builder.setIsActiveResourceRetentionAllowed(true)
        // builder.setDiskCache(DiskLruCacheFactory(cacheDir, cacheSize))
    }
}
