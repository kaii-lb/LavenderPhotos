package com.kaii.photos.compose.dialogs.main_dialog

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.user_action.BugReportDialog
import com.kaii.photos.compose.dialogs.user_action.ExplanationDialog
import com.kaii.photos.compose.widgets.ExpressiveDialogRow
import com.kaii.photos.compose.widgets.PreferencesSeparatorText
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.Screens
import com.kaii.photos.presentation.main_dialog.AboutLinkItems
import com.kaii.photos.presentation.main_dialog.SettingsItems
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
fun LazyListScope.settingsColumnItems(
    navController: NavController,
    coroutineScope: CoroutineScope,
    dismiss: suspend () -> Unit
) {
    item {
        PreferencesSeparatorText(
            text = stringResource(id = R.string.settings)
        )
    }

    itemsIndexed(
        items = SettingsItems.entries
    ) { index, item ->
        ExpressiveDialogRow(
            title = stringResource(id = item.title),
            icon = painterResource(id = item.icon),
            position =
                when (index) {
                    0 -> RowPosition.Top
                    SettingsItems.entries.size - 1 -> RowPosition.Bottom
                    else -> RowPosition.Middle
                },
            onClick = {
                coroutineScope.launch {
                    dismiss()
                    navController.navigate(item.screen)
                }
            }
        )
    }

    item {
        PreferencesSeparatorText(
            stringResource(id = R.string.settings_about_and_updates)
        )
    }

    item {
        ExpressiveDialogRow(
            title = stringResource(id = R.string.settings_about),
            icon = painterResource(id = R.drawable.info),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            position = RowPosition.Top,
            onClick = {
                coroutineScope.launch {
                    dismiss()

                    navController.navigate(Screens.Settings.Misc.AboutPage)
                }
            }
        )
    }

    items(
        items = AboutLinkItems.entries
    ) { item ->
        val context = LocalContext.current

        ExpressiveDialogRow(
            title = stringResource(id = item.title),
            icon = painterResource(id = item.icon),
            enabled = item.enabled,
            containerColor = item.color,
            position = RowPosition.Middle,
            onClick = {
                coroutineScope.launch {
                    dismiss()
                    context.startActivity(item.intent)
                }
            }
        )
    }

    item {
        ExpressiveDialogRow(
            title = stringResource(id = R.string.updates),
            icon = painterResource(id = R.drawable.update),
            position = RowPosition.Bottom,
            onClick = {
                coroutineScope.launch {
                    dismiss()
                    navController.navigate(Screens.Settings.Misc.UpdatePage)
                }
            }
        )
    }

    item {
        PreferencesSeparatorText(
            stringResource(id = R.string.immich_misc)
        )
    }

    item {
        ExpressiveDialogRow(
            title = stringResource(id = R.string.licenses),
            icon = painterResource(id = R.drawable.license),
            position = RowPosition.Top,
            onClick = {
                coroutineScope.launch {
                    dismiss()
                    navController.navigate(Screens.Settings.Misc.LicensesPage)
                }
            }
        )
    }

    item {
        var showPrivacyPolicy by remember { mutableStateOf(false) }
        if (showPrivacyPolicy) {
            ExplanationDialog(
                title = stringResource(id = R.string.privacy_policy_title),
                explanation = stringResource(id = R.string.privacy_policy)
            ) {
                showPrivacyPolicy = false
            }
        }

        ExpressiveDialogRow(
            title = stringResource(id = R.string.privacy_policy_title),
            icon = painterResource(id = R.drawable.privacy_policy),
            position = RowPosition.Bottom,
            onClick = {
                showPrivacyPolicy = true
            }
        )
    }

    item {
        PreferencesSeparatorText(
            text = stringResource(id = R.string.debugging_development)
        )
    }

    item {
        var showDialog by remember { mutableStateOf(false) }
        if (showDialog) {
            BugReportDialog {
                showDialog = false
            }
        }

        ExpressiveDialogRow(
            title = stringResource(id = R.string.debugging_report_issue),
            icon = painterResource(id = R.drawable.flag_2),
            position = RowPosition.Top,
            containerColor = MaterialTheme.colorScheme.error,
            onClick = {
                showDialog = true
            }
        )
    }

    item {
        ExpressiveDialogRow(
            title = stringResource(id = R.string.debugging),
            icon = painterResource(id = R.drawable.bug_report),
            position = RowPosition.Bottom,
            onClick = {
                navController.navigate(Screens.Settings.MainPage.Debugging)
            }
        )
    }
}