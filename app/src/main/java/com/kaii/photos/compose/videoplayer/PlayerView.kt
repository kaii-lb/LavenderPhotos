package com.kaii.photos.compose.videoplayer

import android.util.Xml
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.kaii.photos.R

@OptIn(UnstableApi::class)
@Composable
fun rememberPlayerView(
    blurViews: Boolean,
    useBlackBackground: Boolean,
    useTextureView: Boolean = false
): PlayerView {
    val context = LocalContext.current
    val resources = LocalResources.current

    val backgroundColor = when {
        blurViews -> Color.Transparent.toArgb()

        useBlackBackground -> Color.Black.toArgb()

        else -> MaterialTheme.colorScheme.background.toArgb()
    }

    val playerView = remember {
        PlayerView(
            context,
            if (useTextureView) resources.getXml(R.xml.custom_player_view).let {
                it.next()
                it.nextTag()
                Xml.asAttributeSet(it)
            } else {
                null
            }
        ).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            useController = false

            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)

            setBackgroundColor(backgroundColor)
            setShutterBackgroundColor(backgroundColor)
            outlineSpotShadowColor = backgroundColor
            outlineAmbientShadowColor = backgroundColor
        }
    }

    DisposableEffect(playerView) {
        onDispose {
            playerView.player = null
        }
    }

    return playerView
}