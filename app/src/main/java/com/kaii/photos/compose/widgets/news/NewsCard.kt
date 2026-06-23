package com.kaii.photos.compose.widgets.news

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.getDefaultShapeSpacerForPosition
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.ui.theme.PhotosTheme

@Preview
@Composable
private fun NewsCardPreview() {
    PhotosTheme(theme = 2) {
        NewsCard(
            title = "This is a multiline news item, this text is very long, yes, indeed its so long it could fill up multiple lines",
            issueNumber = 123,
            position = RowPosition.Single,
            modifier = Modifier
                .width(300.dp)
        )
    }
}

@Composable
fun NewsCard(
    title: String,
    issueNumber: Int?,
    position: RowPosition,
    modifier: Modifier = Modifier
) {
    val (shape, _) = getDefaultShapeSpacerForPosition(position, 32.dp, 4.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            space = 16.dp,
            alignment = Alignment.Start
        )
    ) {
        Box(
            modifier = Modifier
                .size(8.dp, 4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )

        Column(
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(
                space = 4.dp,
                alignment = Alignment.CenterVertically
            )
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (issueNumber != null) {
                Text(
                    text = buildAnnotatedString {
                        val githubTag = stringResource(id = R.string.news_github_issue_number, issueNumber)
                        val endIndex = githubTag.indexOf(':') + 2 // +2 for '#' and ' '

                        append(githubTag.substring(0, endIndex))

                        addStyle(
                            style = SpanStyle(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)),
                            start = 0,
                            end = endIndex
                        )

                        withLink(
                            link = LinkAnnotation.Url(
                                url = "https://github.com/kaii-lb/LavenderPhotos/issues/$issueNumber",
                                styles = TextLinkStyles(
                                    style = SpanStyle(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                        textDecoration = TextDecoration.Underline
                                    )
                                )
                            )
                        ) {
                            append(githubTag.substring(endIndex))
                        }
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}