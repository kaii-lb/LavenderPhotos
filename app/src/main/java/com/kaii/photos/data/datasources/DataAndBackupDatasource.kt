package com.kaii.photos.data.datasources

import android.content.Context
import android.net.Uri
import android.util.JsonReader
import android.util.JsonWriter
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.entities.CustomItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source
import java.io.IOException

class DataAndBackupDatasource(
    private val context: Context,
    private val datastore: DataStore<Preferences>,
    private val customDao: CustomEntityDao
) {
    suspend fun save(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val outputStream = try {
            context.contentResolver.openOutputStream(uri)
                ?: throw IOException("Cannot open output stream! $uri")
        } catch (e: IOException) {
            Log.e(DataAndBackupDatasource::class.qualifiedName, "Failed to save JSON settings export! ${e.message}")
            e.printStackTrace()

            return@withContext false
        }

        val datastoreFile = context.preferencesDataStoreFile("settings")

        val data = PreferencesSerializer
            .readFrom(source = datastoreFile.source().buffer())
            .asMap()
            .mapNotNull { (key, value) ->
                if (key.name.startsWith("immich")) null
                else if (key.name.startsWith("permissions_password")) null
                else if (key.name.startsWith("is_media_manager")) null
                else if (value is Set<*>) key.name to "STRINGSET$value"
                else key.name to value.toString()
            }
            .toMap()

        JsonWriter(outputStream.bufferedWriter()).use { writer ->
            try {
                writer.beginObject()

                data.forEach { (key, value) ->
                    writer.name(key).value(value)
                }

                writer.name("custom_ids")
                writer.beginArray()

                var offset = 0
                var objects = customDao.getChunked(chunkSize = 200, offset = offset)
                while (objects.isNotEmpty()) {
                    objects.forEach { (id, album) ->
                        writer.beginObject()
                        writer.name("id").value(id)
                        writer.name("album").value(album)
                        writer.endObject()
                    }

                    offset += 200
                    objects = customDao.getChunked(chunkSize = 200, offset = offset)
                }

                writer.endArray()
                writer.endObject()
            } catch (e: IOException) {
                Log.e(DataAndBackupDatasource::class.qualifiedName, "Failed to write JSON settings export! ${e.message}")
                e.printStackTrace()
            }
        }

        return@withContext true
    }

    suspend fun load(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val inputStream = try {
            context.contentResolver.openInputStream(uri)
                ?: throw IOException("Cannot open input stream! $uri")
        } catch (e: IOException) {
            Log.e(DataAndBackupDatasource::class.qualifiedName, "Failed to load JSON settings export! ${e.message}")
            e.printStackTrace()

            return@withContext false
        }

        JsonReader(inputStream.bufferedReader()).use { reader ->
            try {
                reader.beginObject()

                if (!reader.hasNext()) return@withContext false

                datastore.edit { prefs ->
                    var name = reader.nextName()
                    while (name != "custom_ids") { // this NEEDS to haveNext()
                        val value = reader.nextString()

                        when {
                            value.toBooleanStrictOrNull() != null -> prefs[booleanPreferencesKey(name)] = value.toBoolean()
                            value.toIntOrNull() != null -> prefs[intPreferencesKey(name)] = value.toInt()
                            value.toLongOrNull() != null -> prefs[longPreferencesKey(name)] = value.toLong()
                            value.toFloatOrNull() != null -> prefs[floatPreferencesKey(name)] = value.toFloat()
                            value.toDoubleOrNull() != null -> prefs[doublePreferencesKey(name)] = value.toDouble()
                            value.startsWith("STRINGSET") -> prefs[stringSetPreferencesKey(name)] = stringToStringList(value.removePrefix("STRINGSET"))
                            else -> prefs[stringPreferencesKey(name)] = value
                        }

                        name = reader.nextName()
                    }
                }

                reader.beginArray()

                val chunk = mutableListOf<CustomItem>()

                while (reader.hasNext()) {
                    reader.beginObject()

                    reader.nextName()
                    val id = reader.nextLong()

                    reader.nextName()
                    val album = reader.nextString()

                    reader.endObject()

                    if (chunk.size >= 200) {
                        customDao.upsertAll(chunk)
                        chunk.clear()
                    }

                    chunk.add(CustomItem(id, album))
                }

                customDao.upsertAll(chunk)

                reader.endArray()
                reader.endObject()
            } catch (e: IOException) {
                Log.e(DataAndBackupDatasource::class.qualifiedName, "Failed to load JSON settings export! ${e.message}")
                e.printStackTrace()
            }
        }

        return@withContext true
    }

    private fun stringToStringList(string: String): Set<String> {
        return string.removePrefix("[")
            .removeSuffix("]")
            .split(", ")
            .toSet()
    }
}