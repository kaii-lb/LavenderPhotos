package com.kaii.photos.models.trash_bin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.repositories.TrashRepository

class TrashViewModel(
    context: Context,
    sortMode: MediaItemSortMode,
    format: DisplayDateFormat
) : ViewModel() {
    private val repo = TrashRepository(
        context = context,
        scope = viewModelScope,
        sortMode = sortMode,
        format = format,
        separators = true
    )

    val mediaFlow = repo.mediaFlow

    override fun onCleared() {
        super.onCleared()
        repo.cancel()
    }

    fun update(
        sortMode: MediaItemSortMode? = null,
        format: DisplayDateFormat? = null,
        separators: Boolean? = null
    ) = repo.updateParams(sortMode, format, separators)

    fun cancel() = repo.cancel()

    fun deleteAll() = repo.deleteAll()
}
