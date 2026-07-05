package com.kaii.photos.compose.widgets.news

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kaii.photos.R
import com.kaii.photos.domain.news.News
import com.kaii.photos.presentation.ui.theme.ThemeConfiguration
import com.kaii.photos.ui.theme.PhotosTheme

@Preview
@Composable
private fun NewsNotePreview() {
    PhotosTheme(theme = ThemeConfiguration.Default) {
        NewsNote(
            info = "This is a note.",
            urgency = News.Note.Urgency.Critical,
            modifier = Modifier
                .width(300.dp)
        )
    }
}

@Composable
fun NewsNote(
    info: String,
    urgency: News.Note.Urgency,
    modifier: Modifier = Modifier
) {
    val backgroundColor =
        when (urgency) {
            News.Note.Urgency.Normal -> MaterialTheme.colorScheme.secondaryContainer
            News.Note.Urgency.Critical -> MaterialTheme.colorScheme.error
        }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(size = 32.dp))
            .background(backgroundColor)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            space = 16.dp,
            alignment = Alignment.Start
        )
    ) {
        Column(
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(
                space = 4.dp,
                alignment = Alignment.CenterVertically
            )
        ) {
            Text(
                text = stringResource(id = R.string.news_note, info),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.contentColorFor(backgroundColor)
            )
        }
    }
}