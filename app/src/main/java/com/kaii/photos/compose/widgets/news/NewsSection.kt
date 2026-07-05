package com.kaii.photos.compose.widgets.news

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaii.photos.R
import com.kaii.photos.domain.news.News
import com.kaii.photos.presentation.ui.theme.ThemeConfiguration
import com.kaii.photos.ui.theme.LocalExtraColorsPalette
import com.kaii.photos.ui.theme.PhotosTheme

@Preview
@Composable
private fun NewsSectionPreview() {
    PhotosTheme(theme = ThemeConfiguration.Default) {
        NewsSection(
            version = "v2.0.0",
            date = "20-07-2026",
            status = News.Section.Status.Latest,
            modifier = Modifier
                .width(300.dp)
        )
    }
}

@Composable
fun NewsSection(
    version: String,
    date: String,
    status: News.Section.Status,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = Alignment.Start
        )
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .padding(
                    top = 4.dp, bottom = 4.dp,
                    start = 8.dp, end = 5.dp // shoter because the "v" takes weird space
                )
        ) {
            Text(
                text = version,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        if (status != News.Section.Status.None) {
            Text(
                text = "-",
                color =
                    if (status == News.Section.Status.Latest) LocalExtraColorsPalette.current.success
                    else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                fontWeight = FontWeight.Bold
            )

            Text(
                text =
                    if (status == News.Section.Status.Latest) stringResource(id = R.string.news_version_latest)
                    else stringResource(id = R.string.news_version_broken),
                color =
                    if (status == News.Section.Status.Latest) LocalExtraColorsPalette.current.success
                    else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = date,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}