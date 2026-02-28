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

class PhotosApplication : Application() {
    val appModule = AppModule(applicationContext)

    override fun onCreate() {
        super.onCreate()

        registerContentObserver()
    }

    private fun registerContentObserver() {
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
                            // if its empty then the app hasn't finished startup processing, and we shouldn't mess with that
                            // if it *is* empty, and we have finished startup processing, then another SyncWorker will be
                            // launching by the app, and the dao won't be empty anymore
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