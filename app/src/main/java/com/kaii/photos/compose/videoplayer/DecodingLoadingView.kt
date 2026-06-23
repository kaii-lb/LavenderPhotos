package com.kaii.photos.compose.videoplayer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaii.photos.R

@Composable
fun DecodingLoadingView(
    progress: () -> Float,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = progress(),
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
    )

    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(
            space = 16.dp,
            alignment = Alignment.CenterVertically
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = R.drawable.unlock),
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
        )

        Text(
            text = stringResource(id = R.string.video_decrypting),
            fontSize = 16.sp
        )

        LinearWavyProgressIndicator(
            progress = {
                progress
            },
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainer,
            gapSize = 4.dp,
            stroke = Stroke(
                width = 20f,
                cap = StrokeCap.Round
            ),
            trackStroke = Stroke(
                width = 20f,
                cap = StrokeCap.Round
            ),
            modifier = Modifier
                .height(14.dp)
                .fillMaxWidth(0.6f),
        )
    }
}