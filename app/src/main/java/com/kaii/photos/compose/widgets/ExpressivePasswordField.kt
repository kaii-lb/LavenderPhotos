package com.kaii.photos.compose.widgets

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kaii.photos.compose.modifiers.wiggle
import com.kaii.photos.widgets.ExpressivePasswordFieldState

@Preview
@Composable
private fun ExpressivePasswordFieldPreview() {
    ExpressivePasswordField(
        code = { emptyList() },
        status = { ExpressivePasswordFieldState.Status.Idle },
        modifier = Modifier
            .size(300.dp, 40.dp)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressivePasswordField(
    code: () -> List<ExpressivePasswordFieldState.Code>,
    status: () -> ExpressivePasswordFieldState.Status,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .wiggle(
                start = status() == ExpressivePasswordFieldState.Status.Error,
                ratio = 0.025f,
                durationMillis = 200
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                space = 4.dp,
                alignment = Alignment.CenterHorizontally
            )
        ) {
            repeat(ExpressivePasswordFieldState.MAX_CODE_LENGTH) {
                val color by animateColorAsState(
                    targetValue = when (status()) {
                        ExpressivePasswordFieldState.Status.Successful -> Color(0xFFA2CB8B) // TODO: move to app theme
                        ExpressivePasswordFieldState.Status.Error -> MaterialTheme.colorScheme.error
                        ExpressivePasswordFieldState.Status.Idle -> MaterialTheme.colorScheme.primary
                    }
                )

                Box(
                    modifier = Modifier
                        .size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .border(
                                width = 2.dp,
                                color = color,
                                shape = CircleShape
                            )
                    )

                    val scale by animateFloatAsState(
                        targetValue =
                            if (it in 0 until code().size) 1f else 0f,
                        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
                    )

                    Box(
                        modifier = Modifier
                            .size(28.dp * scale)
                            .clip(
                                code().getOrNull(it)?.shape?.toShape() ?: CircleShape
                            )
                            .background(color)
                    )
                }
            }
        }
    }
}