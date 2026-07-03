package com.kaii.photos.compose.immich.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.PreferenceRowWithCustomBody
import com.kaii.photos.domain.immich.ImmichServerInfo

@Composable
fun ServerStorageRow(
    serverInfo: () -> ImmichServerInfo?,
    enabled: () -> Boolean
) {
    val resources = LocalResources.current

    val storage by remember {
        derivedStateOf {
            val info = serverInfo()
            if (info != null) {
                Pair(
                    info.diskUsed,
                    info.diskSize
                )
            } else {
                Pair(
                    resources.getString(R.string.immich_state_unknown),
                    resources.getString(R.string.immich_state_unknown)
                )
            }
        }
    }

    PreferenceRowWithCustomBody(
        icon = R.drawable.storage,
        title = stringResource(id = R.string.immich_server_storage),
        enabled = enabled()
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        val animated by animateFloatAsState(
            targetValue = serverInfo()?.diskUsedPercentage ?: 1f
        )

        LinearProgressIndicator(
            progress = {
                animated
            },
            color =
                if (serverInfo() != null) {
                    ProgressIndicatorDefaults.linearColor
                } else {
                    MaterialTheme.colorScheme.error
                }.copy(alpha = if (enabled()) 1f else 0.6f),
            modifier = Modifier
                .height(14.dp)
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text =
                if (serverInfo() != null) {
                    stringResource(id = R.string.immich_server_storage_desc, storage.first, storage.second)
                } else {
                    stringResource(id = R.string.immich_server_info_failed)
                },
            fontSize = 14.sp,
            textAlign = TextAlign.Start,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (serverInfo() != null) 1f else 0.6f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth(1f)
        )
    }
}