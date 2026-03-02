package com.kaii.photos.repositories

import com.kaii.photos.database.daos.TaggedItemsDao
import com.kaii.photos.database.entities.Tag
import com.kaii.photos.database.entities.TaggedItem

class TaggedItemsRepository(
    private val dao: TaggedItemsDao
) {
    suspend fun addTag(tag: Tag, mediaId: Long) {
        if (mediaId > 0L) {
            dao.upsert(
                TaggedItem(
                    tag = tag.id,
                    mediaId = mediaId
                )
            )
        }
    }

    suspend fun removeTag(tag: Tag, mediaId: Long) {
        if (mediaId > 0L) {
            dao.remove(
                TaggedItem(
                    tag = tag.id,
                    mediaId = mediaId
                )
            )
        }
    }
}