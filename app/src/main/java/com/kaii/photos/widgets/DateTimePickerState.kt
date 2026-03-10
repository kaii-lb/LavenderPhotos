package com.kaii.photos.widgets

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.core.net.toUri
import com.kaii.photos.R
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.di.appModule
import com.kaii.photos.mediastore.setDateForMedia
import com.kaii.photos.permissions.files.FilePermissionsState
import com.kaii.photos.permissions.files.rememberFilePermissionManager
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.yearMonth
import kotlin.time.Instant

class DateTimePickerState(
    private val mediaItem: MediaStoreData,
    private val context: Context
) {
    internal var permissionManager: FilePermissionsState? = null

    val initialDateTime =
        Instant.fromEpochSeconds(mediaItem.dateTaken)
            .toLocalDateTime(TimeZone.currentSystemDefault())

    var date by mutableStateOf(initialDateTime.date)
        private set

    var time by mutableStateOf(initialDateTime.time)
        private set

    fun setDay(day: Int) {
        val days = date.yearMonth.numberOfDays
        val actual = if (day !in 1..days) days else day

        date = LocalDate(date.year, date.month, actual)
    }

    fun setMonth(month: Month) {
        val days = YearMonth(date.year, month).numberOfDays
        val day = if (date.day !in 1..days) days else date.day

        date = LocalDate(date.year, month, day)
    }

    fun setYear(year: Int) {
        val days = YearMonth(year, date.month).numberOfDays
        val day = if (date.day !in 1..days) days else date.day

        date = LocalDate(year, date.month, day)
    }

    fun setSecond(second: Int) {
        time = LocalTime(time.hour, time.minute, second)
    }

    fun setMinute(minute: Int) {
        time = LocalTime(time.hour, minute, time.second)
    }

    fun setHour(hour: Int) {
        time = LocalTime(hour, time.minute, time.second)
    }

    fun writeDate() {
        permissionManager?.get(listOf(mediaItem.uri.toUri()))
    }

    internal suspend fun save() = withContext(Dispatchers.IO) {
        val dateTime = date.atTime(time).toInstant(timeZone = TimeZone.currentSystemDefault()).epochSeconds

        context.contentResolver.setDateForMedia(
            uri = mediaItem.uri.toUri(),
            type = mediaItem.type,
            dateTaken = dateTime,
            overwriteLastModified = false
        )
    }
}

@Composable
fun rememberDateTimePickerState(
    mediaItem: MediaStoreData,
    onDone: () -> Unit = {},
    onFailed: () -> Unit = {}
): DateTimePickerState {
    val context = LocalContext.current

    val state = remember(mediaItem) {
        DateTimePickerState(
            mediaItem = mediaItem,
            context = context
        )
    }

    val resources = LocalResources.current
    val coroutineScope = rememberCoroutineScope()
    val permissionManager = rememberFilePermissionManager(
        onGranted = {
            context.appModule.scope.launch {
                state.save()
                onDone()
            }
        },
        onRejected = {
            coroutineScope.launch {
                onFailed()
                LavenderSnackbarController.pushEvent(
                    LavenderSnackbarEvent.MessageEvent(
                        message = resources.getString(R.string.exif_date_changed_failed),
                        icon = R.drawable.event_busy,
                        duration = SnackbarDuration.Short
                    )
                )
            }
        }
    )

    DisposableEffect(permissionManager, state) {
        state.permissionManager = permissionManager

        onDispose {
            state.permissionManager = null
        }
    }

    return state
}