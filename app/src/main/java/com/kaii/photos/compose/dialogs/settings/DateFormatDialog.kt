package com.kaii.photos.compose.dialogs.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.LavenderDialogBase
import com.kaii.photos.compose.dialogs.TitleCloseRow
import com.kaii.photos.compose.widgets.PreferencesRow
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.RowPosition
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun DateFormatDialog(
    setDisplayDateFormat: (value: DisplayDateFormat) -> Unit,
    onDismiss: () -> Unit
) {
    LavenderDialogBase(
        onDismiss = onDismiss
    ) {
        TitleCloseRow(
            title = stringResource(id = R.string.look_and_feel_date_format),
            closeOffset = 12.dp
        ) {
            onDismiss()
        }

        Spacer(modifier = Modifier.height(12.dp))
        val currentDate = remember {
            Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
                .toJavaLocalDate()
        }

        LazyColumn(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            itemsIndexed(
                items = DisplayDateFormat.entries
            ) { index, item ->
                PreferencesRow(
                    title = stringResource(id = item.description),
                    summary = currentDate.format(item.format),
                    position =
                        when (index) {
                            0 -> RowPosition.Top
                            DisplayDateFormat.entries.size - 1 -> RowPosition.Bottom
                            else -> RowPosition.Middle
                        },
                    iconResID = item.icon
                ) {
                    setDisplayDateFormat(item)
                    onDismiss()
                }
            }
        }
    }
}