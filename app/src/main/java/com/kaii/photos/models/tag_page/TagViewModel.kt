package com.kaii.photos.models.tag_page

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.Tag
import com.kaii.photos.repositories.TagRepository
import com.kaii.photos.repositories.TaggedItemsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TagViewModel(
    context: Context
) : ViewModel() {
    private val mediaId = MutableStateFlow(0L)

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

    @OptIn(ExperimentalCoroutinesApi::class)
    val appliedTags = mediaId.flatMapLatest {
        tagRepo.getAppliedTags(it)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = emptyList()
    )

    fun setMediaId(id: Long) {
        mediaId.value = id
    }

    fun insertTag(name: String) {
        viewModelScope.launch {
            val tagId = tagRepo.insertTag(name)
            if (tagId < 0) return@launch

            itemsRepo.addTag(
                tag = tagRepo.get(tagId),
                mediaId = mediaId.value
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
                itemsRepo.removeTag(tag, mediaId.value)
            } else {
                itemsRepo.addTag(tag, mediaId.value)
            }
        }
    }
}