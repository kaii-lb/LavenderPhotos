package com.kaii.photos.widgets

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.core.net.toUri
import com.kaii.photos.PhotosApplication
import com.kaii.photos.R
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.mediastore.setDateForMedia
import com.kaii.photos.permissions.files.FilePermissionsState
import com.kaii.photos.permissions.files.rememberFilePermissionManager
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class MediaDateTimerPickerState(
    private val mediaItem: MediaStoreData,
    private val context: Context,
    scope: CoroutineScope
) : DateTimePickerState(
    initialDateTime = Instant.fromEpochSeconds(mediaItem.dateTaken)
        .toLocalDateTime(TimeZone.currentSystemDefault()),
    scope = scope
) {
    internal var permissionManager: FilePermissionsState? = null

    fun writeDate() {
        permissionManager?.get(listOf(mediaItem.uri.toUri()))
    }

    internal suspend fun save() = withContext(Dispatchers.IO) {
        context.contentResolver.setDateForMedia(
            uri = mediaItem.uri.toUri(),
            type = mediaItem.type,
            dateTaken = getDateTime().epochSeconds,
            overwriteLastModified = false
        )

        setIsLoading(false)
    }
}

@Composable
fun rememberDateTimePickerState(
    mediaItem: MediaStoreData
): MediaDateTimerPickerState {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val state = remember(mediaItem) {
        MediaDateTimerPickerState(
            mediaItem = mediaItem,
            context = context,
            scope = coroutineScope
        )
    }

    val resources = LocalResources.current
    val permissionManager = rememberFilePermissionManager(
        onGranted = {
            PhotosApplication.appModule.scope.launch {
                state.setIsError(error = false)
                state.setIsLoading(loading = false)
                state.save()
            }
        },
        onRejected = {
            coroutineScope.launch {
                state.setIsLoading(loading = false)
                state.setIsError(error = true)
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