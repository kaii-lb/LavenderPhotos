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
	// Check if we're in grid view mode (all items in a single section with date=0L)
	val isGridViewMode = item.section.date == 0L && groupedMedia.none { it.type == MediaType.Section }

	if (isGridViewMode) {
		// In grid view mode, just remove the item without section handling
		remove(item)
		return
	}

	// Date-grouped view mode logic
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
	// Check if we're in grid view mode (all items in a single section with date=0L)
	val isGridViewMode = item.section.date == 0L && groupedMedia.none { it.type == MediaType.Section }

	if (isGridViewMode) {
		// In grid view mode, just add the item without section handling
		if (!contains(item)) add(item)
		return
	}

	// Date-grouped view mode logic
	val alreadySelected = filter {
		it.section == item.section && it.type != MediaType.Section
	}

	if (alreadySelected.size == item.section.childCount - 1) {
		groupedMedia.firstOrNull {
			it.section == item.section && it.type == MediaType.Section
		}?.let {
			add(it)
		}
	}

	if (!contains(item)) add(item)
}

fun SnapshotStateList<MediaStoreData>.selectAll(
	items: List<MediaStoreData>,
	groupedMedia: List<MediaStoreData>
) {
	// Check if we're in grid view mode (all items in a single section with date=0L)
	val isGridViewMode = items.isNotEmpty() && items.first().section.date == 0L &&
		groupedMedia.none { it.type == MediaType.Section }

	if (isGridViewMode) {
		// In grid view mode, just add all items without section handling
		val itemsToAdd = items.filter { !contains(it) }
		addAll(itemsToAdd)
		return
	}

	// Date-grouped view mode logic
	val grouped = items.groupBy {
		it.section
	}

	grouped.keys.forEach { key ->
		val sectionItems = grouped[key]

		if (sectionItems?.size == key.childCount) {
			groupedMedia.firstOrNull {
				it.type == MediaType.Section && it.section == key
			}?.let {
				add(it)
			}
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
	// Check if we're in grid view mode (all items in a single section with date=0L)
	val isGridViewMode = items.isNotEmpty() && items.first().section.date == 0L &&
		groupedMedia.none { it.type == MediaType.Section }

	if (isGridViewMode) {
		// In grid view mode, just remove all items without section handling
		removeAll(items.toSet())
		return
	}

	// Date-grouped view mode logic
	val grouped = items.groupBy {
		it.section
	}

	grouped.keys.forEach { key ->
		val sectionItems = grouped[key]

		groupedMedia.firstOrNull {
			it.type == MediaType.Section && it.section == key
		}?.let {
			remove(it)
		}

		sectionItems?.let {
			removeAll(it.toSet())
		}
	}
}

fun SnapshotStateList<MediaStoreData>.selectSection(
	section: SectionItem,
	groupedMedia: List<MediaStoreData>
) {
	// Check if we're in grid view mode (all items in a single section with date=0L)
	val isGridViewMode = section.date == 0L && groupedMedia.none { it.type == MediaType.Section }

	if (isGridViewMode) {
		// In grid view mode, this shouldn't be called, but just in case
		val mediaItems = groupedMedia.filter { it.type != MediaType.Section }
		addAll(mediaItems.filter { !contains(it) })
		return
	}

	// Date-grouped view mode logic
	val media = groupedMedia.filter {
		it.section == section
	}

	if (media.isNotEmpty()) {
		removeAll(media.toSet())
		addAll(media)
	}
}

fun SnapshotStateList<MediaStoreData>.unselectSection(
	section: SectionItem,
	groupedMedia: List<MediaStoreData>
) {
	// Check if we're in grid view mode (all items in a single section with date=0L)
	val isGridViewMode = section.date == 0L && groupedMedia.none { it.type == MediaType.Section }

	if (isGridViewMode) {
		// In grid view mode, this shouldn't be called, but just in case
		clear()
		return
	}

	// Date-grouped view mode logic
	val media = groupedMedia.filter {
		it.section == section
	}

	if (media.isNotEmpty()) {
		removeAll(media.toSet())
	}
}
