package com.kaii.photos.gallery_model

import android.content.Context
import android.net.Uri
import android.text.format.DateFormat.format
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaStoreDataSource
import com.kaii.photos.mediastore.Type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class GalleryViewModel(context: Context) : ViewModel() {
    private val mediaStoreDataSource = MediaStoreDataSource(context)

    private val _uiState: MutableStateFlow<List<MediaStoreData>> = MutableStateFlow(emptyList())
    val mediaStoreData: StateFlow<List<MediaStoreData>> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            mediaStoreDataSource.loadMediaStoreData().flowOn(Dispatchers.IO).collect {
                _uiState.value = it
            }
        }
    }
}

/** Groups photos by date */
fun groupPhotosBy(media: List<MediaStoreData>, sortDescending: Boolean = true, byDay: Boolean = true) : List<MediaStoreData> {
    val mediaItems = emptyList<MediaStoreData>().toMutableList()

    val mediaDataGroups = LinkedHashMap<Long, MutableList<MediaStoreData>>()
    media.forEach { data ->
        val key = if (!byDay) data.getLastModifiedMonth() else data.getLastModifiedDay()
        if (!mediaDataGroups.containsKey(key)) {
            mediaDataGroups[key] = emptyList<MediaStoreData>().toMutableList()
        }
        mediaDataGroups[key]?.add(data)
    }

    val sorted = mediaDataGroups.toSortedMap(
        if (sortDescending) compareByDescending { time ->
            time
        } else compareBy { time ->
            time
        }
    )

    mediaDataGroups.clear()
    for ((key, value) in sorted) {
        mediaDataGroups[key] = value
    }

    val calendar = Calendar.getInstance(Locale.ENGLISH).apply {
         timeInMillis = System.currentTimeMillis()
         set(Calendar.HOUR_OF_DAY, 0)
         set(Calendar.MINUTE, 0)
         set(Calendar.SECOND, 0)
         set(Calendar.MILLISECOND, 0)
     }

	val today = calendar.timeInMillis
    val dayMillis = 86400000
    val yesterday = today - dayMillis
    for ((key, value) in mediaDataGroups) {
        var currentGridPosition = 0
        val sectionKey = when (key) {
            today -> {
                "Today"
            }
            yesterday -> {
                "Yesterday"
            }
            else -> {
                formatDate(key, true)
            }
        }
        mediaItems.add(listSection(sectionKey, key))

        value.forEach {
            it.gridPosition = currentGridPosition++
        }

        mediaItems.addAll(value)
    }

    return mediaItems
}

private fun formatDate(timestamp: Long, showDay: Boolean): String {
    return if (timestamp != 0L) {
        val cal = Calendar.getInstance(Locale.ENGLISH)
        cal.timeInMillis = timestamp
        val format = if (showDay) "EEE d - MMMM yyyy" else "MMMM yyyy"
        format(format, cal).toString()
    } else {
        ""
    }
}

private fun listSection(title: String, key: Long): MediaStoreData {
    val mediaSection = MediaStoreData(
        type = Type.SECTION,
        dateModified = key,
        dateTaken = key,
        uri = Uri.parse(key.toString()),
        displayName = title,
        orientation = 0,
        rowId = 0L,
        mimeType = null,
        dateAdded = key,
    )
    return mediaSection
}

enum class GroupByType {
	LAST_MODIFIED
}
