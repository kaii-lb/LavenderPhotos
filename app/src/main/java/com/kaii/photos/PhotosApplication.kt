package com.kaii.photos

import android.app.Application
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.kaii.photos.database.sync.SyncManager
import com.kaii.photos.database.sync.SyncWorker
import com.kaii.photos.di.AppModule
import com.kaii.photos.mediastore.MEDIA_STORE_FILE_URI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PhotosApplication : Application() {
    lateinit var appModule: AppModule

    override fun onCreate() {
        super.onCreate()

        appModule = AppModule(applicationContext)

        // try to migrate from an older album system on app startup
        appModule.settings.albums.migrate()

        appModule.scope.launch(Dispatchers.IO) {
            registerContentObserver()
        }
    }

    private suspend fun registerContentObserver() = withContext(Dispatchers.IO) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val syncManager = SyncManager(applicationContext)

        var instanceCount = 0

        val contentObserver =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)

                    if (instanceCount >= 10) return

                    scope.launch(Dispatchers.IO) {
                        runCatching {
                            // if the generation is more than 0 then FirstTimeSyncWorker has already run,
                            // and we can proceed to load media by deltas
                            if (syncManager.getGeneration() > 0L) {
                                instanceCount += 1
                                WorkManager.getInstance(applicationContext)
                                    .enqueueUniqueWork(
                                        SyncWorker::class.java.name,
                                        ExistingWorkPolicy.REPLACE,
                                        OneTimeWorkRequest.Builder(SyncWorker::class).build()
                                    ).result.addListener(
                                        {
                                            instanceCount -= 1
                                        },
                                        mainExecutor
                                    )
                            }
                        }
                    }
                }
            }

        applicationContext.contentResolver.registerContentObserver(
            MEDIA_STORE_FILE_URI,
            true,
            contentObserver
        )
    }
}