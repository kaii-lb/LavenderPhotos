package com.kaii.photos.immich

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.kaii.photos.datastore.SQLiteQuery
import kotlinx.serialization.json.Json
import java.util.UUID

object SchedulingManager {
    fun scheduleUploadTask(
        context: Context,
        immichAlbumId: String,
        immichEndpointBase: String,
        immichBearerToken: String,
        mediastoreQuery: SQLiteQuery,
        lavenderAlbumId: Int
    ): UUID {
        val uploadWorkRequest: WorkRequest =
            OneTimeWorkRequestBuilder<UploadWorker>()
                .setInputData(
                    workDataOf(
                        UploadWorker.BEARER_TOKEN to immichBearerToken,
                        UploadWorker.ENDPOINT_BASE to immichEndpointBase,
                        UploadWorker.ALBUM_ID to immichAlbumId,
                        UploadWorker.MEDIASTORE_QUERY to Json.encodeToString(mediastoreQuery),
                        UploadWorker.LAVENDER_ALBUM_ID to lavenderAlbumId
                    )
                )
                .setConstraints(
                    Constraints(
                        requiredNetworkType = NetworkType.UNMETERED
                    )
                )
                .build()

        WorkManager
            .getInstance(context)
            .enqueue(uploadWorkRequest)

        return uploadWorkRequest.id
    }
}