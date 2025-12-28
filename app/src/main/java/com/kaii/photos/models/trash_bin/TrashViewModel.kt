package com.kaii.photos.models.trash_bin

import android.content.Context
import android.os.CancellationSignal
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.TrashDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration.Companion.seconds

class TrashViewModel(
    context: Context,
    sortMode: MediaItemSortMode,
    displayDateFormat: DisplayDateFormat
) : ViewModel() {
    private val cancellationSignal = CancellationSignal()
    private val mediaStoreDataSource =
        TrashDataSource(
            context = context,
            sortMode =
                if (sortMode == MediaItemSortMode.Disabled || sortMode == MediaItemSortMode.DisabledLastModified) sortMode
                else MediaItemSortMode.LastModified,
            cancellationSignal = cancellationSignal,
            displayDateFormat = displayDateFormat
        )

    val mediaFlow by lazy {
        getMediaDataFlow().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = 10.seconds.inWholeMilliseconds
            ),
            initialValue = emptyList()
        )
    }

    private fun getMediaDataFlow(): Flow<List<MediaStoreData>> {
        return mediaStoreDataSource.loadMediaStoreData().flowOn(Dispatchers.IO)
    }

    fun cancelMediaFlow() {
        cancellationSignal.cancel()
    }
}
