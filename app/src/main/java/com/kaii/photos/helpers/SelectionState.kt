package com.kaii.photos.helpers

import android.util.Log
import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import kotlinx.parcelize.Parcelize

private const val TAG = "SELECTION_STATE"

@Parcelize
data class SectionItem(
	val date: Long,
	val childCount: Int
) : Parcelable

@Parcelize
data class SectionChild(
	val id: Long,
	val date: Long,
	val section: SectionItem
) : Parcelable {
	companion object {
		val dummyChild =
			SectionChild(
				id = 0L,
				date = 0L,
				section = SectionItem(
					date = 0L,
					childCount = 0
				)
			)
	}
}

class SelectionState(
	private val allChildren: List<SectionChild>,
	private val selectedItems: SnapshotStateMap<SectionItem, SnapshotStateList<SectionChild>>,
	private val selectedSections: SnapshotStateList<SectionItem>
) {
	fun add(item: SectionChild) {
		if (!selectedItems.containsKey(item.section)) {
			selectedItems[item.section] = mutableStateListOf(item)
		} else if (selectedItems[item.section]?.contains(item) == false) {
			selectedItems[item.section]?.add(item)
		}

		if (selectedItems[item.section]?.size == item.section.childCount) {
			selectedSections.add(item.section)
		}

		// Log.d(TAG, "item $item, selected size ${selectedItems[item.section]?.size} and child count ${item.section.childCount}")
	}

	fun remove(item: SectionChild) {
		if (selectedItems.containsKey(item.section) && selectedItems[item.section]?.contains(item) == true) {
			selectedItems[item.section]?.remove(item)
		}

		if (selectedItems[item.section]?.size != item.section.childCount) {
			selectedSections.remove(item.section)
		}
	}

	fun removeAll(items: List<SectionChild>) {
		items.forEach { item ->
			remove(item)
		}
	}

	fun addAll(items: List<SectionChild>) {
		items.forEach { item ->
			add(item)
		}
	}

	fun isItemSelected(item: SectionChild) : State<Boolean> = derivedStateOf { selectedItems[item.section]?.contains(item) == true }

	fun isSectionSelected(section: SectionItem) : State<Boolean> =  derivedStateOf { selectedSections.contains(section) }

	private fun getChildrenForSection(section: SectionItem) : List<SectionChild> {
		val children = allChildren.filter {
			it.section.date == section.date
		}

		return children
	}

	fun selectEntireSection(section: SectionItem) {
		getChildrenForSection(section).let { children ->
			children.forEach { child ->
				add(child)
			}
		}
	}

	fun unselectEntireSection(section: SectionItem) {
		getChildrenForSection(section).let { children ->
			children.forEach { child ->
				remove(child)
			}
		}
	}

	fun clear() {
		selectedItems.keys.forEach { key ->
			selectedItems[key]?.clear()
		}
		selectedSections.clear()
	}

	operator fun get(keyIndex: Int, childIndex: Int) : SectionChild {
		val key = selectedItems.keys.toList()[keyIndex]
		return selectedItems[key]!![childIndex]
	}

	fun mapTo(list: List<MediaStoreData>) =
		children.mapNotNull { child ->
			list.firstOrNull { media ->
				media.id == child.id && media.type != MediaType.Section
			}
		}

	val children by derivedStateOf {
		val children = emptyList<SectionChild>().toMutableList()

		selectedItems.keys.forEach { key ->
			selectedItems[key]?.let { children.addAll(it) }
		}

		children.toList()
	}

	val atLeastOneSelected by derivedStateOf {
		selectedItems.keys.any { key ->
			selectedItems[key]?.isNotEmpty() == true
		}
	}

	val size by derivedStateOf {
		selectedItems.keys.let { keys ->
			var size = 0

			keys.forEach { key ->
				size += selectedItems[key]?.filter { it != SectionChild.dummyChild }?.size ?: 0
			}

			return@let size
		}
	}
}

@Composable
fun rememberSelectionState(
	allItems: MutableState<List<MediaStoreData>>
) : SelectionState {
	val selectedItems = remember { mutableStateMapOf<SectionItem, SnapshotStateList<SectionChild>>() }
	val selectedSections = remember { mutableStateListOf<SectionItem>() }

	val selectionState by remember {
		derivedStateOf {
			SelectionState(
				allChildren =
					allItems.value.filter {
						it.type != MediaType.Section && it != MediaStoreData.dummyMediaStoreData
					}.map {
						SectionChild(
							id = it.id,
							date = it.dateTaken,
							section = it.section
						)
					},
				selectedItems = selectedItems,
				selectedSections = selectedSections
			)
		}
	}

	return selectionState
}
