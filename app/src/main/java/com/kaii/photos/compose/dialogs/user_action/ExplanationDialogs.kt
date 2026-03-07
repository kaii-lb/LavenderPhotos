package com.kaii.photos.compose.dialogs.user_action

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.LavenderDialogBase
import com.kaii.photos.compose.pages.FullWidthDialogButton
import com.kaii.photos.helpers.RowPosition

@Composable
fun ExplanationDialog(
    title: String,
    explanation: String,
    showPreviousDialog: MutableState<Boolean>? = null,
    onDismiss: () -> Unit
) {
    ExplanationDialogBase(
        title = title,
        body = {
            Text(
                text = explanation,
                fontSize = TextUnit(14f, TextUnitType.Sp),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Justify,
                modifier = Modifier.wrapContentSize()
            )
        },
        onDismiss = onDismiss,
        showPreviousDialog = showPreviousDialog
    )
}

@Composable
fun AnnotatedExplanationDialog(
    title: String,
    annotatedExplanation: AnnotatedString,
    showPreviousDialog: MutableState<Boolean>? = null,
    onDismiss: () -> Unit
) {
    ExplanationDialogBase(
        title = title,
        body = {
            Text(
                text = annotatedExplanation,
                fontSize = TextUnit(14f, TextUnitType.Sp),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Justify,
                modifier = Modifier.wrapContentSize()
            )
        },
        showPreviousDialog = showPreviousDialog,
        onDismiss = onDismiss
    )
}

@Composable
private fun ExplanationDialogBase(
    title: String,
    body: @Composable () -> Unit,
    showPreviousDialog: MutableState<Boolean>? = null,
    onDismiss: () -> Unit
) {
    showPreviousDialog?.value = false

    LavenderDialogBase(
        usePlatformDefaultWidth = false,
        modifier = Modifier
            .animateContentSize()
            .width(300.dp),
        onDismiss = onDismiss
    ) {
        Text(
            text = title,
            fontSize = TextUnit(18f, TextUnitType.Sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.wrapContentSize()
        )

        Spacer(modifier = Modifier.height(24.dp))

        body()

        Spacer(modifier = Modifier.height(24.dp))

        FullWidthDialogButton(
            text = stringResource(id = R.string.media_okay),
            color = MaterialTheme.colorScheme.primary,
            textColor = MaterialTheme.colorScheme.onPrimary,
            position = RowPosition.Single
        ) {
            showPreviousDialog?.value = true

            onDismiss()
        }
    }
}