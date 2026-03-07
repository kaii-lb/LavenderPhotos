package com.kaii.photos.compose.dialogs.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.HorizontalSeparator
import com.kaii.photos.compose.dialogs.InfoRow
import com.kaii.photos.compose.dialogs.LavenderDialogBase
import com.kaii.photos.compose.dialogs.TitleCloseRow
import com.kaii.photos.compose.pages.FullWidthDialogButton
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.datastore.DefaultTabs
import com.kaii.photos.helpers.RowPosition
import kotlinx.coroutines.launch

@Composable
fun TabCustomizationDialog(
    tabList: List<BottomBarTab>,
    setTabList: (list: List<BottomBarTab>) -> Unit,
    closeDialog: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    LavenderDialogBase(
        onDismiss = closeDialog
    ) {
        TitleCloseRow(title = stringResource(id = R.string.tabs_customize)) {
            closeDialog()
        }

        Column(
            modifier = Modifier
                .wrapContentSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            val resources = LocalResources.current

            DefaultTabs.defaultList.forEach { tab ->
                InfoRow(
                    text = tab.name,
                    iconResId = if (tab in tabList) R.drawable.delete else R.drawable.add,
                    opacity = if (tab in tabList) 1f else 0.5f
                ) {
                    setTabList(
                        tabList.toMutableList().apply {
                            if (tab in tabList && tabList.size > 1) {
                                remove(tab)
                            } else if (tab in tabList) {
                                coroutineScope.launch {
                                    LavenderSnackbarController.pushEvent(
                                        LavenderSnackbarEvents.MessageEvent(
                                            message = resources.getString(R.string.tabs_min_reached),
                                            icon = R.drawable.error_2,
                                            duration = SnackbarDuration.Short
                                        )
                                    )
                                }
                            }

                            if (tab !in tabList && tabList.size < 16) {
                                add(tab)
                            } else if (tab !in tabList) {
                                coroutineScope.launch {
                                    LavenderSnackbarController.pushEvent(
                                        LavenderSnackbarEvents.MessageEvent(
                                            message = resources.getString(R.string.tabs_max_reached),
                                            icon = R.drawable.error_2,
                                            duration = SnackbarDuration.Short
                                        )
                                    )
                                }
                            }
                        }
                    )
                }
            }

            tabList.forEach { tab ->
                if (tab !in DefaultTabs.defaultList) {
                    InfoRow(
                        text = tab.name,
                        iconResId = R.drawable.delete
                    ) {
                        if (tabList.size > 1) {
                            setTabList(
                                tabList.toMutableList().apply {
                                    remove(tab)
                                }
                            )
                        } else {
                            coroutineScope.launch {
                                LavenderSnackbarController.pushEvent(
                                    LavenderSnackbarEvents.MessageEvent(
                                        message = resources.getString(R.string.tabs_min_reached),
                                        icon = R.drawable.error_2,
                                        duration = SnackbarDuration.Short
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        HorizontalSeparator()
        Spacer(modifier = Modifier.height(16.dp))

        var showDialog by remember { mutableStateOf(false) }
        if (showDialog) {
            AddTabDialog(
                tabList = tabList,
                setTabList = setTabList,
                dismissDialog = {
                    showDialog = false
                }
            )
        }

        val resources = LocalResources.current
        FullWidthDialogButton(
            text = stringResource(id = R.string.tabs_add),
            color = MaterialTheme.colorScheme.primary,
            position = RowPosition.Single,
            textColor = MaterialTheme.colorScheme.onPrimary
        ) {
            if (tabList.size < 8) {
                showDialog = true
            } else {
                coroutineScope.launch {
                    LavenderSnackbarController.pushEvent(
                        LavenderSnackbarEvents.MessageEvent(
                            message = resources.getString(R.string.tabs_max_reached),
                            icon = R.drawable.error_2,
                            duration = SnackbarDuration.Short
                        )
                    )
                }
            }
        }
    }
}