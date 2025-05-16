package com.aks_labs.tulsi.models.multi_album

import android.content.Context
import android.os.CancellationSignal
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aks_labs.tulsi.MainActivity.Companion.mainViewModel
import com.aks_labs.tulsi.datastore.AlbumInfo
import com.aks_labs.tulsi.datastore.MainPhotosView
import com.aks_labs.tulsi.helpers.MediaItemSortMode
import com.aks_labs.tulsi.helpers.SectionItem
import com.aks_labs.tulsi.mediastore.MediaStoreData
import com.aks_labs.tulsi.mediastore.MediaType
import com.aks_labs.tulsi.mediastore.MultiAlbumDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

private const val TAG = "MULTI_ALBUM_VIEW_MODEL"

class MultiAlbumViewModel(
    context: Context,
    var albumInfo: AlbumInfo,
    var sortBy: MediaItemSortMode
) : ViewModel() {
    private var cancellationSignal = CancellationSignal()
    private var isGridView = false
    private val mediaStoreDataSource = mutableStateOf(initDataSource(context, albumInfo, sortBy))

    // Add a mutable state flow for direct media updates
    private val _mediaFlow = mutableStateOf<List<MediaStoreData>>(emptyList())

    val mediaFlow by derivedStateOf {
        getMediaDataFlow().value.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            emptyList()
        )
    }

    private fun getMediaDataFlow(): State<Flow<List<MediaStoreData>>> = derivedStateOf {
        mediaStoreDataSource.value.loadMediaStoreData().flowOn(Dispatchers.IO)
    }

    fun cancelMediaFlow() = cancellationSignal.cancel()

    fun reinitDataSource(
        context: Context,
        album: AlbumInfo,
        sortMode: MediaItemSortMode = sortBy,
        gridView: Boolean = isGridView
    ) {
        sortBy = sortMode
        isGridView = gridView
        if (album == albumInfo) return

        cancelMediaFlow()
        cancellationSignal = CancellationSignal()
        mediaStoreDataSource.value = initDataSource(context, album, sortBy)
    }

    fun changeSortMode(
        context: Context,
        sortMode: MediaItemSortMode
    ) {
        sortBy = sortMode

        cancelMediaFlow()
        cancellationSignal = CancellationSignal()
        mediaStoreDataSource.value = initDataSource(context, albumInfo, sortBy)
    }

    fun toggleGridViewMode(context: Context) {
        isGridView = !isGridView

        cancelMediaFlow()
        cancellationSignal = CancellationSignal()
        mediaStoreDataSource.value = initDataSource(context, albumInfo, sortBy)
    }

    fun setGridViewMode(context: Context, gridView: Boolean) {
        if (isGridView == gridView) return

        isGridView = gridView

        cancelMediaFlow()
        cancellationSignal = CancellationSignal()
        mediaStoreDataSource.value = initDataSource(context, albumInfo, sortBy)
    }

    fun getGridViewMode(): Boolean {
        return isGridView
    }

    // Set grouped media directly for immediate UI updates
    fun setGroupedMedia(media: List<MediaStoreData>) {
        _mediaFlow.value = media
    }

    private fun initDataSource(
        context: Context,
        album: AlbumInfo,
        sortBy: MediaItemSortMode
    ) = run {
        val query = mainViewModel.settings.MainPhotosView.getSQLiteQuery(album.paths)
        Log.d(TAG, "query is $query")

        albumInfo = album
        this.sortBy = sortBy

        MultiAlbumDataSource(
            context = context,
            queryString = query,
            sortBy = sortBy,
            cancellationSignal = cancellationSignal,
            isGridView = isGridView
        )
    }
}

/** Groups photos by date or returns a simple grid */
fun groupPhotosBy(
    media: List<MediaStoreData>,
    sortBy: MediaItemSortMode = MediaItemSortMode.DateTaken,
    isGridView: Boolean = false
): List<MediaStoreData> {
    if (media.isEmpty()) return emptyList()

    val sortedList =
        media.sortedByDescending { item ->
            when (sortBy) {
                MediaItemSortMode.DateTaken, MediaItemSortMode.MonthTaken, MediaItemSortMode.Disabled -> {
                    item.dateTaken
                }

                MediaItemSortMode.LastModified -> {
                    item.dateModified
                }
            }
        }

    // If grid view mode is enabled or sorting is disabled, return the sorted list without date sections
    if (isGridView || sortBy == MediaItemSortMode.Disabled) {
        // Create a single section for all items to maintain consistent section data
        val allItemsSection = SectionItem(date = 0L, childCount = sortedList.size)
        return sortedList.map { item ->
            item.section = allItemsSection
            item
        }
    }

    // Date-grouped view mode
    val grouped = sortedList.groupBy { item ->
        when (sortBy) {
            MediaItemSortMode.DateTaken -> {
                item.getDateTakenDay()
            }

            MediaItemSortMode.LastModified -> {
                item.getLastModifiedDay()
            }

            MediaItemSortMode.MonthTaken -> {
                item.getDateTakenMonth()
            }

            else -> throw IllegalStateException("Sort mode $sortBy should not be reached here")
        }
    }

    val sortedMap = grouped.toSortedMap(
        compareByDescending { time ->
            time
        }
    )

    val calendar = Calendar.getInstance(Locale.ENGLISH).apply {
        timeInMillis = System.currentTimeMillis()
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val today = calendar.timeInMillis / 1000
    val daySeconds = 60 * 60 * 24
    val yesterday = today - daySeconds

    val mediaItems = mutableListOf<MediaStoreData>()
    sortedMap.forEach { (sectionTime, children) ->
        val sectionKey = when (sectionTime) {
            today -> {
                "Today"
            }

            yesterday -> {
                "Yesterday"
            }

            else -> {
                formatDate(timestamp = sectionTime, sortBy = sortBy)
            }
        }

        val section = SectionItem(date = sectionTime, childCount = children.size)
        mediaItems.add(listSection(sectionKey, sectionTime, children.size))

        mediaItems.addAll(
            children.onEach {
                it.section = section
            }
        )
    }

    return mediaItems
}

fun formatDate(timestamp: Long, sortBy: MediaItemSortMode): String {
    return if (timestamp != 0L) {
        val dateTimeFormat =
            if (sortBy == MediaItemSortMode.MonthTaken) DateTimeFormatter.ofPattern("MMMM yyyy")
            else DateTimeFormatter.ofPattern("EEE d - MMMM yyyy")

        val localDateTime =
            Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()
        val dateTimeString = localDateTime.format(dateTimeFormat)
        dateTimeString.toString()
    } else {
        "Pretend there is a date here"
    }
}

private fun listSection(title: String, key: Long, childCount: Int): MediaStoreData {
    val mediaSection = MediaStoreData(
        type = MediaType.Section,
        dateModified = key,
        dateTaken = key,
        uri = "$title $key".toUri(),
        displayName = title,
        id = 0L,
        mimeType = null,
        section = SectionItem(
            date = key,
            childCount = childCount
        )
    )
    return mediaSection
}


