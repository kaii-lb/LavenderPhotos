package com.kaii.photos.compose.immich.dashboard

import android.content.Intent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.core.net.toUri
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.PreferencesRow
import com.kaii.photos.domain.immich.ImmichServerInfo
import com.kaii.photos.helpers.RowPosition

@Composable
fun ServerInfoRow(
    serverInfo: () -> ImmichServerInfo?,
    enabled: () -> Boolean
) {
    val resources = LocalResources.current

    val info by remember {
        derivedStateOf {
            val info = serverInfo()
            if (info != null) {
                Pair(
                    info.version,
                    info.build ?: resources.getString(R.string.immich_state_unknown)
                )
            } else {
                Pair(
                    resources.getString(R.string.immich_state_unknown),
                    resources.getString(R.string.immich_state_unknown)
                )
            }
        }
    }

    val context = LocalContext.current
    PreferencesRow(
        title = buildAnnotatedString {
            append(resources.getString(R.string.immich_server_info_with_status))
            append(" ")

            val online = serverInfo()?.online == true

            withStyle(
                style = SpanStyle(
                    color =
                        if (online) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                )
            ) {
                if (online) append(resources.getString(R.string.immich_server_online))
                else append(resources.getString(R.string.immich_server_offline))
            }
        },
        summary = buildAnnotatedString {
            val base = stringResource(id = R.string.immich_server_info_desc, info.first, info.second)
            append(base)

            val newVersion = serverInfo()?.newVersion

            if (newVersion != null) withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary
                )
            ) {
                val extra = stringResource(id = R.string.immich_server_info_new_version_available, newVersion)
                appendLine()
                append(extra)
            }
        },
        iconResID = R.drawable.handyman,
        position = RowPosition.Middle,
        showBackground = false,
        enabled = enabled(),
        action = {
            val intent = Intent().apply {
                action = Intent.ACTION_VIEW
                data = "https://github.com/immich-app/immich/releases/tag/${serverInfo()?.newVersion}".toUri()
            }
            context.startActivity(intent)
        }.takeIf {
            serverInfo()?.newVersion != null
        }
    )
}