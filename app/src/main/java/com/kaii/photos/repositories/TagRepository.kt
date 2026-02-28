package com.kaii.photos.repositories

import com.kaii.photos.database.daos.TagDao
import com.kaii.photos.database.entities.Tag

class TagRepository(
    private val dao: TagDao
) {
    val allTags = dao.getAll()

    fun getAppliedTags(mediaId: Long) = dao.getAppliedToMedia(id = mediaId)

    suspend fun insertTag(tag: Tag) = dao.insertAndGet(tag).toInt()

    suspend fun deleteTag(tag: Tag) = dao.delete(tag)

    suspend fun get(id: Int) = dao.get(id)
}