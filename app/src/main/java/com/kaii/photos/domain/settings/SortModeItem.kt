package com.kaii.photos.domain.settings

import androidx.annotation.StringRes
import com.kaii.photos.R
import com.kaii.photos.helpers.grid_management.MediaItemSortMode

data class SortModeItem(
    val sortMode: MediaItemSortMode,
    @param:StringRes val labelId: Int
) {
    companion object {
        val defaultItems = listOf(
            SortModeItem(
                sortMode = MediaItemSortMode.DateTaken,
                labelId = R.string.settings_sort_mode_date_taken
            ),
            SortModeItem(
                sortMode = MediaItemSortMode.DateModified,
                labelId = R.string.settings_sort_mode_date_modified
            ),
            SortModeItem(
                sortMode = MediaItemSortMode.MonthTaken,
                labelId = R.string.settings_sort_mode_month_taken
            ),
            SortModeItem(
                sortMode = MediaItemSortMode.Disabled,
                labelId = R.string.settings_sort_mode_date_taken_no_separators
            ),
            SortModeItem(
                sortMode = MediaItemSortMode.DisabledLastModified,
                labelId = R.string.settings_sort_mode_date_modified_no_separators
            )
        )

        fun getForMode(mode: MediaItemSortMode) =
            defaultItems.first {
                it.sortMode == mode
            }
    }
}