package com.kaii.photos.database.daos

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.MapColumn
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Upsert
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.database.escapeLikeWildcards
import com.kaii.photos.datastore.AlbumSortMode
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.state.AlbumGridState
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.mediastore.signature

@Dao
interface SAFDao {
    @Query(value = "SELECT * FROM MEDIA WHERE id = :id")
    fun getById(id: Long): MediaStoreData?

    @Query(value = "SELECT * FROM media WHERE parentPath = :treeUri ORDER BY dateTaken DESC")
    fun getPagedMediaDateTaken(treeUri: String): PagingSource<Int, MediaStoreData>

    @Query(value = "SELECT * FROM media WHERE parentPath = :treeUri ORDER BY dateModified DESC")
    fun getPagedMediaDateModified(treeUri: String): PagingSource<Int, MediaStoreData>

    @RawQuery(observedEntities = [MediaStoreData::class])
    fun getPagedMediaByPathPrefixRaw(query: SupportSQLiteQuery): PagingSource<Int, MediaStoreData>

    fun getPagedMediaDateTakenByPathPrefix(treeUri: String): PagingSource<Int, MediaStoreData> =
        getPagedMediaByPathPrefixRaw(
            query = buildPathPrefixQuery(
                select = "SELECT *",
                treeUri = treeUri,
                isDateModified = false
            )
        )

    fun getPagedMediaDateModifiedByPathPrefix(treeUri: String): PagingSource<Int, MediaStoreData> =
        getPagedMediaByPathPrefixRaw(
            query = buildPathPrefixQuery(
                select = "SELECT *",
                treeUri = treeUri,
                isDateModified = true
            )
        )

    @Query(value = "SELECT COUNT(id) FROM media WHERE parentPath = :treeUri")
    fun countMediaInPaths(treeUri: String): Int

    @Query(value = "SELECT SUM(size) FROM media WHERE parentPath = :treeUri")
    fun mediaSize(treeUri: String): Long

    @RawQuery(observedEntities = [MediaStoreData::class])
    fun countMediaInPathsPrefixesRaw(query: SupportSQLiteQuery): Int

    fun countMediaInPathsPrefixes(treeUri: String): Int =
        countMediaInPathsPrefixesRaw(
            query = buildPathPrefixQuery(
                select = "SELECT COUNT(id)",
                treeUri = treeUri,
                isDateModified = true
            )
        )

    @RawQuery(observedEntities = [MediaStoreData::class])
    fun mediaSizeByPathPrefixRaw(query: SupportSQLiteQuery): Long

    @RawQuery(observedEntities = [MediaStoreData::class])
    fun mediaSizeByPathPrefixes(treeUri: String): Long =
        mediaSizeByPathPrefixRaw(
            query = buildPathPrefixQuery(
                select = "SELECT SUM(size)",
                treeUri = treeUri,
                isDateModified = true
            )
        )

    @Query(value = "SELECT id FROM media WHERE parentPath = :treeUri")
    fun getAllIdsIn(treeUri: String): List<Long>

    @Query(
        value = "SELECT id as keyId, id, uri, immichUrl, absolutePath, parentPath, " +
                "CASE WHEN type = 'Image' THEN 1 ELSE 0 END as isImage " +
                "from media WHERE " +
                "CASE WHEN :dateModified = 1 THEN dateModified ELSE dateTaken END " +
                "BETWEEN :timestamp AND :timestamp+86400 AND parentPath = :treeUri LIMIT 2000"
    )
    fun mediaInDateRange(timestamp: Long, treeUri: String, dateModified: Boolean): Map<
            @MapColumn(columnName = "keyId") Long,
            SelectionManager.SelectedItem>

    @RawQuery(observedEntities = [MediaStoreData::class])
    fun mediaInDateRangeNestedRaw(query: SupportSQLiteQuery): Map<@MapColumn(columnName = "keyId") Long, SelectionManager.SelectedItem>

    fun mediaInDateRangeNested(timestamp: Long, treeUri: String, dateModified: Boolean) =
        mediaInDateRangeNestedRaw(
            buildMediaInDateRangeQuery(
                treeUri = treeUri,
                timestamp = timestamp,
                isDateModified = dateModified
            )
        )

