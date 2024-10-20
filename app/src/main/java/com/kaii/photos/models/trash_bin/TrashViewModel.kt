package com.kaii.photos.models.trash_bin

import android.content.Context
import android.os.CancellationSignal
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.TrashStoreDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

class TrashViewModel(context: Context) : ViewModel() {
    private val cancellationSignal = CancellationSignal()
    private val mediaStoreDataSource = TrashStoreDataSource(context, MediaItemSortMode.LastModified, cancellationSignal)

    val mediaFlow by lazy {
        getMediaDataFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    }

    private fun getMediaDataFlow(): Flow<List<MediaStoreData>> {
        return mediaStoreDataSource.loadMediaStoreData().flowOn(Dispatchers.IO)
    }

    fun cancelMediaSource() {
        cancellationSignal.cancel()
    }
}
