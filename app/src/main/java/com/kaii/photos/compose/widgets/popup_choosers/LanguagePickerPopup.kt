package com.kaii.photos.compose.widgets.popup_choosers

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.widgets.LanguagePicker
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerPopup(
    state: LanguagePicker,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    ),
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val itemList by state.languages.collectAsStateWithLifecycle()

    GenericPopupChooser(
        sheetState = sheetState,
        query = { state.query },
        onQueryChanged = state::search,
        itemList = { itemList },
        key = { it.tag },
        modifier = modifier,
        onDismiss = {
            coroutineScope.launch {
                sheetState.hide()
                onDismiss()
            }
        }
    ) { index, item ->
        GenericPopupChooserItem(
            name = item.name,
            summary = item.localName,
            selected = {
                state.currentLanguage == item
            },
            position = when {
                itemList.size == 1 -> RowPosition.Single
                index == 0 -> RowPosition.Top
                index == itemList.size - 1 -> RowPosition.Bottom
                else -> RowPosition.Middle
            },
            onClick = {
                state.choose(item)
            }
        )
    }
}