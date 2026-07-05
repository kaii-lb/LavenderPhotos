package com.kaii.photos.compose.dialogs.immich

import android.content.ClipData
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.nativeClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.LavenderDialogBase
import com.kaii.photos.compose.pages.FullWidthDialogButton
import com.kaii.photos.compose.widgets.shimmerEffect
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.models.immich_share_album_page.CreateLinkState
import com.kaii.photos.presentation.ui.theme.ThemeConfiguration
import com.kaii.photos.ui.theme.PhotosTheme
import com.kaii.photos.widgets.QrCodeState
import com.kaii.photos.widgets.rememberQrCodeState

@Preview
@Composable
private fun Preview() {
    PhotosTheme(theme = ThemeConfiguration.Default) {
        ImmichCopyShareLinkDialog(
            state = { CreateLinkState.Failed },
            onDismiss = {}
        )
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ImmichCopyShareLinkDialog(
    state: () -> CreateLinkState,
    qrCodeState: QrCodeState = rememberQrCodeState(),
    onDismiss: () -> Unit
) {
    val resources = LocalResources.current
    val clipboard = LocalClipboard.current

    LavenderDialogBase(
        onDismiss = onDismiss,
        verticalArrangement = Arrangement.spacedBy(
            space = 2.dp,
            alignment = Alignment.Top
        )
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(id = R.string.immich_share_album),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .clickable(
                    enabled = state() is CreateLinkState.Success
                ) {
                    val state = state()

                    if (state is CreateLinkState.Success) {
                        val label = resources.getString(R.string.immich_share_album_clip_title)
                        clipboard.nativeClipboardManager.setPrimaryClip(
                            ClipData.newPlainText(label, state.url)
                        )
                    }
                }
                .padding(all = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                alignment = Alignment.Start
            )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.link),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text =
                    when (state()) {
                        is CreateLinkState.Success -> (state() as CreateLinkState.Success).url
                        is CreateLinkState.Creating -> stringResource(id = R.string.immich_share_album_loading)
                        else -> stringResource(id = R.string.immich_share_album_loading_failed)
                    },
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .scrollable(
                        state = rememberScrollState(),
                        orientation = Orientation.Horizontal
                    )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LaunchedEffect(state()) {
            val state = state()
            if (state is CreateLinkState.Success) {
                qrCodeState.setContent(content = state.url)
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(32.dp))
                .then(
                    if (state() is CreateLinkState.Creating) Modifier.shimmerEffect()
                    else Modifier
                )
                .border(
                    width = 4.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(32.dp)
                )
                .onGloballyPositioned {
                    qrCodeState.setSize(it.size.width)
                }
        ) {
            AnimatedContent(
                targetState = state() is CreateLinkState.Failed,
                contentAlignment = Alignment.Center
            ) { error ->
                if (error) {
                    Icon(
                        painter = painterResource(id = R.drawable.error),
                        contentDescription = stringResource(id = R.string.immich_share_album_loading_failed),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .size(96.dp)
                    )
                } else {
                    GlideImage(
                        model = qrCodeState.bitmap,
                        contentDescription = stringResource(id = R.string.immich_share_album_qr_code),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(32.dp))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        FullWidthDialogButton(
            text = stringResource(id = R.string.media_copy),
            color = MaterialTheme.colorScheme.tertiary,
            textColor = MaterialTheme.colorScheme.onTertiary,
            position = RowPosition.Top,
            enabled = state() is CreateLinkState.Success
        ) {
            val state = state()

            if (state is CreateLinkState.Success) {
                val label = resources.getString(R.string.immich_share_album_clip_title)
                clipboard.nativeClipboardManager.setPrimaryClip(
                    ClipData.newPlainText(label, state.url)
                )
            }
        }

        FullWidthDialogButton(
            text = stringResource(id = R.string.media_okay),
            color = MaterialTheme.colorScheme.primary,
            textColor = MaterialTheme.colorScheme.onPrimary,
            position = RowPosition.Bottom,
            onClick = onDismiss
        )
    }
}