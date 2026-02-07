package com.kaii.photos

import android.app.Application
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.kaii.photos.database.sync.SyncWorker
import com.kaii.photos.mediastore.MediaDataSource.Companion.MEDIA_STORE_FILE_URI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PhotosApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // TODO: move to per-folder thing
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val contentObserver =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    scope.launch(Dispatchers.IO) {
                        runCatching {
                            WorkManager.getInstance(applicationContext)
                                .enqueueUniqueWork(
                                    SyncWorker::class.java.name,
                                    ExistingWorkPolicy.REPLACE,
                                    OneTimeWorkRequest.Builder(SyncWorker::class).build()
                                )
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