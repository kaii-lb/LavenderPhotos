package com.kaii.photos.compose.immich.backup_options_page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kaii.photos.R

@Preview
@Composable
private fun ImmichBackupOptionsPageHeaderPreview() {
    ImmichBackupOptionsPageHeader(
        selectedCount = { 5 },
        assetCount = { 1234 }
    )
}

@Composable
fun ImmichBackupOptionsPageHeader(
    selectedCount: () -> Int,
    assetCount: () -> Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(size = 32.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(all = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                alignment = Alignment.CenterVertically
            ),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = stringResource(id = R.string.immich_backup_option_cloud_synced_albums, selectedCount()),
                style = MaterialTheme.typography.bodyLargeEmphasized
            )

            Text(
                text = stringResource(id = R.string.immich_backup_option_total_uploaded_count, assetCount()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
            )
        }

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.cloud_upload_filled),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(56.dp)
                    .offset(y = (-2).dp)
            )
        }
    }
}