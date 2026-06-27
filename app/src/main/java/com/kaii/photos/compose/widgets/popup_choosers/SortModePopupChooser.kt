package com.kaii.photos.compose.widgets.popup_choosers

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kaii.photos.domain.settings.SortModeItem
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.widgets.SortModePickerState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortModePopupChooser(
    state: SortModePickerState,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    ),
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    GenericPopupChooser(
        sheetState = sheetState,
        query = { "" },
        onQueryChanged = {},
        itemList = {
            SortModeItem.defaultItems
        },
        key = { it.sortMode.ordinal },
        modifier = modifier,
        showSearchBar = false,
        onDismiss = {
            coroutineScope.launch {
                sheetState.hide()
                onDismiss()
            }
        }
    ) { index, item ->
        GenericPopupChooserItem(
            name = stringResource(id = item.labelId),
            summary = null,
            selected = {
                state.currentMode == item
            },
            position = when (index) {
                0 -> RowPosition.Top
                SortModeItem.defaultItems.size - 1 -> RowPosition.Bottom
                else -> RowPosition.Middle
            },
            onClick = {
                state.setMode(item)
            }
        )
    }
}