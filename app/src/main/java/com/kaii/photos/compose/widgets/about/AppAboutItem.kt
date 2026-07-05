package com.kaii.photos.compose.widgets.about

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.kaii.photos.R
import com.kaii.photos.presentation.ui.theme.ThemeConfiguration
import com.kaii.photos.ui.theme.PhotosTheme

@Preview
@Composable
private fun AppAboutItemPreview() {
    PhotosTheme(theme = ThemeConfiguration.Default) {
        AppAboutItem(
            version = "v2.0.0",
            showNews = {}
        )
    }
}

@Composable
fun AppAboutItem(
    version: String,
    modifier: Modifier = Modifier,
    showNews: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 32.dp))
            .background(color = MaterialTheme.colorScheme.primaryContainer)
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(size = 32.dp)
            )
            .padding(all = 12.dp),
        verticalArrangement = Arrangement.spacedBy(
            space = 16.dp,
            alignment = Alignment.CenterVertically
        ),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                space = 16.dp,
                alignment = Alignment.Start
            )
        ) {
            Image(
                painter = painterResource(id = R.drawable.lavender_no_padding),
                contentDescription = stringResource(id = R.string.app_name_full),
                colorFilter = ColorFilter.tint(
                    color = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(color = MaterialTheme.colorScheme.primary)
                    .padding(all = 12.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(
                    space = 4.dp
                )
            ) {
                Text(
                    text = stringResource(id = R.string.app_name_full),
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = stringResource(id = R.string.app_description),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                space = 4.dp,
                alignment = Alignment.Start
            )
        ) {
            TextButton(
                onClick = showNews,
                colors = ButtonDefaults.textButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                shapes = ButtonDefaults.shapes(
                    shape = CircleShape,
                    pressedShape = RoundedCornerShape(size = 10.dp)
                )
            ) {
                Text(
                    text = stringResource(id = R.string.app_version, version.replace("v", "")),
                    style = MaterialTheme.typography.titleSmall
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            FilledIconButton(
                onClick = {
                    val intent = Intent().apply {
                        action = Intent.ACTION_VIEW
                        data = "https://github.com/kaii-lb/LavenderPhotos/".toUri()
                    }

                    context.startActivity(intent)
                },
                shapes = IconButtonDefaults.shapes(
                    shape = CircleShape,
                    pressedShape = RoundedCornerShape(size = 10.dp)
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.code),
                    contentDescription = stringResource(id = R.string.app_repo)
                )
            }

            FilledIconButton(
                onClick = {
                    val intent = Intent().apply {
                        action = Intent.ACTION_VIEW
                        data = "https://github.com/kaii-lb/LavenderPhotos/releases".toUri()
                    }

                    context.startActivity(intent)
                },
                shapes = IconButtonDefaults.shapes(
                    shape = CircleShape,
                    pressedShape = RoundedCornerShape(size = 10.dp)
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.download),
                    contentDescription = stringResource(id = R.string.app_get)
                )
            }
        }
    }
}