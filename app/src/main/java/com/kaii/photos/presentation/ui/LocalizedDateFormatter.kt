package com.kaii.photos.presentation.ui

import com.kaii.photos.datastore.preferences.SettingsLookAndFeelImpl
import com.kaii.photos.datastore.preferences.SettingsPhotoGridImpl
import com.kaii.photos.di.ApplicationScope
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalizedDateFormatter @Inject constructor(
    photoGrid: SettingsPhotoGridImpl,
    lookAndFeel: SettingsLookAndFeelImpl,
    @ApplicationScope scope: CoroutineScope
) {
    private var currentFormat = DisplayDateFormat.Default
    private var currentSortMode = MediaItemSortMode.DateTaken

    private var formatter = currentFormat.format
    private val zoneId = ZoneId.systemDefault()

    init {
        scope.launch {
            launch {
                photoGrid.getSortMode().collect { sortMode ->
                    val dateFormat =
                        if (sortMode == MediaItemSortMode.MonthTaken) {
                            DateTimeFormatter.ofPattern("MMMM yyyy")
                        } else {
                            currentFormat.format
                        }

                    currentSortMode = sortMode
                    formatter = dateFormat
                }
            }

            launch {
                lookAndFeel.getDisplayDateFormat().collect { format ->
                    val dateFormat =
                        if (currentSortMode == MediaItemSortMode.MonthTaken) {
                            DateTimeFormatter.ofPattern("MMMM yyyy")
                        } else {
                            format.format
                        }

                    currentFormat = format
                    formatter = dateFormat
                }
            }
        }
    }

    fun formatDay(dateTakenSeconds: Long): String =
        Instant.ofEpochSecond(dateTakenSeconds)
            .atZone(zoneId)
            .format(formatter)
}