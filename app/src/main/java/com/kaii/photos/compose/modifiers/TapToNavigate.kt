package com.kaii.photos.compose.modifiers

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.tapOnScreenHalves(
    enabled: Boolean = true,
    onTap: (isLeftSideTap: Boolean) -> Unit
): Modifier {
    val layoutDirection = LocalLayoutDirection.current

    return this then if (!enabled) Modifier
    else Modifier.pointerInput(Unit) {
        val areaHeight = 0.3f * size.height

        val top = (size.height - areaHeight) / 2f
        val bottom = top + areaHeight
        val touchSlop = viewConfiguration.touchSlop

        fun inTargetArea(position: Offset): Boolean =
            position.y in top..bottom &&
                    (position.x < 32.dp.toPx() || position.x > size.width - 32.dp.toPx())

        awaitEachGesture {
            val down = awaitFirstDown(pass = PointerEventPass.Initial)

            if (inTargetArea(down.position)) {
                down.consume()
                var isTap = true

                while (true) {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    val change = event.changes.firstOrNull() ?: break

                    if ((change.position - down.position).getDistance() > touchSlop || !inTargetArea(change.position)) {
                        isTap = false
                    }

                    change.consume()

                    if (!change.pressed) {
                        if (isTap) {
                            if (down.position.x < 32.dp.toPx()) onTap(layoutDirection == LayoutDirection.Ltr)
                            else if (down.position.x > size.width - 32.dp.toPx()) onTap(layoutDirection != LayoutDirection.Ltr)
                        }

                        break
                    }
                }
            }
        }
    }
}
