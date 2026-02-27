package com.kaii.photos.compose.settings

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.AnnotatedExplanationDialog
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.helpers.Updater
import com.kaii.photos.helpers.rememberUpdater
import kotlinx.coroutines.launch

private const val TAG = "com.kaii.photos.compose.settings.UpdatePage"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesPage() {
    val updater = rememberUpdater()
    val resources = LocalResources.current

    var showLoadingSpinner by remember { mutableStateOf(true) }
    val updateStatusText = remember { mutableStateOf(resources.getString(R.string.updates_not_found)) }

    var changelog by remember { mutableStateOf(updater.getChangelog()) }

    fun refresh() {
        updater.refresh { state ->
            when (state) {
                Updater.State.Succeeded -> {
                    showLoadingSpinner = false

                    updateStatusText.value =
                        if (updater.hasUpdates.value) resources.getString(R.string.updates_new_version_available)
                        else resources.getString(R.string.updates_latest)

                    changelog = updater.getChangelog()
                }

                Updater.State.Failed -> {
                    showLoadingSpinner = false
                    updateStatusText.value = resources.getString(R.string.updates_failed)
                }

                Updater.State.Checking -> {
                    showLoadingSpinner = true
                    updateStatusText.value = resources.getString(R.string.updates_checking)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopBar()
        },
        bottomBar = {
            BottomBar(
                updater = updater,
                updateStatusText = updateStatusText
            )
        },
        contentWindowInsets = WindowInsets.systemBars
    ) { innerPadding ->
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            coroutineScope.launch { refresh() }
        }

        PullToRefreshBox(
            isRefreshing = showLoadingSpinner,
            onRefresh = {
                coroutineScope.launch {
                    refresh()
                }
            },
            modifier = Modifier
                .fillMaxSize(1f)
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(1f)
                    .padding(8.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.updates_whats_new),
                    fontSize = TextUnit(TextStylingConstants.LARGE_TEXT_SIZE, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                        .padding(16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    item {
                        Text(
                            text = AnnotatedString.fromHtml(changelog),
                            fontSize = TextUnit(14f, TextUnitType.Sp),
                            modifier = Modifier
                                .padding(2.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar() {
    TopAppBar(
        title = {
            Text(
                text = stringResource(id = R.string.updates),
                fontSize = TextUnit(TextStylingConstants.EXTRA_EXTRA_LARGE_TEXT_SIZE, TextUnitType.Sp)
            )
        },
        navigationIcon = {
            val navController = LocalNavController.current

            IconButton(
                onClick = {
                    navController.popBackStack()
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.back_arrow),
                    contentDescription = stringResource(id = R.string.return_to_previous_page),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(24.dp)
                )
            }
        },
        actions = {
            val mainViewModel = LocalMainViewModel.current
            val resources = LocalResources.current
            val showUpdateNotice by mainViewModel.settings.versions.getShowUpdateNotice().collectAsStateWithLifecycle(false)
            var showDialog by remember { mutableStateOf(false) }

            LaunchedEffect(showUpdateNotice) {
                if (showUpdateNotice) {
                    showDialog = true

                    mainViewModel.settings.versions.setShowUpdateNotice(false)
                }
            }

            val htmlString = remember {
                resources.getString(R.string.updates_notice_desc).trimIndent()
            }

            if (showDialog) {
                AnnotatedExplanationDialog(
                    title = stringResource(id = R.string.updates_notice),
                    annotatedExplanation = AnnotatedString.fromHtml(
                        htmlString = htmlString,
                        linkStyles = TextLinkStyles(
                            style = SpanStyle(
                                textDecoration = TextDecoration.Underline,
                                fontStyle = FontStyle.Normal,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            pressedStyle = SpanStyle(
                                textDecoration = TextDecoration.Underline,
                                fontStyle = FontStyle.Normal,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    )
                ) {
                    showDialog = false
                }
            }

            IconButton(
                onClick = {
                    showDialog = true
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.info),
                    contentDescription = stringResource(id = R.string.updates_check)
                )
            }
        }
    )
}

@Composable
private fun BottomBar(
    updater: Updater,
    updateStatusText: MutableState<String>
) {
    val resources = LocalResources.current

    var isDownloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxWidth(1f)
            .wrapContentHeight()
            .systemBarsPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val newVersionExists by updater.hasUpdates

        AnimatedContent(
            targetState = isDownloading,
            transitionSpec = {
                (slideInHorizontally { width -> width } + fadeIn())
                    .togetherWith(
                        slideOutHorizontally { width -> -width } + fadeOut()
                    )
            },
            label = "animate update download loading bar",
            modifier = Modifier
                .padding(8.dp, 8.dp, 8.dp, 16.dp)
        ) { downloading ->
            if (downloading) {
                LinearProgressIndicator(
                    progress = {
                        progress
                    },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceBright,
                    strokeCap = StrokeCap.Round,
                    // drawStopIndicator = {},
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(0.6f)
                        .height(16.dp)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .wrapContentHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = updateStatusText.value,
                        fontSize = TextUnit(TextStylingConstants.SMALL_TEXT_SIZE, TextUnitType.Sp),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(12.dp, 8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (newVersionExists) {
                        Button(
                            onClick = {
                                if (!isDownloading) {
                                    updater.startUpdate(
                                        progress = { percent ->
                                            isDownloading = true
                                            progress = percent / 100f
                                        },

                                        onDownloadStopped = { success ->
                                            isDownloading = false

                                            if (success) {
                                                Log.d(TAG, "Download succeeded, installing...")
                                                updater.installUpdate()
                                            } else {
                                                Log.d(TAG, "Download failed")
                                                updateStatusText.value = resources.getString(R.string.updates_failed_download)
                                            }
                                        }
                                    )
                                }
                            }
                        ) {
                            Text(
                                text = stringResource(id = R.string.updates_start),
                                fontSize = TextUnit(TextStylingConstants.SMALL_TEXT_SIZE, TextUnitType.Sp),
                            )
                        }
                    }
                }
            }
        }
    }
}
