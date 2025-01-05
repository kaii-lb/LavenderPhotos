package com.kaii.photos.compose

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.kaii.photos.R
import com.kaii.photos.helpers.CustomMaterialTheme

@Composable
fun SplitButton(
    enabled: Boolean = true,
    secondaryContentMaxWidth: Dp = 1000.dp,
    primaryContentPadding: PaddingValues = PaddingValues(11.dp),
    secondaryContentPadding: PaddingValues = PaddingValues(0.dp, 5.dp, 4.dp, 5.dp),
    primaryContainerColor: Color = CustomMaterialTheme.colorScheme.primary,
    secondaryContainerColor: Color = CustomMaterialTheme.colorScheme.primary,
    primaryContent: @Composable () -> Unit,
    secondaryContent: @Composable () -> Unit,
    primaryAction: () -> Unit,
    secondaryAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .wrapContentSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = {
                primaryAction()
            },
            shape = RoundedCornerShape(1000.dp, 4.dp, 4.dp, 1000.dp),
            enabled = enabled,
            colors = ButtonDefaults.buttonColors().copy(
                containerColor = primaryContainerColor
            ),
            contentPadding = primaryContentPadding,
            modifier = Modifier
            	.widthIn(min = 40.dp)
        ) {
            primaryContent()
        }

        Spacer(modifier = Modifier.width(4.dp))

        Button(
            onClick = secondaryAction,
            shape = RoundedCornerShape(4.dp, 1000.dp, 1000.dp, 4.dp),
            enabled = enabled,
            colors = ButtonDefaults.buttonColors().copy(
                containerColor = secondaryContainerColor
            ),
            contentPadding = secondaryContentPadding,
            modifier = Modifier
            	.widthIn(min = 20.dp, max = secondaryContentMaxWidth)
            	.wrapContentSize()
                .animateContentSize()
        ) {
            secondaryContent()
        }
    }
}

@Composable
fun SelectableDropDownMenuItem(
    text: String,
    iconResId: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                fontSize = TextUnit(14f, TextUnitType.Sp),
            )
        },
        onClick = onClick,
        trailingIcon = {
            if (isSelected) {
                Icon(
                    painter = painterResource(id = iconResId),
                    contentDescription = "This save option is selected",
                    tint = CustomMaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .size(24.dp)
                )
            }
        }
    )
}
