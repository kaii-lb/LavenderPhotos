package com.kaii.photos.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.kaii.photos.data.logging.LogManager
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.datastore.Settings
import com.kaii.photos.datastore.state.createAlbumGridState
import com.kaii.photos.file_management.sync.ProgressManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.kaii_lb.lavender.immichintegration.clients.ApiClient
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

@OptIn(UnstableApi::class)
class AppModule @Inject constructor(
    @ApplicationContext context: Context,
    val settings: Settings,
    val apiClient: ApiClient,
    @param:ApplicationScope val scope: CoroutineScope,
    val db: MediaDatabase,
    val logManager: LogManager
) {
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
