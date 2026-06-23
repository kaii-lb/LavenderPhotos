package com.kaii.photos.compose.videoplayer

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.SelectableDropDownMenuItem
import com.kaii.photos.screens.video.LavenderExoPlayer

@Composable
fun AudioSelectorMenu(
    expanded: () -> Boolean,
    selectedTrack: () -> LavenderExoPlayer.AudioTrack?,
    tracks: () -> List<LavenderExoPlayer.AudioTrack>,
    setTrack: (language: String) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded(),
        shape = RoundedCornerShape(size = 24.dp),
        offset = DpOffset(x = 0.dp, y = (-8).dp),
        modifier = Modifier
            .padding(horizontal = 8.dp),
        onDismissRequest = onDismiss
    ) {
        tracks().forEach { track ->
            SelectableDropDownMenuItem(
                isSelected = selectedTrack() == track,
                iconResId = R.drawable.checkmark_thin,
                text = track.label,
                onClick = {
                    setTrack(track.language)
                }
            )
        }
    }
}