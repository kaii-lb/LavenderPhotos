package com.kaii.photos.datastore

import android.content.Context
import android.graphics.Bitmap
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.util.UUID

/** Stores small, app-private copies; never changes or depends on the selected original. */
class AlbumCoverStore(private val context: Context) {
    private val key = stringPreferencesKey("album_custom_covers")

    fun get() = context.datastore.data.map { data ->
        decode(data[key])
    }.distinctUntilChanged()

    suspend fun save(albumId: String, bitmap: Bitmap) = withContext(Dispatchers.IO) {
        val directory = File(context.filesDir, DIRECTORY)
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Cannot create cover directory")

        // A new filename also invalidates Glide's cache when replacing a cover.
        val file = File(directory, "${UUID.randomUUID()}.jpg")
        var previous: String? = null
        try {
            file.outputStream().use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)) {
                    throw IOException("Cannot encode album cover")
                }
            }

            // Finish the tiny transaction even if its dialog leaves composition.
            withContext(NonCancellable) {
                context.datastore.edit { data ->
                    val covers = decode(data[key]).toMutableMap()
                    previous = covers.put(albumId, file.name)
                    data[key] = Json.encodeToString(covers)
                }
                previous?.let { File(directory, it).delete() }
            }
        } catch (error: Exception) {
            file.delete()
            throw error
        }
    }

    suspend fun remove(albumIds: Set<String>) = withContext(Dispatchers.IO + NonCancellable) {
        if (albumIds.isEmpty()) return@withContext
        val removed = mutableListOf<String>()
        context.datastore.edit { data ->
            val covers = decode(data[key]).toMutableMap()
            albumIds.forEach { id -> covers.remove(id)?.let(removed::add) }
            data[key] = Json.encodeToString(covers)
        }
        removed.forEach { File(File(context.filesDir, DIRECTORY), it).delete() }
    }

    companion object {
        const val DIRECTORY = "album_covers"

        private fun decode(value: String?): Map<String, String> =
            value?.let { Json.decodeFromString<Map<String, String>>(it) } ?: emptyMap()
    }
}
