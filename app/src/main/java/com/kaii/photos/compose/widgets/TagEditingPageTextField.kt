package com.kaii.photos.compose.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
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
    exists: (name: String) -> Boolean
) {
    Row(
        modifier = modifier
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .clip(
                    RoundedCornerShape(
                        topStart = 1000.dp, topEnd = 6.dp,
                        bottomStart = 1000.dp, bottomEnd = 6.dp
                    )
                )
                .background(TextFieldDefaults.colors().focusedContainerColor)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(48.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 1000.dp, topEnd = 4.dp,
                            bottomStart = 1000.dp, bottomEnd = 4.dp
                        )
                    )
                    .background(Color.Blue)
            )
        }

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
                            onClick = {

                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.checkmark_thin),
                                contentDescription = stringResource(id = R.string.media_confirm)
                            )
                        }
                    }
                }
            },
            shape = RoundedCornerShape(
                topStart = 6.dp, topEnd = 1000.dp,
                bottomStart = 6.dp, bottomEnd = 1000.dp
            ),
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