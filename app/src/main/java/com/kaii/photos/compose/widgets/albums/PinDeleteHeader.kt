package com.kaii.photos.compose.widgets.albums

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kaii.photos.R
import com.kaii.photos.reorderable_lists.SortableGridState

fun LazyGridScope.pinDeleteHeader(
    sortableGridState: SortableGridState,
    removeAlbumIcon: Int
) {
    stickyHeader(
        key = "PinDeleteRow"
    ) {
        val animatedRowHeight by animateDpAsState(
            targetValue = if (sortableGridState.selectedItem != null) 104.dp else 0.dp,
            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
        )

        Row(
            modifier = Modifier
                .height(animatedRowHeight)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
        ) {
            val pinAlbumColor by animateColorAsState(
                targetValue =
                    when (sortableGridState.pinAlbumState) {
                        SortableGridState.PinAlbumState.Pinning -> MaterialTheme.colorScheme.primary
                        else -> Color.Transparent
                    },
                animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .clip(CircleShape)
                    .background(pinAlbumColor)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        id =
                            if (sortableGridState.selectedItem?.pinned == true) R.drawable.keep_off
                            else R.drawable.keep
                    ),
                    contentDescription = stringResource(id = R.string.albums_pin),
                    tint = MaterialTheme.colorScheme.contentColorFor(pinAlbumColor)
                )
            }

            val deleteAlbumColor by animateColorAsState(
                targetValue =
                    when (sortableGridState.deleteAlbumState) {
                        SortableGridState.DeleteAlbumState.Deleting -> MaterialTheme.colorScheme.primary
                        SortableGridState.DeleteAlbumState.NotAllowed -> MaterialTheme.colorScheme.error
                        else -> Color.Transparent
                    },
                animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .clip(CircleShape)
                    .background(deleteAlbumColor)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        id =
                            if (sortableGridState.deleteAlbumState == SortableGridState.DeleteAlbumState.NotAllowed) R.drawable.block
                            else removeAlbumIcon
                    ),
                    contentDescription = stringResource(id = R.string.albums_remove),
                    tint = MaterialTheme.colorScheme.contentColorFor(deleteAlbumColor)
                )
            }
        }
    }
}