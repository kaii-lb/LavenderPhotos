package com.kaii.photos.compose.widgets

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TimeInput
import androidx.compose.material3.getSelectedDate
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.LocalAppDatabase
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.ConfirmCancelRow
import com.kaii.photos.compose.dialogs.LavenderDialogBase
import com.kaii.photos.database.entities.MediaEntity
import com.kaii.photos.helpers.GetPermissionAndRun
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.helpers.exif.getDateTakenForMedia
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.setDateForMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.format
import kotlinx.datetime.toInstant
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaYearMonth
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.yearMonth
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class, ExperimentalMaterial3Api::class)
@Composable
fun DateTimePicker(
    mediaItem: MediaStoreData,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateTime = remember {
        val time =
            getDateTakenForMedia(
                absolutePath = mediaItem.absolutePath,
                dateModified = Clock.System.now().epochSeconds
            )

        Instant.fromEpochSeconds(time)
            .toLocalDateTime(timeZone = TimeZone.currentSystemDefault())
    }

    var selectedDate by remember { mutableStateOf(dateTime.date) }
    var showDatePicker by remember { mutableStateOf(true) }
    var showTimePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDate = selectedDate.toJavaLocalDate(),
            initialDisplayedMonth = selectedDate.yearMonth.toJavaYearMonth()
        )

        LavenderDialogBase(
            onDismiss = onDismiss
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DatePicker(
                    state = datePickerState,
                    colors = DatePickerDefaults.colors(
                        containerColor = Color.Transparent,
                        selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                        selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                        todayDateBorderColor = MaterialTheme.colorScheme.secondary
                    )
                )

                ConfirmCancelRow(
                    onCancel = onDismiss,
                    onConfirm = {
                        datePickerState.getSelectedDate()?.let { selectedDate = it.toKotlinLocalDate() }
                        showTimePicker = true
                        showDatePicker = false
                    },
                    confirmColors = ButtonDefaults.buttonColors()
                )
            }
        }
    }

    if (showTimePicker) {
        val context = LocalContext.current
        val resources = LocalResources.current
        val mainViewModel = LocalMainViewModel.current
        val mediaDao = LocalAppDatabase.current.mediaEntityDao()
        val coroutineScope = rememberCoroutineScope()

        var selectedTime by remember { mutableStateOf(dateTime.time) }
        val getPermission = remember { mutableStateOf(false) }
        GetPermissionAndRun(
            uris = listOf(mediaItem.uri),
            shouldRun = getPermission,
            onGranted = {
                mainViewModel.launch(Dispatchers.IO) {
                    val dateTime = selectedDate.atTime(selectedTime).toInstant(timeZone = TimeZone.UTC).epochSeconds // UTC since this is since epoch

                    mediaDao.deleteEntityById(mediaItem.id)
                    mediaDao.insertEntity(
                        MediaEntity(
                            id = mediaItem.id,
                            dateTaken = dateTime,
                            mimeType = mediaItem.mimeType ?: "image/png", // videos aren't supported right now.
                            displayName = mediaItem.displayName
                        )
                    )

                    context.contentResolver.setDateForMedia(
                        uri = mediaItem.uri,
                        type = mediaItem.type,
                        dateTaken = dateTime,
                        context = context,
                        overwriteLastModified = false
                    )
                }

                onDismiss()
                onSuccess()
            },
            onRejected = {
                coroutineScope.launch {
                    LavenderSnackbarController.pushEvent(
                        LavenderSnackbarEvents.MessageEvent(
                            message = resources.getString(R.string.exif_date_changed_failed),
                            icon = R.drawable.event_busy,
                            duration = SnackbarDuration.Short
                        )
                    )

                    onDismiss()
                }
            }
        )

        TimePicker(
            time = dateTime.time,
            onDismiss = onDismiss,
            onSave = { time ->
                selectedTime = time
                getPermission.value = true
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePicker(
    time: LocalTime,
    onDismiss: () -> Unit,
    onSave: (LocalTime) -> Unit
) {
    var selectedTime by remember { mutableStateOf(time) }

    println("SELECTED_TIME ${time.format(LocalTime.Formats.ISO)}")

    LavenderDialogBase(
        onDismiss = onDismiss
    ) {
        val context = LocalContext.current
        val timePickerState = rememberTimePickerState(
            initialHour = selectedTime.hour,
            initialMinute = selectedTime.minute,
            is24Hour = DateFormat.is24HourFormat(context)
        )

        Column(
            modifier = Modifier
                .padding(all = 12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.exif_enter_time),
                fontSize = TextStylingConstants.EXTRA_SMALL_TEXT_SIZE.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            )

            TimeInput(
                state = timePickerState,
                modifier = Modifier
                    .fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    space = 8.dp,
                    alignment = Alignment.Start
                )
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.clock),
                        contentDescription = null
                    )
                }

                FilledTonalButton(
                    onClick = onDismiss
                ) {
                    Text(
                        text = stringResource(id = R.string.media_cancel),
                        fontSize = TextStylingConstants.SMALL_TEXT_SIZE.sp
                    )
                }

                Button(
                    onClick = {
                        selectedTime = LocalTime(
                            hour = timePickerState.hour,
                            minute = timePickerState.minute,
                            second = 0
                        )

                        onSave(selectedTime)
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.media_confirm),
                        fontSize = TextStylingConstants.SMALL_TEXT_SIZE.sp
                    )
                }
            }
        }
    }
}