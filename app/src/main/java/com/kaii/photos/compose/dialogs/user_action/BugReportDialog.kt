package com.kaii.photos.compose.dialogs.user_action

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.LavenderDialogBase
import com.kaii.photos.compose.dialogs.TitleCloseRow
import com.kaii.photos.compose.widgets.PreferencesRow
import com.kaii.photos.domain.settings.BugReportType
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.presentation.ui.theme.ThemeConfiguration
import com.kaii.photos.ui.theme.PhotosTheme

@Preview
@Composable
private fun BugReportDialogPreview() {
    PhotosTheme(theme = ThemeConfiguration.Default) {
        Box(
            modifier = Modifier
                .width(1080.dp)
                .height(1920.dp)
        ) {
            BugReportDialog(
                onDismiss = {}
            )
        }
    }
}

@Composable
fun BugReportDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    LavenderDialogBase(
        modifier = modifier,
        onDismiss = onDismiss
    ) {
        TitleCloseRow(
            title = stringResource(id = R.string.debugging_report_issue),
            onClose = onDismiss,
            closeOffset = 8.dp
        )

        Spacer(modifier = Modifier.height(4.dp))

        val context = LocalContext.current
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(all = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BugReportType.entries.forEachIndexed { index, type ->
                PreferencesRow(
                    title = stringResource(id = type.title),
                    summary = stringResource(id = type.summary),
                    position = when (index) {
                        0 if BugReportType.entries.size == 1 -> RowPosition.Single

                        0 -> RowPosition.Top

                        BugReportType.entries.lastIndex -> RowPosition.Bottom

                        else -> RowPosition.Middle
                    },
                    iconResID = type.icon,
                    cornerRadius = 16.dp
                ) {
                    val intent = Intent().apply {
                        action = Intent.ACTION_VIEW
                        data = type.url.toUri()
                    }

                    context.startActivity(intent)
                }
            }
        }
    }
}