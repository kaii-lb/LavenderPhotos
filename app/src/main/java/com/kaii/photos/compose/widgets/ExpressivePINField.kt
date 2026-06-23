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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kaii.photos.compose.modifiers.wiggle
import com.kaii.photos.ui.theme.LocalExtraColorsPalette
import com.kaii.photos.widgets.ExpressivePINFieldState

@Preview
@Composable
private fun ExpressivePINFieldPreview() {
    ExpressivePINField(
        code = { emptyList() },
        status = { ExpressivePINFieldState.Status.Idle },
        modifier = Modifier
            .size(300.dp, 40.dp)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressivePINField(
    code: () -> List<ExpressivePINFieldState.Code>,
    status: () -> ExpressivePINFieldState.Status,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .wiggle(
                start = status() == ExpressivePINFieldState.Status.Error,
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
            repeat(ExpressivePINFieldState.MAX_CODE_LENGTH) {
                val color by animateColorAsState(
                    targetValue = when (status()) {
                        ExpressivePINFieldState.Status.Successful -> LocalExtraColorsPalette.current.success
                        ExpressivePINFieldState.Status.Error -> MaterialTheme.colorScheme.error
                        ExpressivePINFieldState.Status.Idle -> MaterialTheme.colorScheme.primary
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