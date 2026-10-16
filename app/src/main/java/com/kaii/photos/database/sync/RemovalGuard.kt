package com.kaii.photos.database.sync

import android.content.Context
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.util.Log
import com.kaii.photos.database.daos.MediaDao
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

object RemovalGuard {
    enum class Decision { Remove, Keep }

    val VOLUME_GRACE_PERIOD: Duration = 1.days

    private val removableVolumeRoot = Regex("^(/storage/[^/]+)/")

    fun volumeRootOf(path: String): String? {
        val root = removableVolumeRoot.find(path)?.groupValues?.get(1) ?: return null

        return when (root.removePrefix("/storage/")) {
            "emulated", "self" -> null
            else -> root
        }
    }

    fun decide(
        absolutePath: String,
        primaryRoot: String?,
        usableRemovableRoots: Collection<String>,
        fileExists: (String) -> Boolean,
        unavailableFor: (volumeRoot: String) -> Duration = { Duration.ZERO },
        gracePeriod: Duration = VOLUME_GRACE_PERIOD
    ): Decision {
        if (primaryRoot != null && absolutePath.isUnder(primaryRoot)) return Decision.Remove

        val usableRoot = usableRemovableRoots.firstOrNull { absolutePath.isUnder(it) }
        if (usableRoot != null) {
            return when {
                !fileExists(absolutePath) -> Decision.Remove
                isExcludedFromMediaStore(absolutePath, usableRoot, fileExists) -> Decision.Remove
                else -> Decision.Keep
            }
        }

        val unavailableRoot = volumeRootOf(absolutePath) ?: return Decision.Keep

        return if (unavailableFor(unavailableRoot) >= gracePeriod) Decision.Remove else Decision.Keep
    }

    fun isExcludedFromMediaStore(
        absolutePath: String,
        volumeRoot: String,
        fileExists: (String) -> Boolean
    ): Boolean {
        val segments = absolutePath.removePrefix(volumeRoot.trimEnd('/') + "/").split('/')
        if (segments.last().startsWith(".")) return true

        val dirs = segments.dropLast(1)
        var current = volumeRoot.trimEnd('/')

        dirs.forEachIndexed { index, name ->
            current += "/$name"

            if (name.startsWith(".")) return true

            val dirSegments = dirs.take(index + 1)
            if (!isAlwaysVisible(dirSegments) && fileExists("$current/.nomedia")) return true
        }

        return false
    }

    private fun isAlwaysVisible(dirSegments: List<String>): Boolean {
        if (dirSegments.size == 1) return true
        if (dirSegments.joinToString("/").equals("DCIM/Camera", ignoreCase = true)) return true

        return dirSegments.size <= 2 && dirSegments.last().equals("Screenshots", ignoreCase = true)
    }

    suspend fun filterSafeRemovals(
        context: Context,
        dao: MediaDao,
        candidates: Collection<Long>
    ): List<Long> {
        val syncManager = SyncManager(context.applicationContext)
        val storageManager = context.getSystemService(StorageManager::class.java)
        val listedVolumes = MediaStore.getExternalVolumeNames(context)
        val now = System.currentTimeMillis()

        val primaryRoot = storageManager.primaryStorageVolume.directory?.absolutePath

        val usableRemovableRoots = storageManager.storageVolumes
            .asSequence()
            .filter { !it.isPrimary }
            .filter { it.state == Environment.MEDIA_MOUNTED || it.state == Environment.MEDIA_MOUNTED_READ_ONLY }
            .filter { volume -> volume.mediaStoreVolumeName?.let { it in listedVolumes } == true }
            .mapNotNull { it.directory?.absolutePath }
            .toList()

        usableRemovableRoots.forEach { syncManager.clearVolumeMissing(it) }

        val mediaStoreCandidates = candidates.filter { it >= 0 }
        if (mediaStoreCandidates.isEmpty()) return emptyList()

        val unavailableForCache = mutableMapOf<String, Duration>()
        val unavailableFor = { root: String ->
            unavailableForCache.getOrPut(root) {
                val since = syncManager.getVolumeMissingSince(root)

                if (since == null) {
                    syncManager.markVolumeMissing(root, now)
                    Duration.ZERO
                } else {
                    (now - since).coerceAtLeast(0L).milliseconds
                }
            }
        }

        val safe = mutableListOf<Long>()
        var kept = 0

        mediaStoreCandidates.chunked(500).forEach { chunk ->
            dao.getIdsAndPaths(chunk).forEach { row ->
                val decision = decide(
                    absolutePath = row.absolutePath,
                    primaryRoot = primaryRoot,
                    usableRemovableRoots = usableRemovableRoots,
                    fileExists = { File(it).exists() },
                    unavailableFor = unavailableFor
                )

                if (decision == Decision.Remove) safe.add(row.id) else kept += 1
            }
        }

        if (kept > 0) {
            Log.d(RemovalGuard::class.qualifiedName, "Kept $kept item(s) that are missing from MediaStore but whose volume is unavailable or whose file still exists")
        }

        return safe
    }

    private fun String.isUnder(root: String) = startsWith(root.trimEnd('/') + "/")
}
