package com.kaii.photos.repositories

import android.content.Context
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.file_management.managers.GenericFileManager
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.grid_management.SelectionManager
import kotlin.reflect.KClass

open class RoomQueryParams(
    open val sortMode: MediaItemSortMode,
    open val format: DisplayDateFormat,
    open val info: ImmichBasicInfo
)

interface BaseRepo {
    val fileManager: GenericFileManager

    suspend fun getMediaCount(): Int
    suspend fun getMediaSize(): Long

    suspend fun getExifData(
        context: Context,
        media: MediaStoreData
    ) = fileManager.getExifData(context, media)

    fun allowedAlbumTypesFor(
        moving: Boolean
    ): List<KClass<out AlbumType>>

    suspend fun copy(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: AlbumType,
        preserveDate: Boolean,
        overrideDisplayName: ((displayName: String) -> String)?,
        onItemDone: (totaCount: Int) -> Unit
    ): Boolean {
        var count = 0

        return fileManager.copyItems(context, list, destination, preserveDate, overrideDisplayName) {
            count += 1
            onItemDone(count)
        }.size == list.size
    }

    suspend fun move(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        origin: AlbumType? = null,
        destination: AlbumType,
        preserveDate: Boolean,
        onItemDone: (totalCount: Int) -> Unit
    ): Boolean {
        var count = 0

        return fileManager.moveItems(context, list, destination, preserveDate, null, origin) {
            count += 1
            onItemDone(count)
        }
    }

    fun renameItem(
        context: Context,
        uri: String,
        newName: String
    ) = fileManager.renameItem(context, uri, newName)

    suspend fun renameAlbum(
        context: Context,
        newName: String
    )

    suspend fun setTrashed(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        trashed: Boolean,
        albumId: String?,
        immichId: String?,
        onItemDone: (totaCount: Int) -> Unit
    ) = fileManager.setTrashed(context, list, trashed, albumId, immichId, null, onItemDone)

    suspend fun delete(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ) = fileManager.permanentlyDelete(context, list)

    suspend fun setFavourite(
        context: Context,
        favourite: Boolean,
        list: List<SelectionManager.SelectedItem>
    ) = fileManager.setFavourite(context, favourite, list)

    suspend fun share(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ) = fileManager.share(context, list)

    suspend fun secure(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ) = fileManager.secure(context, list)

    suspend fun restore(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ) = fileManager.restore(context, list)
}