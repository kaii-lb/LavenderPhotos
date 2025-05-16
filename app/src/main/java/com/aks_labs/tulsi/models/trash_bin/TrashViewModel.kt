package com.aks_labs.tulsi.models.trash_bin

import android.content.Context
import android.os.CancellationSignal
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aks_labs.tulsi.helpers.MediaItemSortMode
import com.aks_labs.tulsi.mediastore.MediaStoreData
import com.aks_labs.tulsi.mediastore.TrashStoreDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

class TrashViewModel(context: Context) : ViewModel() {
    private val cancellationSignal = CancellationSignal()
    private val mediaStoreDataSource =
        TrashStoreDataSource(
            context = context,
            sortBy = MediaItemSortMode.LastModified,
            cancellationSignal = cancellationSignal
        )

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


