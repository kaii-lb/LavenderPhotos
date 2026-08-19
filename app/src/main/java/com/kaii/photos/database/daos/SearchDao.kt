package com.kaii.photos.database.daos

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.kaii.photos.database.entities.MediaStoreData

@Dao
interface SearchDao {
    @Query(value = "SELECT * FROM media " +
            "WHERE displayName LIKE :query " +
            "AND isSAF = 0 " +
            "ORDER BY CASE WHEN :dateModified = 1 THEN dateModified ELSE dateTaken END DESC"
    )
    fun searchByName(query: String, dateModified: Boolean): PagingSource<Int, MediaStoreData>

    @Query(value = "SELECT * FROM media " +
            "WHERE strftime('%m', dateTaken, 'unixepoch') = :month " +
            "AND isSAF = 0 " +
            "ORDER BY CASE WHEN :dateModified = 1 THEN dateModified ELSE dateTaken END DESC"
    )
    fun searchByMonth(month: String, dateModified: Boolean): PagingSource<Int, MediaStoreData>

    @Query(value = "SELECT * FROM media " +
            "WHERE strftime('%w', dateTaken, 'unixepoch') = :day " +
            "AND isSAF = 0 " +
            "ORDER BY CASE WHEN :dateModified = 1 THEN dateModified ELSE dateTaken END DESC"
    )
    fun searchByDay(day: String, dateModified: Boolean): PagingSource<Int, MediaStoreData>

    @Query(value = "SELECT * FROM media " +
            "WHERE dateTaken >= :startDate AND dateTaken < :endDate " +
            "AND isSAF = 0 " +
            "ORDER BY CASE WHEN :dateModified = 1 THEN dateModified ELSE dateTaken END DESC"
    )
    fun searchBetweenDates(startDate: Long, endDate: Long, dateModified: Boolean): PagingSource<Int, MediaStoreData>

    @Query(value = "SELECT * FROM media " +
            "WHERE dateTaken >= :startDate AND dateTaken < :endDate " +
            "AND isSAF = 0 " +
            "AND strftime('%u', dateTaken, 'unixepoch') = :day " +
            "ORDER BY CASE WHEN :dateModified = 1 THEN dateModified ELSE dateTaken END DESC"
    )
    fun searchForDaysInMonthYear(startDate: Long, endDate: Long, day: String, dateModified: Boolean): PagingSource<Int, MediaStoreData>

    @Query(value = "SELECT * FROM media " +
            "WHERE isSAF = 0 " +
            "ORDER BY CASE WHEN :dateModified = 1 THEN dateModified ELSE dateTaken END DESC")
    fun getAll(dateModified: Boolean): PagingSource<Int, MediaStoreData>
}