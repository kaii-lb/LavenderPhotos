package com.kaii.photos.compose.widgets.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaii.photos.R
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.presentation.ui.theme.LavenderThemes
import com.kaii.photos.presentation.ui.theme.ThemeConfiguration
import com.kaii.photos.ui.theme.PhotosTheme

@Preview
@Composable
private fun PreferencesStyleRowPreview() {
    PhotosTheme(theme = ThemeConfiguration.Default) {
        PreferencesStyleRow(
            styles = LavenderThemes.styles,
            selected = { it == LavenderThemes.Style.System },
            position = RowPosition.Single,
            modifier = Modifier
                .width(300.dp),
            onSelect = {}
        )
    }
}

@Composable
fun PreferencesStyleRow(
    styles: List<LavenderThemes.Style>,
    selected: (style: LavenderThemes.Style) -> Boolean,
    position: RowPosition,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onSelect: (style: LavenderThemes.Style) -> Unit
) {
    CustomBodyPreferencesRow(
        title = stringResource(id = R.string.look_and_feel_style),
        summary = stringResource(id = R.string.look_and_feel_style_desc),
        icon = R.drawable.paintbrush,
        modifier = modifier,
        position = position,
        enabled = enabled
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            verticalAlignment = Alignment.CenterVertically
        ) {
            styles.forEachIndexed { index, style ->
                ToggleButton(
                    checked = selected(style),
                    onCheckedChange = { onSelect(style) },
                    colors = ToggleButtonDefaults.tonalToggleButtonColors(),
                    shapes =
                        when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            styles.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                    enabled = enabled,
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Text(
                        text = stringResource(id = style.labelId),
                        style = MaterialTheme.typography.bodyMedium,
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = 12.sp,
                            maxFontSize = 14.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}