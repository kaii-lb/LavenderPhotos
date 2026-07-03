package com.kaii.photos.compose.widgets.tags

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaii.photos.database.entities.Tag
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.presentation.ui.ColorCreator

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TagItem(
    tag: Tag,
    selected: Boolean,
    colorCreator: ColorCreator,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else tag.color,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()
    )

    Box(
        modifier = modifier
            .wrapContentWidth()
            .clip(CircleShape)
            .background(tag.color)
            .border(
                width = 2.dp,
                color = borderColor,
                shape = CircleShape
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onRemove
            )
            .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = tag.name,
            fontSize = TextStylingConstants.SMALL_TEXT_SIZE.sp,
            color = colorCreator.onColorFor(tag.color)
        )
    }
}