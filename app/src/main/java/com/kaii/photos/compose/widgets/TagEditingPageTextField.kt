package com.kaii.photos.compose.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaii.photos.R
import com.kaii.photos.helpers.TextStylingConstants

@Composable
fun TagEditingPageTextField(
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (value: String) -> Unit,
    exists: (name: String) -> Boolean,
    addTag: (name: String) -> Unit
) {
    Row(
        modifier = modifier
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 4.dp)
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = stringResource(id = R.string.tags_add)
                )
            },
            suffix = {
                if (exists(value)) {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                            positioning = TooltipAnchorPosition.Start,
                            spacingBetweenTooltipAndAnchor = 16.dp
                        ),
                        tooltip = {
                            PlainTooltip(
                                shape = CircleShape,
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                                modifier = Modifier
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.tags_exists),
                                    fontSize = TextStylingConstants.SMALL_TEXT_SIZE.sp
                                )
                            }
                        },
                        state = rememberTooltipState()
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.error_2),
                            contentDescription = stringResource(id = R.string.tags_exists)
                        )
                    }
                }
            },
            trailingIcon = {
                if (!exists(value)) {
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = { addTag(value) }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.checkmark_thin),
                                contentDescription = stringResource(id = R.string.media_confirm)
                            )
                        }
                    }
                }
            },
            shape = CircleShape,
            colors = TextFieldDefaults.colors(
                errorIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = modifier
                .weight(1f)
        )
    }
}