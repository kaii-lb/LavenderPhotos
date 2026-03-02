package com.kaii.photos.repositories

import androidx.compose.ui.graphics.Color
import com.kaii.photos.database.daos.TagDao
import com.kaii.photos.database.entities.Tag
import com.kaii.photos.helpers.editing.random

class TagRepository(
    private val dao: TagDao
) {
    val allTags = dao.getAll()

    fun getAppliedTags(mediaIds: List<Long>) =
        dao.getAppliedToMedia(ids = mediaIds, idCount = mediaIds.size)

    suspend fun insertTag(name: String) =
        if (name.isNotBlank()) {
            dao.insertAndGet(
                Tag(
                    name = name,
                    description = "",
                    color = Color.random()
                )
            ).toInt()
        } else -1

    suspend fun deleteTag(tag: Tag) = dao.delete(tag)

    suspend fun get(id: Int) = dao.get(id)
}