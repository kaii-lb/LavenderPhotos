package com.kaii.photos.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.kaii.photos.BuildConfig
import com.kaii.photos.PhotosApplication
import com.kaii.photos.datastore.Settings
import com.kaii.photos.datastore.state.createAlbumGridState
import com.kaii.photos.file_management.sync.ProgressManager
import io.github.kaii_lb.lavender.immichintegration.clients.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob

val Context.appModule: AppModule
    get() = (applicationContext as PhotosApplication).appModule

@OptIn(UnstableApi::class)
class AppModule(
    context: Context
) {
    val settings = Settings(context.applicationContext, MainScope())

    val apiClient by lazy {
        ApiClient(
            debugMode = BuildConfig.DEBUG
        )
    }

    val scope = CoroutineScope(SupervisorJob())

    val albumGridState by lazy {
        createAlbumGridState(
            context = context,
            coroutineScope = scope,
            apiClient = apiClient
        )
    }

    val cache by lazy {
        SimpleCache(
            context.externalCacheDir ?: context.cacheDir,
            NoOpCacheEvictor(),
            StandaloneDatabaseProvider(context.applicationContext)
        )
    }

    val cloudProgressManager by lazy {
        ProgressManager(
            scope = scope,
            settings = settings.immich
        )
    }
}
