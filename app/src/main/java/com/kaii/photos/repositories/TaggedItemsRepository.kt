package com.kaii.photos.repositories

import com.kaii.photos.database.daos.TaggedItemsDao
import com.kaii.photos.database.entities.Tag
import com.kaii.photos.database.entities.TaggedItem

class TaggedItemsRepository(
    private val dao: TaggedItemsDao
) {
    suspend fun addTag(tag: Tag, mediaId: Long) =
        dao.upsert(
            TaggedItem(
                tag = tag.id,
                mediaId = mediaId
            )
        )

    suspend fun removeTag(tag: Tag, mediaId: Long) =
        dao.remove(
            TaggedItem(
                tag = tag.id,
                mediaId = mediaId
            )
        )

    suspend fun getItem(mediaId: Long) = dao.getItem(id = mediaId)
}