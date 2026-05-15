package com.kaii.photos.helpers

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.kaii.photos.R
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import io.github.kaii_lb.lavender.immichintegration.serialization.SharedLinkResponse
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
fun formatDate(timestamp: Long, sortBy: MediaItemSortMode, format: DisplayDateFormat): String {
    return if (timestamp != 0L) {
        val dateFormat =
            if (sortBy == MediaItemSortMode.MonthTaken) {
                DateTimeFormatter.ofPattern("MMMM yyyy")
            } else {
                format.format
            }

        Instant.fromEpochSeconds(timestamp)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toJavaLocalDate()
            .format(dateFormat)
    } else {
        "Pretend there is a date here"
    }
}

enum class DisplayDateFormat(
    @param:StringRes val description: Int,
    @param:DrawableRes val icon: Int,
    val format: DateTimeFormatter
) {
    Default(
        description = R.string.look_and_feel_date_format_default,
        icon = R.drawable.event_filled,
        format = DateTimeFormatter.ofPattern("EEE dd - MMM yyyy")
    ),

    Numeric(
        description = R.string.look_and_feel_date_format_numeric,
        icon = R.drawable.schedule_filled,
        format = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    ),

    Short(
        description = R.string.look_and_feel_date_format_short,
        icon = R.drawable.acute,
        format = DateTimeFormatter.ofPattern("dd MMM yyyy")
    ),

    Standard(
        description = R.string.look_and_feel_date_format_standard,
        icon = R.drawable.article_filled,
        format = DateTimeFormatter.ISO_LOCAL_DATE
    )
}

@OptIn(ExperimentalTime::class)
enum class TopBarDetailsFormat(
    @param:StringRes val description: Int,
    @param:DrawableRes val icon: Int,
    val format: (Context, String, Long) -> String
) {
    FileName(
        description = R.string.look_and_feel_date_format_filename,
        icon = R.drawable.clarify,
        format = { _, name, _ ->
            name
        }
    ),

    Date(
        description = R.string.look_and_feel_date_format_date_taken,
        icon = R.drawable.acute,
        format = { _, _, datetime ->
            Instant.fromEpochSeconds(datetime)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .toJavaLocalDateTime()
                .format(DateTimeFormatter.ofPattern("EEE dd MMM yyyy"))
        },
    ),

    DateTime(
        description = R.string.look_and_feel_date_format_datetime_taken,
        icon = R.drawable.overview_filled,
        format = { context, _, datetime ->
            val is24Hr = android.text.format.DateFormat.is24HourFormat(context)

            Instant.fromEpochSeconds(datetime)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .toJavaLocalDateTime()
                .format(
                    DateTimeFormatter.ofPattern(
                        if (is24Hr) "EEE dd MMM yyyy HH:mm"
                        else "EEE dd MMM yyyy h:mm a"
                    )
                )
        }
    )
}

fun String.toPascalCase(): String {
    return this.lowercase().split(Regex("[\\s_-]+")) // Split by spaces, underscores, or hyphens
        .joinToString("") { word ->
            word.replaceFirstChar {
                it.uppercase()
            }
        }
}

fun Duration.formatLikeANormalPerson(): Pair<String, Boolean> {
    val longboi = this > 60.minutes
    val formatted = if (longboi) {
        this.toComponents { hours, minutes, seconds, _ ->
            String.format(
                Locale.ENGLISH,
                "%02d:%02d:%02d",
                hours,
                minutes,
                seconds
            )
        }
    } else {
        this.toComponents { minutes, seconds, _ ->
            String.format(
                Locale.ENGLISH,
                "%02d:%02d",
                minutes,
                seconds
            )
        }
    }
    return Pair(formatted, longboi)
}

fun String.immichDurationToSecondsOrNull(): Long? {
    val stripped = replace(".", "").replace(":", "")

    if (isBlank() || stripped.all { it == '0' }) return null

    val duration = split(".")[0]
    val split = duration.split(":").reversed()

    val seconds = split.getOrNull(0)?.toLongOrNull() ?: 0L
    val minutes = split.getOrNull(1)?.toLongOrNull() ?: 0L
    val hours = split.getOrNull(2)?.toLongOrNull() ?: 0L
    val days = split.getOrNull(3)?.toLongOrNull() ?: 0L

    return days * 86400L + hours * 3600L + minutes * 60L + seconds
}

/** @param n is the precision of the output */
fun Long.bytesToGB(n: Int = 2) = ((this.toDouble() / (1024 * 1024 * 1024)) * 10f.pow(n)).roundToLong() / 10f.pow(n)

fun SharedLinkResponse.expiryDate(context: Context): String? {
    if (expiresAt == null) return null

    val is24Hr = android.text.format.DateFormat.is24HourFormat(context)

    return Instant.parse(expiresAt!!)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .toJavaLocalDateTime()
        .format(
            DateTimeFormatter.ofPattern(
                if (is24Hr) "MMM dd, yyyy - HH:mm"
                else "MMM dd yyyy - h:mm a"
            )
        )
}

fun SharedLinkResponse.creationDate(context: Context): String? {
    val is24Hr = android.text.format.DateFormat.is24HourFormat(context)

    return Instant.parse(createdAt)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .toJavaLocalDateTime()
        .format(
            DateTimeFormatter.ofPattern(
                if (is24Hr) "MMM dd, yyyy - HH:mm"
                else "MMM dd yyyy - h:mm a"
            )
        )
}