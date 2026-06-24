package com.kaii.photos.presentation.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.random.Random

class ColorCreator {
    fun generateColor(): Color {
        val r = Random.nextInt(from = 100, until = 220)
        val b = Random.nextInt(from = 100, until = 220)
        val g = Random.nextInt(from = 100, until = 220)

        return Color(r, g, b)
    }

    fun onColorFor(color: Color): Color {
        return if (color.luminance() > 0.5) Color.Black else Color.White
    }
}