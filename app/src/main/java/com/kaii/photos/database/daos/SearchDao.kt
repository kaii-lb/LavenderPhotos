package com.kaii.photos.database.daos

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.kaii.photos.database.entities.ExifData
import com.kaii.photos.database.entities.MediaStoreData

@Dao
interface SearchDao {
    @Query(value = "SELECT * FROM media WHERE displayName LIKE :query " +
            "ORDER BY CASE WHEN :dateModified = 1 THEN dateModified ELSE dateTaken END DESC")
    fun searchByName(query: String, dateModified: Boolean): PagingSource<Int, MediaStoreData>

    @Query(value = "SELECT * FROM media WHERE strftime('%m', dateTaken, 'unixepoch') = :month " +
            "ORDER BY CASE WHEN :dateModified = 1 THEN dateModified ELSE dateTaken END DESC")
    fun searchByMonth(month: String, dateModified: Boolean): PagingSource<Int, MediaStoreData>

    @Query(value = "SELECT * FROM media WHERE strftime('%u', dateTaken, 'unixepoch') = :day " +
            "ORDER BY CASE WHEN :dateModified = 1 THEN dateModified ELSE dateTaken END DESC")
    fun searchByDay(day: String, dateModified: Boolean): PagingSource<Int, MediaStoreData>

    @Query(value = "SELECT * FROM media WHERE dateTaken >= :startDate AND dateTaken < :endDate " +
            "ORDER BY CASE WHEN :dateModified = 1 THEN dateModified ELSE dateTaken END DESC")
    fun searchBetweenDates(startDate: Long, endDate: Long, dateModified: Boolean): PagingSource<Int, MediaStoreData>

    @Query(value = "SELECT * FROM media WHERE dateTaken >= :startDate AND dateTaken < :endDate " +
            "AND strftime('%u', dateTaken, 'unixepoch') = :day " +
            "ORDER BY CASE WHEN :dateModified = 1 THEN dateModified ELSE dateTaken END DESC")
    fun searchForDaysInMonthYear(startDate: Long, endDate: Long, day: String, dateModified: Boolean): PagingSource<Int, MediaStoreData>

    @Query(value = "SELECT * FROM media ORDER BY " +
            "CASE WHEN :dateModified = 1 THEN dateModified ELSE dateTaken END DESC")
    fun getAll(dateModified: Boolean): PagingSource<Int, MediaStoreData>

    @Transaction
    @Query(value = "SELECT * FROM media_exif_data WHERE mediaId = :id")
    suspend fun getExifData(id: Long): ExifData?
}