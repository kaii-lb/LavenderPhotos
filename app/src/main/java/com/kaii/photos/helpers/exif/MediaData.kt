package com.kaii.photos.helpers.exif

import com.kaii.photos.R

enum class MediaData(val icon: Int, val description: Int) {
    Name(icon = R.drawable.name, description = R.string.exif_name),
    Path(icon = R.drawable.folder, description = R.string.exif_path),
    Date(icon = R.drawable.calendar, description = R.string.exif_date),
    LatLong(icon = R.drawable.location, description = R.string.exif_latlong),
    Device(icon = R.drawable.camera, description = R.string.exif_device),
    FNumber(icon = R.drawable.light, description = R.string.exif_fnumber),
    ShutterSpeed(icon = R.drawable.shutter_speed, description = R.string.exif_shutter_speed),
    MegaPixels(icon = R.drawable.maybe_megapixel, description = R.string.exif_mp),
    Resolution(icon = R.drawable.resolution, description = R.string.exif_res),
    Size(icon = R.drawable.storage, description = R.string.exif_size)
}