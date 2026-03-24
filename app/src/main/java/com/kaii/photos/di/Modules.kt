package com.kaii.photos.di

import android.content.Context
import com.kaii.photos.PhotosApplication
import com.kaii.photos.datastore.Settings
import com.kaii.photos.datastore.state.createAlbumGridState
import io.github.kaii_lb.lavender.immichintegration.clients.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob

val Context.appModule: AppModule
    get() = (applicationContext as PhotosApplication).appModule

class AppModule(
    context: Context
) {
    val settings = Settings(context.applicationContext, MainScope())

    val apiClient by lazy {
        ApiClient()
    }

    val scope = CoroutineScope(SupervisorJob())

    val albumGridState by lazy {
        createAlbumGridState(
            context = context,
            coroutineScope = scope,
            apiClient = apiClient
        )
    }
}