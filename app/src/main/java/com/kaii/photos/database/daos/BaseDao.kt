package com.kaii.photos.database.daos

import androidx.paging.PagingSource
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.helpers.grid_management.SelectionManager

interface BaseDao {
    fun getPagedMediaDateTaken(paths: Set<String>): PagingSource<Int, MediaStoreData>

    fun getPagedMediaDateModified(paths: Set<String>): PagingSource<Int, MediaStoreData>

    fun mediaInDateTaken(timestamp: Long, paths: Set<String>, dateModified: Boolean): List<SelectionManager.SelectedItem>
    fun mediaInDateRange(timestamp: Long, dateModified: Boolean): List<SelectionManager.SelectedItem>
}