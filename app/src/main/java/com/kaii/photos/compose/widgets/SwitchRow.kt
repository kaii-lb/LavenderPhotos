package com.kaii.photos.compose.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kaii.photos.compose.dialogs.getDefaultShapeSpacerForPosition
import com.kaii.photos.helpers.RowPosition

@Composable
fun SwitchRow(
    text: String,
    position: RowPosition = RowPosition.Single,
    showBackground: Boolean = false,
    padding: PaddingValues = PaddingValues(all = 8.dp),
    checked: () -> Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val (shape, _) = getDefaultShapeSpacerForPosition(
        position = position,
        cornerRadius = 32.dp,
        innerCornerRadius = 8.dp
    )

    Box(
        modifier = Modifier
            .clip(shape)
            .background(
                if (showBackground) MaterialTheme.colorScheme.surfaceContainer
                else Color.Transparent
            )
            .padding(padding)
    ) {
        Row(
            modifier = Modifier
                .clickable(
                    interactionSource = null,
                    indication = null
                ) {
                    onCheckedChange(!checked())
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .weight(1f)
            )

            Switch(
                checked = checked(),
                onCheckedChange = onCheckedChange
            )
        }
    }
}