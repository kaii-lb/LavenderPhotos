package com.kaii.photos.database.sync

import android.content.Context
import android.provider.MediaStore
import androidx.core.content.edit

private const val LEGACY_KEY = "last_sync_generation"
private const val VOLUME_PREFIX = "gen_"
private const val MISSING_PREFIX = "volume_missing_since_"

class SyncManager(
    context: Context
) {
    private val prefs = context.getSharedPreferences("media_sync_prefs", Context.MODE_PRIVATE)

    fun getGeneration(): Long {
        val volumeMax = prefs.all
            .filter { it.key.startsWith(VOLUME_PREFIX) }
            .maxOfOrNull { (it.value as? Long) ?: 0L } ?: 0L

        return maxOf(prefs.getLong(LEGACY_KEY, 0L), volumeMax)
    }

    /** Passing 0 (or less) resets every volume, which forces a full resync */
    fun setGeneration(gen: Long) {
        prefs.edit {
            if (gen <= 0L) {
                prefs.all.keys
                    .filter { it.startsWith(VOLUME_PREFIX) }
                    .forEach { remove(it) }
            }

            putLong(LEGACY_KEY, gen)
        }
    }

    fun getVolumeGeneration(volumeName: String): Long {
        val fallback =
            if (volumeName == MediaStore.VOLUME_EXTERNAL_PRIMARY) prefs.getLong(LEGACY_KEY, 0L)
            else 0L

        return prefs.getLong(VOLUME_PREFIX + volumeName, fallback)
    }

    fun setVolumeGeneration(volumeName: String, gen: Long) {
        prefs.edit {
            putLong(VOLUME_PREFIX + volumeName, gen)
        }
    }

    fun getVolumeMissingSince(volumeRoot: String): Long? {
        val value = prefs.getLong(MISSING_PREFIX + volumeRoot, -1L)

        return if (value >= 0L) value else null
    }

    fun markVolumeMissing(volumeRoot: String, sinceMillis: Long) {
        prefs.edit {
            putLong(MISSING_PREFIX + volumeRoot, sinceMillis)
        }
    }

    fun clearVolumeMissing(volumeRoot: String) {
        val key = MISSING_PREFIX + volumeRoot
        if (!prefs.contains(key)) return

        prefs.edit {
            remove(key)
        }
    }
}
