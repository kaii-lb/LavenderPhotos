package com.kaii.photos.datastore.preferences

import android.content.Context
import android.os.Environment
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kaii.photos.datastore.AlbumGroup
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.AlbumSortMode
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.datastore
import com.kaii.photos.helpers.baseInternalStorageDirectory
import com.kaii.photos.helpers.filename
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class SettingsAlbumsListImpl(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val v095ListKey = stringPreferencesKey("album_folder_path_list")
    private val v140ListKey = stringPreferencesKey("album_items_key")
    private val sortModeKey = intPreferencesKey("album_sort_mode")
    private val autoDetectAlbumsKey = booleanPreferencesKey("album_auto_detect")
    private val albumsKey = stringPreferencesKey("album_albums_key")
    private val albumsOrderKey = stringPreferencesKey("album_albums_order_key")
    private val albumGroupsKey = stringPreferencesKey("album_groups_key")

    val json = Json { ignoreUnknownKeys = true }

    fun add(list: List<AlbumType>) = scope.launch {
        context.datastore.edit { data ->
            val jsonString = data[albumsKey]
            val present = jsonString?.let { json.decodeFromString<List<AlbumType>>(jsonString) } ?: defaultAlbumsList

            val presentPaths = present.fastMapNotNull { (it as? AlbumType.Folder)?.paths }
            val presentIds = present.fastMap { it.id }

            present.toMutableList().apply {
                addAll(
                    list.filter {
                        when (it) {
                            is AlbumType.Folder -> it.paths !in presentPaths && it.name.isNotBlank()
                            else -> it.id !in presentIds && it.name.isNotBlank()
                        }
                    }
                )
            }.let { new ->
                data[albumsKey] = json.encodeToString(new)
            }
        }
    }

    private fun parsePreV083List(data: String): List<AlbumType> {
        if (!data.startsWith(",")) return emptyList()

        val list = data.split(",").distinct().toMutableList()
        list.remove("")
        list.remove(baseInternalStorageDirectory.removeSuffix("/"))

        return list.map { path ->
            AlbumType.Folder(
                id = Uuid.random().toString(),
                name = path.filename(),
                paths = setOf(path),
                pinned = false,
                immichId = null
            )
        }
    }

    private fun parsePreV095List(data: String): List<AlbumType> {
        val separator = "|-SEPARATOR-|"

        if (!data.startsWith(separator)) return emptyList()
        val list = data.split(separator).distinct().toMutableList()
        list.remove("")

        return list.map { path ->
            AlbumType.Folder(
                id = Uuid.random().toString(),
                name = path.filename(),
                paths = setOf(baseInternalStorageDirectory + path),
                pinned = false,
                immichId = null
            )
        }
    }

    private fun parsePreV140List(data: String): List<AlbumType> {
        val list = json.decodeFromString<List<AlbumInfo>>(data)

        return list.fastMap {
            if (it.isCustomAlbum) {
                AlbumType.Custom(
                    id = Uuid.random().toString(),
                    name = it.name,
                    pinned = it.isPinned,
                    immichId = null
                )
            } else {
                AlbumType.Folder(
                    id = Uuid.random().toString(),
                    name = it.name,
                    pinned = it.isPinned,
                    immichId = null,
                    paths = it.paths.map { path ->
                        if (!path.startsWith("/storage/")) baseInternalStorageDirectory + path.removePrefix("/")
                        else path
                    }.toSet()
                )
            }
        }
    }

    fun migrate() = scope.launch {
        context.datastore.data.first().let {
            if (it[v095ListKey] == null && it[v140ListKey] == null) return@launch

            var data = it[v095ListKey]
            var list = emptyList<AlbumType>()

            if (data != null) {
                list = parsePreV083List(data)
            }

            if (list.isEmpty() && data != null) {
                list = parsePreV095List(data)
            }

            if (list.isEmpty()) {
                data = it[v140ListKey] ?: return@launch
                list = parsePreV140List(data)
            }

            set(list)
        }

        context.datastore.edit {
            it.remove(v095ListKey)
            it.remove(v140ListKey)
        }
    }

    fun get() = context.datastore.data.map { data ->
        val jsonString = data[albumsKey] ?: jsonDefaultAlbumsList
        return@map json.decodeFromString<List<AlbumType>>(jsonString)
    }

    fun set(list: List<AlbumType>) = scope.launch {
        context.datastore.edit {
            it[albumsKey] = json.encodeToString(list)
        }
    }

    fun remove(albumId: String) = scope.launch {
        context.datastore.edit { data ->
            val list = data[albumsKey] ?: jsonDefaultAlbumsList
            val present = json.decodeFromString<List<AlbumType>>(list).toMutableList()

            present.removeIf {
                it.id == albumId
            }

            data[albumsKey] = json.encodeToString(present)
        }
    }

    fun removeAll(albumIds: List<String>) = scope.launch {
        context.datastore.edit { data ->
            val list = data[albumsKey] ?: jsonDefaultAlbumsList
            val present = json.decodeFromString<List<AlbumType>>(list).toMutableList()

            present.removeIf {
                it.id in albumIds
            }

            data[albumsKey] = json.encodeToString(present)
        }
    }

    fun edit(
        id: String,
        newInfo: AlbumType
    ) = scope.launch {
        context.datastore.edit { data ->
            val list = data[albumsKey] ?: jsonDefaultAlbumsList

            val present = json.decodeFromString<List<AlbumType>>(list).toMutableList()
            val index = present.indexOfFirst { it.id == id }

            present[index] =
                when (newInfo) {
                    is AlbumType.Folder -> newInfo.copy(id = id)
                    is AlbumType.Custom -> newInfo.copy(id = id)
                    is AlbumType.Cloud -> newInfo.copy(id = id)
                    else -> AlbumType.PlaceHolder
                }

            data[albumsKey] = json.encodeToString(present)
        }
    }

    fun reset() = scope.launch {
        context.datastore.edit {
            it[albumsKey] = jsonDefaultAlbumsList
        }
    }

    fun setSortMode(sortMode: AlbumSortMode) = scope.launch {
        context.datastore.edit {
            it[sortModeKey] = sortMode.ordinal
        }
    }

    fun getSortMode(): Flow<AlbumSortMode> = context.datastore.data.map {
        AlbumSortMode.entries[it[sortModeKey] ?: AlbumSortMode.LastModifiedDesc.ordinal]
    }

    fun getAutoDetect() = context.datastore.data.map {
        it[autoDetectAlbumsKey] != false
    }

    fun setAutoDetect(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[autoDetectAlbumsKey] = value
        }
    }

    fun getGroups() = context.datastore.data.map { data ->
        val string = data[albumGroupsKey] ?: "[]"

        json.decodeFromString<List<AlbumGroup>>(string)
    }

    fun addGroup(group: AlbumGroup) {
        scope.launch {
            context.datastore.edit { data ->
                val string = data[albumGroupsKey] ?: "[]"
                val present = json.decodeFromString<List<AlbumGroup>>(string).toMutableList()

                if (group.id !in present.fastMap { it.id }) {
                    present.add(group)
                }

                data[albumGroupsKey] = json.encodeToString(present)
            }
        }
    }

    fun editGroup(
        id: String,
        name: String? = null,
        pinned: Boolean? = null,
        albumIds: List<String>? = null
    ) {
        scope.launch {
            context.datastore.edit { data ->
                val string = data[albumGroupsKey] ?: "[]"
                val present = json.decodeFromString<List<AlbumGroup>>(string).toMutableList()

                val index = present.indexOfFirst { it.id == id }

                if (index == -1) return@edit

                val group = present[index]
                present[index] = group.copy(
                    name = name ?: group.name,
                    pinned = pinned ?: group.pinned,
                    albumIds = albumIds ?: group.albumIds
                )

                data[albumGroupsKey] = json.encodeToString(present)
            }
        }
    }

    fun removeGroup(id: String) {
        scope.launch {
            context.datastore.edit { data ->
                val string = data[albumGroupsKey] ?: ""
                val present = json.decodeFromString<List<AlbumGroup>>(string).toMutableList()

                present.removeIf { it.id == id }

                data[albumGroupsKey] = json.encodeToString(present)
            }
        }
    }

    fun getOrder() = context.datastore.data.map { data ->
        json.decodeFromString<List<String>>(data[albumsOrderKey] ?: "[]")
    }

    fun setOrder(order: List<String>) {
        scope.launch {
            context.datastore.edit { data ->
                data[albumsOrderKey] = json.encodeToString(order)
            }
        }
    }

    private val defaultAlbumsList =
        listOf<AlbumType>(
            AlbumType.Folder(
                id = Uuid.random().toString(),
                name = "Camera",
                paths = setOf(
                    File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                        "Camera"
                    ).absolutePath
                ),
                pinned = false,
                immichId = null
            ),
            AlbumType.Folder(
                id = Uuid.random().toString(),
                name = "Pictures",
                paths = setOf(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath
                ),
                pinned = false,
                immichId = null
            ),
            AlbumType.Folder(
                id = Uuid.random().toString(),
                name = "Download",
                paths = setOf(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
                ),
                pinned = false,
                immichId = null
            )
        )

    private val jsonDefaultAlbumsList = json.encodeToString(defaultAlbumsList)
}