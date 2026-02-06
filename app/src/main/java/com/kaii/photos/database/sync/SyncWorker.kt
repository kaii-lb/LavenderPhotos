package com.kaii.photos.database.sync

import android.content.Context
import androidx.compose.ui.util.fastForEach
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.mediastore.SimpleMediaDataSource
import kotlinx.coroutines.flow.first

class SyncWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext = context, params = params) {
    override suspend fun doWork(): Result {
        // TODO: try a different approach as to not annihilate memory
        val source = SimpleMediaDataSource(
            context = context,
            paths = emptyList(),
            reversed = true
        ).query().associateBy { it.id }

        val db = MediaDatabase.getInstance(context)
        val destination = db.mediaDao().getAllMedia().first().associateBy { it.id }

        val addable = mutableListOf<MediaStoreData>()
        val deletable = mutableListOf<MediaStoreData>()


        source.keys.toList().chunked(1000).fastForEach { chunk ->
            chunk.fastForEach { key ->
                val value = source[key]!!

                if (key !in destination.keys || value.dateModified != destination[key]!!.dateModified) {
                    addable.add(value)
                }

                db.withTransaction {
                    if (addable.isNotEmpty()) db.mediaDao().upsertAll(addable)
                }

                addable.clear()
            }
        }

        destination.forEach { (key, value) ->
            if (key !in source.keys) {
                deletable.add(value)
            }
        }

        db.withTransaction {
            if (deletable.isNotEmpty()) db.mediaDao().deleteEntities(deletable)
        }

        return Result.success()
    }
}