package com.kaii.photos.mediastore

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import com.kaii.photos.helpers.MediaItemSortMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

abstract class MediaStoreDataSource
internal constructor(
    val context: Context,
    val neededPath: String,
    val sortBy: MediaItemSortMode,
    private val cancellationSignal: CancellationSignal
) {
    companion object {
        val MEDIA_STORE_FILE_URI: Uri =
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
    }

    open fun loadMediaStoreData(): Flow<List<MediaStoreData>> = callbackFlow {
        var localCancellationSignal = CancellationSignal()
        val mutex = Mutex()

        val contentObserver =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    launch(Dispatchers.IO) {
                        mutex.withLock {
                            localCancellationSignal.cancel()
                            localCancellationSignal = CancellationSignal()
                        }

                        runCatching {
                            trySend(query())
                        }
                    }
                }
            }

        context.contentResolver.registerContentObserver(
            MEDIA_STORE_FILE_URI,
            true,
            contentObserver
        )

        launch(Dispatchers.IO) {
            runCatching {
                trySend(query())
            }
        }

        cancellationSignal.setOnCancelListener {
            try {
                cancel("Cancelling MediaStoreDataSource $neededPath channel because of exit signal...")
            } catch (e: Throwable) {
                Log.e("MEDIA_STORE_DATASOURCE", e.toString())
            }
        }

        awaitClose {
            localCancellationSignal.cancel()
            context.contentResolver.unregisterContentObserver(contentObserver)
        }
    }.conflate()

    abstract fun query() : List<MediaStoreData>
}
