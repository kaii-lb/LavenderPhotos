package com.aks_labs.tulsi.helpers

import android.os.Parcelable
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.aks_labs.tulsi.mediastore.MediaStoreData
import com.aks_labs.tulsi.mediastore.MediaType
import kotlinx.parcelize.Parcelize

@Parcelize
data class SectionItem(
	val date: Long,
	val childCount: Int
) : Parcelable

fun SnapshotStateList<MediaStoreData>.unselectItem(
	item: MediaStoreData,
	groupedMedia: List<MediaStoreData>
) {
	// not necessary to check if not the same as section size
	// cuz were removing an item, it will never be
	groupedMedia.firstOrNull {
		it.section == item.section && it.type == MediaType.Section
	}?.let {
		remove(it)
	}

	remove(item)
}

fun SnapshotStateList<MediaStoreData>.selectItem(
	item: MediaStoreData,
	groupedMedia: List<MediaStoreData>
) {
	val alreadySelected = filter {
		it.section == item.section && it.type != MediaType.Section
	}

	if (alreadySelected.size == item.section.childCount - 1) {
		groupedMedia.first {
			it.section == item.section && it.type == MediaType.Section
		}.let {
			add(it)
		}
	}

	if (!contains(item)) add(item)
}

fun SnapshotStateList<MediaStoreData>.selectAll(
	items: List<MediaStoreData>,
	groupedMedia: List<MediaStoreData>
) {
	val grouped = items.groupBy {
		it.section
	}

	grouped.keys.forEach { key ->
		val sectionItems = grouped[key]

		if (sectionItems?.size == key.childCount) {
			val section = groupedMedia.first {
				it.type == MediaType.Section && it.section == key
			}

			add(section)
		}

		sectionItems?.let {
			removeAll(it.toSet())
			addAll(it)
		}
	}
}

fun SnapshotStateList<MediaStoreData>.unselectAll(
	items: List<MediaStoreData>,
	groupedMedia: List<MediaStoreData>
) {
	val grouped = items.groupBy {
		it.section
	}

	grouped.keys.forEach { key ->
		val sectionItems = grouped[key]

		val section = groupedMedia.first {
			it.type == MediaType.Section && it.section == key
		}

		remove(section)

		sectionItems?.let {
			removeAll(it.toSet())
		}
	}
}

fun SnapshotStateList<MediaStoreData>.selectSection(
	section: SectionItem,
	groupedMedia: List<MediaStoreData>
) {
	val media = groupedMedia.filter {
		it.section == section
	}

	removeAll(media.toSet())
	addAll(media)
}

fun SnapshotStateList<MediaStoreData>.unselectSection(
	section: SectionItem,
	groupedMedia: List<MediaStoreData>
) {
	val media = groupedMedia.filter {
		it.section == section
	}

	removeAll(media.toSet())
}
