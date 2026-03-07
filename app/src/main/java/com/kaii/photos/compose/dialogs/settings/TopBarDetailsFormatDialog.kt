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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.LavenderDialogBase
import com.kaii.photos.compose.dialogs.TitleCloseRow
import com.kaii.photos.compose.widgets.PreferencesRow
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.TopBarDetailsFormat
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun TopBarDetailsFormatDialog(
    setTopBarDetailsFormat: (value: TopBarDetailsFormat) -> Unit,
    onDismiss: () -> Unit
) {
    LavenderDialogBase(
        onDismiss = onDismiss
    ) {
        TitleCloseRow(
            title = stringResource(id = R.string.look_and_feel_image_details_format),
            closeOffset = 12.dp
        ) {
            onDismiss()
        }

        Spacer(modifier = Modifier.height(12.dp))

        val context = LocalContext.current
        val currentDate = remember {
            Clock.System.now()
                .epochSeconds
        }

        LazyColumn(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            itemsIndexed(
                items = TopBarDetailsFormat.entries
            ) { index, item ->
                PreferencesRow(
                    title = stringResource(id = item.description),
                    summary = item.format(context, "Screenshot.png", currentDate),
                    position =
                        when (index) {
                            0 -> RowPosition.Top
                            TopBarDetailsFormat.entries.size - 1 -> RowPosition.Bottom
                            else -> RowPosition.Middle
                        },
                    iconResID = item.icon
                ) {
                    setTopBarDetailsFormat(item)
                    onDismiss()
                }
            }
        }
    }
}