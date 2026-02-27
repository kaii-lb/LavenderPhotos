package com.kaii.photos.compose.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaii.photos.R
import com.kaii.photos.helpers.TextStylingConstants

@Composable
fun VersionInfoDialog(
    changelog: String,
    onDismiss: () -> Unit
) {
    LavenderDialogBase(
        onDismiss = onDismiss,
        modifier = Modifier
            .width(340.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(
                space = 8.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.changelog_name),
                fontSize = TextStylingConstants.LARGE_TEXT_SIZE.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            LazyColumn(
                modifier = Modifier
                    .height(320.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Text(
                        text = AnnotatedString.fromHtml(changelog),
                        fontSize = TextStylingConstants.SMALL_TEXT_SIZE.sp,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .height(56.dp)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onDismiss
                ) {
                    Text(
                        text = stringResource(id = R.string.close),
                        fontSize = TextStylingConstants.SMALL_TEXT_SIZE.sp,
                    )
                }
            }
        }
    }
}