    @Query(
        value = """
        SELECT parentPath AS albumPath, media.* FROM media 
        WHERE albumPath IN (:treeUris) 
        GROUP BY albumPath 
        HAVING dateModified = MAX(dateModified)
        """
    )
    suspend fun getBatchFolderThumbnailsDateModified(treeUris: List<String>): Map<@MapColumn("albumPath") String, MediaStoreData>

    @Query(
        value = """
        SELECT parentPath AS albumPath, media.* FROM media 
        WHERE albumPath IN (:treeUris)
        GROUP BY albumPath
        HAVING dateTaken = MAX(dateTaken)
        """
    )
    suspend fun getBatchFolderThumbnailsDateTaken(treeUris: List<String>): Map<@MapColumn("albumPath") String, MediaStoreData>

    suspend fun getFolderThumbnails(
        folders: List<AlbumType.SAFFolder>,
        sortMode: MediaItemSortMode,
        albumSortMode: AlbumSortMode
    ): Map<String, AlbumGridState.Info.Thumbnail> {
        if (folders.isEmpty()) return emptyMap()

        val pathThumbnails = if (albumSortMode == AlbumSortMode.LastModified) {
            getBatchFolderThumbnailsDateModified(treeUris = folders.map { it.base64TreeUri })
        } else {
            getBatchFolderThumbnailsDateTaken(treeUris = folders.map { it.base64TreeUri })
        }

        val result = mutableMapOf<String, AlbumGridState.Info.Thumbnail>()

        folders.forEach { folder ->
            val thumbnail = pathThumbnails[folder.base64TreeUri] ?: MediaStoreData.dummyItem

            result[folder.id] = AlbumGridState.Info.Thumbnail(
                uri = thumbnail.uri,
                date = if (sortMode.isDateModified) thumbnail.dateModified else thumbnail.dateTaken,
                signature = thumbnail.signature(),
                albumId = folder.id,
                isGif = thumbnail.displayName.endsWith(".gif")
            )
        }

        val emptyMedia = MediaStoreData.dummyItem
        val missing = folders.filter {
            it.id !in result
        }.associate { album ->
            album.id to AlbumGridState.Info.Thumbnail(
                uri = emptyMedia.uri,
                date = emptyMedia.dateTaken,
                signature = emptyMedia.signature(),
                albumId = album.id,
                isGif = false
            )
        }

        return result + missing
    }

    @Upsert
    suspend fun upsertAll(items: List<MediaStoreData>)

    @Query(value = "DELETE FROM media WHERE id IN (:ids)")
    suspend fun deleteAll(ids: List<Long>)

    private fun buildPathPrefixQuery(
        select: String,
        treeUri: String,
        isDateModified: Boolean
    ): SupportSQLiteQuery {
        val treeUri = treeUri.trimEnd('/')
        val escapedTreeUri = treeUri.escapeLikeWildcards()

        val where = "(parentPath = ? OR parentPath LIKE ? ESCAPE '\\')"
        val args = listOf(treeUri, "$escapedTreeUri/%")

        val orderByColumn = if (isDateModified) "dateModified" else "dateTaken"
        val sql = "$select FROM media WHERE $where ORDER BY $orderByColumn DESC"

        return SimpleSQLiteQuery(sql, args.toTypedArray())
    }

    private fun buildMediaInDateRangeQuery(
        treeUri: String,
        timestamp: Long,
        isDateModified: Boolean
    ): SupportSQLiteQuery {
        val orderByColumn = if (isDateModified) "dateModified" else "dateTaken"
        val dateFilter = "$orderByColumn BETWEEN $timestamp AND $timestamp+86400"
        val locationFilter = "(parentPath = ? OR parentPath LIKE ? ESCAPE '\\')"

        val sql = "SELECT id as keyId, id, uri, immichUrl, absolutePath, parentPath, " +
                "CASE WHEN type = 'Image' THEN 1 ELSE 0 END as isImage " +
                "FROM media WHERE $dateFilter AND $locationFilter LIMIT 2000"

        val treeUri = treeUri.trimEnd('/')
        val escapedTreeUri = treeUri.escapeLikeWildcards()
        val args = listOf(treeUri, "$escapedTreeUri/%")

        return SimpleSQLiteQuery(sql, args.toTypedArray())
    }
}