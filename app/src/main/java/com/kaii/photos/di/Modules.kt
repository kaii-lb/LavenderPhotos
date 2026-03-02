package com.kaii.photos.di

import android.content.Context
import com.kaii.photos.PhotosApplication
import com.kaii.photos.datastore.Settings
import io.github.kaii_lb.lavender.immichintegration.clients.ApiClient
import kotlinx.coroutines.MainScope

val Context.appModule: AppModule
    get() = (applicationContext as PhotosApplication).appModule

class AppModule(
    context: Context
) {
    val settings = Settings(context.applicationContext, MainScope())

    val apiClient = ApiClient()

    // TODO: check if this is the best way or if its even okay to do
    val scope = MainScope()
}