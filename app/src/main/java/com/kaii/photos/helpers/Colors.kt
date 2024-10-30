package com.kaii.photos.helpers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

fun brightenColor(color: Color, percentage: Float) : Color {
	val r = (color.red * 255).toInt()
	val g = (color.green * 255).toInt()
	val b = (color.blue * 255).toInt()

	val newR = (r + (255 - r) * percentage).toInt().coerceIn(0, 255)
	val newG = (g + (255 - g) * percentage).toInt().coerceIn(0, 255)
	val newB = (b + (255 - b) * percentage).toInt().coerceIn(0, 255)

	return Color(newR, newG, newB)
}

fun darkenColor(color: Color, percentage: Float) : Color {
	val r = (color.red * 255).toInt()
	val g = (color.green * 255).toInt()
	val b = (color.blue * 255).toInt()

	val newR = (r * (1 - percentage)).toInt().coerceIn(0, 255)
	val newG = (g * (1 - percentage)).toInt().coerceIn(0, 255)
	val newB = (b * (1 - percentage)).toInt().coerceIn(0, 255)

	return Color(newR, newG, newB)
}

class DrawingColors {
	companion object {
		/** white white */
		val White = Color.White

		/** black black */
		val Black = Color.Black

		/** poppy red */
		val Red = Color(red = 227, green = 83, blue = 53)

		/** lemon yellow */
		val Yellow = Color(red = 250, green = 250, blue = 51)

		/** emerald green */
		val Green = Color(red = 80, green = 200, blue = 120)

		/** bright blue */
		val Blue = Color(red = 0, green = 150, blue = 255)

		/** lavender purple */
		val Purple = Color(red = 204, green = 204, blue = 255)
	}
}

@Composable
fun ColorIndicator(
	color: Color,
	selected: Boolean,
	modifier: Modifier = Modifier,
	onClick: () -> Unit
) {
	Box(
		modifier = Modifier
			.size(40.dp)
			.clip(CircleShape)
			.background(
				if (selected) CustomMaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
					else CustomMaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
			)
			.clickable {
				onClick()
			}
			.then(modifier)
	) {
		Box(
			modifier = Modifier
				.size(32.dp)
				.clip(CircleShape)
				.background(color)
				.align(Alignment.Center)
		)
	}
}