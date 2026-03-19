package com.kaii.photos.compose.grids.media

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toIntRect

@Suppress("UNCHECKED_CAST")
fun <T> LazyGridState.getGridItemAtOffset(
    offset: Offset,
    keys: List<T>,
    numberOfHorizontalItems: Int
): Int? {
    var key: T? = null

    // scan the entire row for this item
    // if there's only one or two items on a row and user drag selects to the empty space they get selected
    for (i in 1..numberOfHorizontalItems) {
        val possibleItem = layoutInfo.visibleItemsInfo.find { item ->
            val stretched = item.size.toIntRect().let {
                IntRect(
                    top = it.top,
                    bottom = it.bottom,
                    left = it.left,
                    right = it.right * i
                )
            }

            stretched.contains(offset.round() - item.offset)
        }

        if (possibleItem != null) {
            key = possibleItem.key as T
            break
        }
    }

    val found = keys.find {
        it == key
    } ?: return null

    return keys.indexOf(found)
}