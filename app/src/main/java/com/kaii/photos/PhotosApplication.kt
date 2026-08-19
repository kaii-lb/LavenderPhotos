package com.kaii.photos

import android.app.Application
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingWorkPolicy
import com.kaii.photos.data.logging.CrashHandler
import com.kaii.photos.data.logging.LogWriter
import com.kaii.photos.data.providers.NetworkShareCapabilityProvider
import com.kaii.photos.database.sync.CloudSyncWorker
import com.kaii.photos.database.sync.SyncManager
import com.kaii.photos.database.sync.SyncWorker
import com.kaii.photos.di.AppModule
import com.kaii.photos.mediastore.MEDIA_STORE_FILE_URI
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltAndroidApp
class PhotosApplication : Application(), Configuration.Provider {
    companion object {
        lateinit var appModule: AppModule
    }

    @Inject lateinit var localAppModule: AppModule
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        appModule = localAppModule

        localAppModule.scope.launch(Dispatchers.IO) {
            Thread.setDefaultUncaughtExceptionHandler(
                CrashHandler(
                    context = applicationContext,
                    defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
                )
            )

            LogWriter.init(applicationContext)

            // try to migrate from an older datastore system on app startup
            localAppModule.settings.albums.migrate()
            localAppModule.settings.permissions.migrate()

            val shouldRecordLogs = localAppModule.settings.debugging.getRecordLogs().first()
            if (shouldRecordLogs) {
                localAppModule.logManager.startRecording()
            }

            val hasClearedCache = localAppModule.settings.versions.getHasClearedGlideCache().first()
            if (!hasClearedCache) {
                localAppModule.settings.storage.clearThumbnailCache()
                localAppModule.settings.versions.setHasClearedGlideCache(true)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                localAppModule.settings.permissions.setIsMediaManager(
                    MediaStore.canManageMedia(applicationContext)
                )
            }

            NetworkShareCapabilityProvider.init(applicationContext)

            delay(2000.milliseconds)
            CloudSyncWorker.enqueue(applicationContext)

            // run infinitely while app is open
            registerContentObserver()
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

        flow.debounce(
            timeout = 300.milliseconds
        ).collect {
            SyncWorker.start(applicationContext, ExistingWorkPolicy.APPEND_OR_REPLACE)
        }
    }
}