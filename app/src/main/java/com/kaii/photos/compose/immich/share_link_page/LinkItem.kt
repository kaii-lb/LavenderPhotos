package com.kaii.photos.compose.immich.share_link_page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.getDefaultShapeSpacerForPosition
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.creationDate
import io.github.kaii_lb.lavender.immichintegration.serialization.shared_links.SharedLinkResponseDto

@Preview
@Composable
private fun Preview() {
    LinkItem(
        title = "Cat Pictures",
        summary = "May 16, 2026 - Download - Upload - Metadata",
        expiresAt = "May 17, 2026",
        position = RowPosition.Single,
        modifier = Modifier
            .width(400.dp),
        copyLink = {},
        deleteLink = {}
    )
}

@Composable
fun getLinkItemDescription(
    link: SharedLinkResponseDto,
) = buildString {
    append(link.creationDate(context = LocalContext.current))

    if (link.allowDownload) {
        val download = stringResource(id = R.string.download)
        append(" - $download")
    }

    if (link.allowDownload) {
        val upload = stringResource(id = R.string.upload)
        append(" - $upload")
    }

    if (link.showMetadata) {
        val metadata = stringResource(id = R.string.metadata)
        append(" - $metadata")
    }

    if (link.password != null) {
        val password = stringResource(id = R.string.immich_auth_password)
        append(" - $password")
    }
}

@Composable
fun LinkItem(
    title: String,
    summary: String,
    expiresAt: String?,
    position: RowPosition,
    modifier: Modifier = Modifier,
    copyLink: () -> Unit,
    deleteLink: () -> Unit
) {
    val (shape, _) = getDefaultShapeSpacerForPosition(
        position = position,
        cornerRadius = 32.dp,
        innerCornerRadius = 8.dp
    )

    Row(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(all = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (expiresAt != null) {
                Text(
                    text = stringResource(id = R.string.immich_share_album_expires_at, expiresAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(
            onClick = copyLink
        ) {
            Icon(
                painter = painterResource(id = R.drawable.link),
                contentDescription = stringResource(id = R.string.copy_link)
            )
        }

        FilledIconButton(
            onClick = deleteLink
        ) {
            Icon(
                painter = painterResource(id = R.drawable.delete),
                contentDescription = stringResource(id = R.string.immich_share_album_delete_link)
            )
        }
    }
}