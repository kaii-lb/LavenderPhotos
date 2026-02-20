package com.kaii.photos.compose.pages

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.AnnotatedExplanationDialog
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.permissions.StartupManager
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProcessingPage(startupManager: StartupManager) {
    var itemCount by remember { mutableIntStateOf(0) }
    var currentProgress by remember { mutableFloatStateOf(0f) }

    val animatedCount by animateIntAsState(
        targetValue = (itemCount * currentProgress).roundToInt(),
        animationSpec = AnimationConstants.expressiveTween(
            durationMillis = AnimationConstants.DURATION_EXTRA_EXTRA_LONG
        )
    )

    val animatedFill by animateFloatAsState(
        targetValue = currentProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    LaunchedEffect(Unit) {
        startupManager.launchFirstTimeSyncWorker { count, progress ->
            itemCount = count
            currentProgress = progress
        }
    }

    LaunchedEffect(currentProgress) {
        if (currentProgress >= 1f) {
            delay(2.seconds)
            startupManager.checkState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val split = stringResource(id = R.string.app_name_full).split(" ")

                    val firstName = remember(split) { split.first() }

                    val secondName = remember(split) {
                        if (split.size >= 2) split[1]
                        else ""
                    }

                    Row {
                        Text(
                            text = "$firstName ",
                            fontWeight = FontWeight.Bold,
                            fontSize = TextStylingConstants.EXTRA_EXTRA_LARGE_TEXT_SIZE.sp
                        )
                        Text(
                            text = secondName,
                            fontWeight = FontWeight.Normal,
                            fontSize = TextStylingConstants.EXTRA_EXTRA_LARGE_TEXT_SIZE.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            var showInfoDialog by remember { mutableStateOf(false) }
            if (showInfoDialog) {
                AnnotatedExplanationDialog(
                    title = stringResource(id = R.string.startup_processing_explanation),
                    annotatedExplanation = AnnotatedString.fromHtml(
                        htmlString = stringResource(id = R.string.startup_processing_explanation_desc)
                    )
                ) {
                    showInfoDialog = false
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .fillMaxWidth()
                    .height(48.dp)
                    .offset(y = (-24).dp)
            ) {
                val waitLabel = stringResource(id = R.string.startup_processing_explanation)
                val skipLabel = stringResource(id = R.string.startup_processing_skip)

                ButtonGroup(
                    overflowIndicator = {},
                    horizontalArrangement = Arrangement.spacedBy(
                        space = 8.dp,
                        alignment = Alignment.CenterHorizontally
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 56.dp)
                ) {
                    toggleableItem(
                        checked = showInfoDialog,
                        onCheckedChange = {
                            showInfoDialog = true
                        },
                        weight = 1f,
                        label = waitLabel
                    )

                    clickableItem(
                        onClick = {
                            startupManager.skipIndexing()
                        },
                        weight = 0.65f,
                        label = skipLabel
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.4f),
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { paddingValues ->
        Column(
            verticalArrangement = Arrangement.spacedBy(
                space = 24.dp,
                alignment = Alignment.CenterVertically
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            val color = MaterialTheme.colorScheme.primary
            val iconSize = 160.dp

            Text(
                text = stringResource(id = R.string.startup_processing_indexing),
                fontWeight = FontWeight.Bold,
                fontSize = TextStylingConstants.EXTRA_LARGE_TEXT_SIZE.sp
            )

            val painter = painterResource(id = R.drawable.lavender_no_padding)
            Icon(
                painter = painter,
                contentDescription = "",
                tint = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .size(iconSize)
                    .graphicsLayer(
                        compositingStrategy = CompositingStrategy.Offscreen
                    )
                    .drawWithContent {
                        drawContent()

                        drawRect(
                            color = color,
                            blendMode = BlendMode.SrcIn,
                            topLeft = Offset(
                                x = 0f,
                                y = size.height - size.height * animatedFill
                            ),
                            size = size.copy(height = size.height * animatedFill)
                        )
                    }
            )

            AnimatedContent(
                targetState = animatedFill >= 1f,
                transitionSpec = {
                    val enter = scaleIn(animationSpec = AnimationConstants.expressiveSpring()) + fadeIn()
                    val exit = scaleOut(animationSpec = AnimationConstants.expressiveSpring()) + fadeOut()

                    enter.togetherWith(exit)
                }
            ) { state ->
                if (state) {
                    LinearWavyProgressIndicator(
                        progress = { 1f },
                        wavelength = 64.dp,
                        gapSize = 8.dp,
                        waveSpeed = 1.dp
                    )
                } else {
                    LinearWavyProgressIndicator(
                        amplitude = 1.5f,
                        wavelength = 64.dp,
                        gapSize = 8.dp,
                        waveSpeed = 1.dp
                    )
                }
            }

            Text(
                text = stringResource(id = R.string.startup_processing_info),
                fontWeight = FontWeight.Bold,
                fontSize = TextStylingConstants.MEDIUM_TEXT_SIZE.sp
            )

            AnimatedContent(
                targetState = animatedFill >= 1f,
                transitionSpec = {
                    val enter = scaleIn(animationSpec = AnimationConstants.expressiveSpring()) + fadeIn()
                    val exit = scaleOut(animationSpec = AnimationConstants.expressiveSpring()) + fadeOut()

                    enter.togetherWith(exit)
                }
            ) { state ->
                if (state) {
                    Icon(
                        painter = painterResource(id = R.drawable.checkmark_thin),
                        contentDescription = stringResource(id = R.string.media_okay),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(64.dp)
                    )
                } else {
                    Text(
                        text = stringResource(id = R.string.startup_processing_progress, "$animatedCount/$itemCount"),
                        fontSize = TextStylingConstants.MEDIUM_TEXT_SIZE.sp
                    )
                }
            }
        }
    }
}