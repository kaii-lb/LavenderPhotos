package com.kaii.photos.database.entities

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.ExifResponseDto

@Immutable
@Entity(
    tableName = "media_exif_data",
    indices = [
        Index(value = ["mediaId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = MediaStoreData::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("mediaId"),
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExifData(
    @PrimaryKey val mediaId: Long,
    val city: String?,
    val country: String?,
    val dateTimeOriginal: String?,
    val description: String?,
    val exifImageHeight: Int?,
    val exifImageWidth: Int?,
    val exposureTime: String?,
    val fNumber: Double?,
    val fileSizeInByte: Long?,
    val focalLength: Double?,
    val iso: Double?,
    val latitude: Double?,
    val longitude: Double?,
    val lensModel: String?,
    val make: String?,
    val model: String?,
    val modifyDate: String?,
    val orientation: String?,
    val projectionType: String?,
    val rating: Int?,
    val state: String?,
    val timeZone: String?
)

fun ExifResponseDto.toExifData(mediaId: Long) = ExifData(
    mediaId = mediaId,
    city = city,
    country = country,
    dateTimeOriginal = dateTimeOriginal,
    description = description,
    exifImageHeight = exifImageHeight,
    exifImageWidth = exifImageWidth,
    exposureTime = exposureTime,
    fNumber = fNumber,
    fileSizeInByte = fileSizeInByte,
    focalLength = focalLength,
    iso = iso?.toDouble(),
    latitude = latitude,
    longitude = longitude,
    lensModel = lensModel,
    make = make,
    model = model,
    modifyDate = modifyDate,
    orientation = orientation,
    projectionType = projectionType,
    rating = rating,
    state = state,
    timeZone = timeZone
)