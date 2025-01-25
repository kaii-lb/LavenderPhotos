package com.kaii.photos.models.gallery_model

import android.content.Context
import android.net.Uri
import android.os.CancellationSignal
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.SectionItem
import com.kaii.photos.mediastore.DefaultMediaStoreDataSource
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
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

class GalleryViewModel(context: Context, path: String, sortBy: MediaItemSortMode) : ViewModel() {
	private val cancellationSignal = CancellationSignal()
    private val mediaStoreDataSource = DefaultMediaStoreDataSource(context, path, sortBy, cancellationSignal)

    val mediaFlow by lazy {
        getMediaDataFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    }

    private fun getMediaDataFlow(): Flow<List<MediaStoreData>> {
        return mediaStoreDataSource.loadMediaStoreData().flowOn(Dispatchers.IO)
    }

    fun cancelMediaFlow() {
    	cancellationSignal.cancel()
    }
}

/** Groups photos by date */
fun groupPhotosBy(media: List<MediaStoreData>, sortBy: MediaItemSortMode = MediaItemSortMode.DateTaken, sortDescending: Boolean = true): List<MediaStoreData> {
    if (media.isEmpty()) return emptyList()

    val mediaItems = emptyList<MediaStoreData>().toMutableList()

    val mediaDataGroups = LinkedHashMap<Long, MutableList<MediaStoreData>>()
    media.forEach { data ->
        val key = when (sortBy) {
            MediaItemSortMode.DateTaken -> {
                data.getDateTakenDay()
            }
            MediaItemSortMode.LastModified -> {
                data.getLastModifiedDay()
            }
        }

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

	val today = calendar.timeInMillis / 1000
    val daySeconds= 86400
    val yesterday = today - daySeconds
    for ((key, value) in mediaDataGroups) {
        val sectionKey = when (key) {
            today -> {
                "Today"
            }
            yesterday -> {
                "Yesterday"
            }
            else -> {
                formatDate(key)
            }
        }

        val section = SectionItem(date = key, childCount = value.size)
        mediaItems.add(listSection(sectionKey, key, value.size))

		if (sortDescending) {
			if (sortBy == MediaItemSortMode.DateTaken) {
				value.sortByDescending { it.dateTaken }
			} else {
				value.sortByDescending { it.dateModified }
			}
		} else {
			if (sortBy == MediaItemSortMode.DateTaken) {
				value.sortBy { it.dateTaken }
			} else {
				value.sortBy { it.dateModified }
			}
		}

        mediaItems.addAll(
        	value.map {
                it.copy(
                    section = section
                )
            }
       	)
    }

    return mediaItems.distinct()
}

private fun formatDate(timestamp: Long): String {
    return if (timestamp != 0L) {
	    val dateTimeFormat = DateTimeFormatter.ofPattern("EEE d - MMMM yyyy")
	    val localDateTime = Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()
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
        uri = Uri.parse("$title $key"),
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
