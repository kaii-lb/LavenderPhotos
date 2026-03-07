package com.kaii.photos.compose.dialogs.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.ConfirmCancelRow
import com.kaii.photos.compose.dialogs.LavenderDialogBase
import com.kaii.photos.compose.dialogs.TitleCloseRow
import com.kaii.photos.compose.widgets.RadioButtonRow
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.grid_management.MediaItemSortMode.Companion.presentableName

@Composable
fun SortModeSelectorDialog(
    currentSortMode: MediaItemSortMode,
    setSortMode: (mode: MediaItemSortMode) -> Unit,
    dismiss: () -> Unit
) {
    LavenderDialogBase(onDismiss = dismiss) {
        TitleCloseRow(title = stringResource(id = R.string.sort_mode)) {
            dismiss()
        }

        var chosenSortMode by remember { mutableStateOf(currentSortMode) }
        val sortModes = remember {
            // ignore "Disabled" and "DisabledLastModified", handled by toggle switch
            MediaItemSortMode.entries.filter { it != MediaItemSortMode.Disabled && it != MediaItemSortMode.DisabledLastModified }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth(1f)
                .wrapContentHeight()
        ) {
            items(
                count = sortModes.size
            ) { index ->
                val sortMode = sortModes[index]

                RadioButtonRow(
                    text = sortMode.presentableName,
                    checked = chosenSortMode == sortMode
                ) {
                    chosenSortMode = sortMode
                }
            }
        }

        ConfirmCancelRow(
            onConfirm = {
                setSortMode(chosenSortMode)
                dismiss()
            }
        )
    }
}