package com.kaii.photos.helpers

import androidx.compose.ui.graphics.Color

fun brightenColor(color: Color, percentage: Float) : Color {
	val r = (color.red * 255).toInt()
	val g = (color.green * 255).toInt()
	val b = (color.blue * 255).toInt()

	val newR = (r + (255 - r) * percentage).toInt().coerceIn(0, 255)
	val newG = (g + (255 - g) * percentage).toInt().coerceIn(0, 255)
	val newB = (b + (255 - b) * percentage).toInt().coerceIn(0, 255)

	return Color(newR, newG, newB)
}
