package com.kaii.photos.models.tag_page

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.database.entities.Tag
import com.kaii.photos.repositories.TagRepository
import com.kaii.photos.repositories.TaggedItemsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TagViewModel(
    private val mediaId: Long,
    context: Context
) : ViewModel() {
    private val tagRepo = TagRepository(
        dao = MediaDatabase.getInstance(context.applicationContext).tagDao()
    )

    private val itemsRepo = TaggedItemsRepository(
        dao = MediaDatabase.getInstance(context.applicationContext).taggedItemsDao()
    )

    val tags = tagRepo.allTags.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = emptyList()
    )

    val appliedTags = tagRepo.getAppliedTags(mediaId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = emptyList()
    )

    private val _item = MutableStateFlow(MediaStoreData.dummyItem)
    val item = _item.asStateFlow()

    init {
        viewModelScope.launch {
            _item.value = itemsRepo.getItem(mediaId) ?: MediaStoreData.dummyItem
        }
    }

    fun insertTag(tag: Tag) {
        viewModelScope.launch {
            val tagId = tagRepo.insertTag(tag)
            itemsRepo.addTag(
                tag = tagRepo.get(tagId),
                mediaId = mediaId
            )
        }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            tagRepo.deleteTag(tag)
        }
    }

    fun toggleTag(tag: Tag) {
        viewModelScope.launch {
            if (tag in appliedTags.value) {
                itemsRepo.removeTag(tag, mediaId)
            } else {
                itemsRepo.addTag(tag, mediaId)
            }
        }
    }
}