package com.kaii.photos.helpers.exif

import com.kaii.photos.helpers.round
import kotlin.math.roundToInt

fun Double.toFraction(): String =
        if (this in 0f..1f) {
            val x = (1 / this).roundToInt()
            "1/${x}s"
        } else {
            "${this.round()}s"
        }

