package com.kaii.photos.compose.dialogs.settings

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.ConfirmCancelRow
import com.kaii.photos.compose.dialogs.LavenderDialogBase
import com.kaii.photos.compose.dialogs.ReorderableRadioButtonRow
import com.kaii.photos.compose.dialogs.TitleCloseRow
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.reorderable_lists.ReorderableItem
import com.kaii.photos.reorderable_lists.ReorderableLazyList
import com.kaii.photos.reorderable_lists.rememberReorderableListState

@Composable
fun DefaultTabSelectorDialog(
    tabList: List<BottomBarTab>,
    defaultTab: BottomBarTab,
    setTabList: (list: List<BottomBarTab>) -> Unit,
    setDefaultTab: (tab: BottomBarTab) -> Unit,
    dismissDialog: () -> Unit
) {
    var selectedTab by remember(defaultTab) { mutableStateOf(defaultTab) }
    val tabListDynamic = remember { mutableStateListOf<BottomBarTab>().apply { addAll(tabList) } }

    LavenderDialogBase(
        onDismiss = dismissDialog
    ) {
        TitleCloseRow(title = stringResource(id = R.string.tabs_default)) {
            dismissDialog()
        }

        // val state = rememberLazyListState()
        // val itemOffset = remember { mutableFloatStateOf(0f) }
        // var selectedItem: BottomBarTab? by remember { mutableStateOf(null) }

        val listState = rememberLazyListState()

        val reorderableState = rememberReorderableListState(listState) { fromIndex, toIndex ->
            val newList = tabListDynamic.toMutableList()
            newList.add(toIndex, newList.removeAt(fromIndex))

            tabListDynamic.clear()
            tabListDynamic.addAll(newList.distinctBy { it.name })
        }

        ReorderableLazyList(
            listState = listState,
            reorderableState = reorderableState
        ) {
            items(
                count = tabListDynamic.size,
                key = { index ->
                    tabListDynamic[index].name
                }
            ) { index ->
                ReorderableItem(
                    index = index,
                    reorderableState = reorderableState
                ) {
                    val tab = tabListDynamic[index]

                    ReorderableRadioButtonRow(
                        text = tab.name,
                        checked = selectedTab == tab
                    ) {
                        selectedTab = tab
                    }
                }
            }
        }

        ConfirmCancelRow(
            onConfirm = {
                setTabList(tabListDynamic)
                setDefaultTab(selectedTab)

                dismissDialog()
            }
        )
    }
}