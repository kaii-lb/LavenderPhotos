package com.kaii.photos.compose.dialogs.album_info

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaii.photos.R
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.helpers.TextStylingConstants

@Composable
fun NestedDisplayToggle(
    album: () -> AlbumType.Folder,
    modifier: Modifier = Modifier,
    containerColor: Color =
        if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    editAlbum: (id: String, newInfo: AlbumType) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(shape = RoundedCornerShape(32.dp))
            .background(color = containerColor.copy(alpha = if (enabled) 1f else 0.4f))
            .clickable(enabled = enabled) {
                editAlbum(
                    album().id,
                    album().copy(
                        showNested = !album().showNested
                    )
                )
            }
            .padding(all = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(id = R.string.albums_toggle_nested_display),
            fontSize = TextStylingConstants.MEDIUM_TEXT_SIZE.sp,
            fontWeight = FontWeight.SemiBold,
            color = contentColor.copy(alpha = if (enabled) 1f else 0.4f)
        )

        Switch(
            checked = album().showNested,
            onCheckedChange = { checked ->
                editAlbum(
                    album().id,
                    album().copy(
                        showNested = checked
                    )
                )
            }
        )
    }
}