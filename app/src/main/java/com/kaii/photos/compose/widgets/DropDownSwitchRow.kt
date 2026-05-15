package com.kaii.photos.compose.widgets

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun TextDropDownSwitchRow(
    text: String,
    checked: () -> Boolean,
    textFieldValue: () -> String?,
    placeholder: String,
    @DrawableRes icon: Int,
    shape: Shape,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit,
    onTextFieldValueChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    DropDownSwitchRow(
        text = text,
        checked = checked,
        shape = shape,
        modifier = modifier,
        onCheckedChange = onCheckedChange,
        content = {
            ClearableTextField(
                value = textFieldValue() ?: "",
                onValueChange = onTextFieldValueChange,
                placeholder = placeholder,
                icon = icon,
                onClear = {
                    onTextFieldValueChange("")
                },
                onConfirm = onConfirm
            )
        }
    )
}

@Composable
fun DropDownSwitchRow(
    text: String,
    checked: () -> Boolean,
    shape: Shape,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 8.dp, vertical = 0.dp)
    ) {
        SwitchRow(
            text = text,
            checked = checked,
            onCheckedChange = onCheckedChange
        )

        AnimatedVisibility(
            visible = checked(),
            enter = fadeIn() + expandVertically(
                animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                expandFrom = Alignment.Top
            ),
            exit = fadeOut() + shrinkVertically(
                animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                shrinkTowards = Alignment.Top
            )
        ) {
            Column {
                content()
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}