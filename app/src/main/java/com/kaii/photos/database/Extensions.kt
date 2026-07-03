package com.kaii.photos.database

import androidx.compose.ui.util.fastMap
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.helpers.grid_management.SelectionManager

suspend fun MediaDao.getMediaByIds(
    list: List<SelectionManager.SelectedItem>
) =
    list.chunked(500).flatMap { chunk ->
        this.getMedia(ids = chunk.fastMap { it.id })
    }