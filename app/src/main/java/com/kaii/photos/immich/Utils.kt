package com.kaii.photos.immich

import android.os.CancellationSignal
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import com.kaii.photos.datastore.ImmichBackupMedia
import com.kaii.photos.helpers.calculateSha1Checksum
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import java.io.File

suspend fun getImmichBackupMedia(
    groupedMedia: List<MediaStoreData>,
    cancellationSignal: CancellationSignal
): List<ImmichBackupMedia> {
    val nonSectioned = groupedMedia.fastFilter { it.type != MediaType.Section }

    val checksums = calculateSha1Checksum(
        files = nonSectioned
            .groupBy {
                it.size
            }
            .filter {
                it.value.size > 1
            }
            .flatMap {
                it.value
            }.fastMap {
                File(it.absolutePath)
            },
        cancellationSignal = cancellationSignal
    )

    return nonSectioned
        .fastMap {
            ImmichBackupMedia(
                deviceAssetId = "${it.displayName}-${it.size}",
                absolutePath = it.absolutePath,
                checksum = checksums[it.absolutePath] ?: ""
            )
        }
}