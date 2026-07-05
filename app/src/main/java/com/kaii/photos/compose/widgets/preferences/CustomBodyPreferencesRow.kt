package com.kaii.photos.compose.widgets.preferences

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kaii.photos.compose.dialogs.getDefaultShapeSpacerForPosition
import com.kaii.photos.helpers.RowPosition

@Composable
fun CustomBodyPreferencesRow(
    title: String,
    summary: String,
    @DrawableRes icon: Int,
    position: RowPosition,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val (shape, _) = getDefaultShapeSpacerForPosition(position, 32.dp, 8.dp)

    Column(
        modifier = modifier
            .clip(shape)
            .background(color = MaterialTheme.colorScheme.surfaceContainer.copy(
                alpha = if (enabled) 1f else 0.8f
            ))
            .padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = if (enabled) 1f else 0.8f
                ),
                modifier = Modifier
                    .size(28.dp)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = if (enabled) 1f else 0.8f
                )
            )
        }

        Text(
            text = summary,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Start,
            color = MaterialTheme.colorScheme.onSurface.copy(
                alpha = if (enabled) 0.8f else 0.6f
            )
        )

        Box(
            modifier = Modifier
                .clip(shape = RoundedCornerShape(size = 16.dp))
        ) {
            content()
        }
    }
}