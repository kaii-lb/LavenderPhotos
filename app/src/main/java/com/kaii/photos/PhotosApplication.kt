package com.kaii.photos

import android.app.Application
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.kaii.photos.database.sync.CloudSyncWorker
import com.kaii.photos.database.sync.SyncManager
import com.kaii.photos.database.sync.SyncWorker
import com.kaii.photos.di.AppModule
import com.kaii.photos.mediastore.MEDIA_STORE_FILE_URI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

class PhotosApplication : Application() {
    companion object {
        lateinit var appModule: AppModule
    }

    override fun onCreate() {
        super.onCreate()

        CoroutineScope(Dispatchers.IO).launch {
            appModule = AppModule(applicationContext)

            // try to migrate from an older datastore system on app startup
            appModule.settings.albums.migrate()
            appModule.settings.permissions.migrate()

            registerContentObserver()

            delay(2000.milliseconds)
            CloudSyncWorker.enqueue(applicationContext)
        }
    }

    @OptIn(FlowPreview::class)
    private suspend fun registerContentObserver() = withContext(Dispatchers.IO) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val syncManager = SyncManager(applicationContext)

        val flow = callbackFlow {
            val contentObserver =
                object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean) {
                        super.onChange(selfChange)

                        scope.launch(Dispatchers.IO) {
                            runCatching {
                                // if the generation is more than 0 then FirstTimeSyncWorker has already run,
                                // and we can proceed to load media by deltas
                                if (syncManager.getGeneration() > 0L) {
                                    send(Unit)
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

            awaitClose {
                applicationContext.contentResolver.unregisterContentObserver(contentObserver)
            }
        }

        launch {
            flow.debounce(
                timeout = 300.milliseconds
            ).collect {
                WorkManager.getInstance(applicationContext)
                    .enqueueUniqueWork(
                        SyncWorker::class.java.name,
                        ExistingWorkPolicy.APPEND_OR_REPLACE,
                        OneTimeWorkRequest.Builder(SyncWorker::class).build()
                    )
            }
        }
    }
}