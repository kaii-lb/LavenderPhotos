package com.kaii.photos.compose.dialogs.user_action

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.rememberDeviceOrientation

@Composable
fun ConfirmationDialog(
    showDialog: MutableState<Boolean>,
    dialogTitle: String,
    confirmButtonLabel: String,
    action: () -> Unit
) {
    val isLandscape by rememberDeviceOrientation()

    val modifier = if (isLandscape)
        Modifier.width(256.dp)
    else
        Modifier.Companion

    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = {
                showDialog.value = false
            },
            modifier = modifier,
            confirmButton = {
                Button(
                    onClick = {
                        showDialog.value = false
                        action()
                    }
                ) {
                    Text(
                        text = confirmButtonLabel,
                        fontSize = TextUnit(14f, TextUnitType.Sp)
                    )
                }
            },
            title = {
                Text(
                    text = dialogTitle,
                    fontSize = TextUnit(16f, TextUnitType.Sp)
                )
            },
            dismissButton = {
                Button(
                    onClick = {
                        showDialog.value = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.media_cancel),
                        fontSize = TextUnit(14f, TextUnitType.Sp)
                    )
                }
            },
            shape = RoundedCornerShape(32.dp)
        )
    }
}