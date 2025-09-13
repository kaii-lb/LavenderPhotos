package com.kaii.photos.compose.editing_view.image_editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kaii.photos.compose.editing_view.EditingViewBottomAppBarItem
import com.kaii.photos.helpers.editing.ImageModification
import com.kaii.photos.helpers.editing.MediaAdjustments

@Composable
fun ImageEditorAdjustContent(
    modifier: Modifier = Modifier,
    modifications: SnapshotStateList<ImageModification>,
    increaseModCount: () -> Unit
) {
    LazyRow(
        modifier = modifier
            .fillMaxSize(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items(
            count = MediaAdjustments.entries.size
        ) { index ->
            val entry = MediaAdjustments.entries[index]

            ImageEditingAdjustmentItem(
                type = entry,
                modifications = modifications,
                extraOnClick = increaseModCount
            )
        }
    }
}

@Composable
private fun ImageEditingAdjustmentItem(
    type: MediaAdjustments,
    modifications: SnapshotStateList<ImageModification>,
    extraOnClick: () -> Unit
) {
    EditingViewBottomAppBarItem(
        text = stringResource(id = type.title),
        icon = type.icon,
        selected = (modifications.lastOrNull {
            it is ImageModification.Adjustment
        } as? ImageModification.Adjustment)?.type == type,
        onClick = {
            val last = modifications.lastOrNull {
                it is ImageModification.Adjustment && it.type == type
            } as? ImageModification.Adjustment

            if (last != null) {
                val latest = modifications.lastOrNull { it is ImageModification.Adjustment } as? ImageModification.Adjustment

                if (latest?.type == type) { // double click for reset value
                    modifications.remove(last)
                    modifications.add(last.copy(value = type.startValue))
                } else {
                    modifications.remove(last)
                    modifications.add(last)
                }
            } else {
                modifications.add(
                    ImageModification.Adjustment(
                        type = type,
                        value = type.startValue
                    )
                )
            }

            extraOnClick()
        }
    )
}