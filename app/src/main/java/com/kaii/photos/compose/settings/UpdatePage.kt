package com.kaii.photos.compose.settings

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.user_action.AnnotatedExplanationDialog
import com.kaii.photos.compose.widgets.news.NewsList
import com.kaii.photos.di.appModule
import com.kaii.photos.domain.news.News
import com.kaii.photos.domain.news.UpdateState
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.ui.theme.LocalExtraColorsPalette
import com.kaii.photos.ui.theme.PhotosTheme

@Preview
@Composable
private fun UpdatePagePreview() {
    PhotosTheme(theme = 2) {
        UpdatesPage(
            updateState = { UpdateState.Available },
            news = {
                listOf(
                    News.Section(
                        version = "v2.0.0",
                        date = "20-7-2026",
                        status = News.Section.Status.Latest,
                        id = 0
                    ),
                    News.Note(
                        info = "This is a note.",
                        urgency = News.Note.Urgency.Critical,
                        id = 1
                    ),
                    News.Category(
                        category = News.Category.Type.Features,
                        id = 2
                    ),
                    News.Item(
                        title = "This is a very long description for a very boring UI component",
                        issueNumber = 123,
                        id = 3
                    ),
                    News.Item(
                        title = "This is a very long description for a very boring UI component",
                        issueNumber = 123,
                        id = 4
                    )
                )
            },
            navController = rememberNavController(),
            showUpdateNotice = { false },
            onRefresh = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesPage(
    updateState: () -> UpdateState,
    news: () -> List<News>,
    showUpdateNotice: () -> Boolean,
    modifier: Modifier = Modifier,
    navController: NavController = LocalNavController.current,
    onRefresh: () -> Unit
) {
    Scaffold(
        topBar = {
            TopBar(navController, showUpdateNotice)
        },
        bottomBar = {
            BottomBar(updateState = updateState)
        },
        modifier = modifier
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = updateState() == UpdateState.Loading,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize(1f)
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.updates_whats_new),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                        .padding(16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedContent(
                    targetState = news().isNotEmpty(),
                    transitionSpec = {
                        val enter = fadeIn() + scaleIn(initialScale = 0.8f)
                        val exit = fadeOut() + scaleOut()

                        enter.togetherWith(exit)
                    },
                    contentAlignment = Alignment.Center
                ) { state ->
                    if (state) {
                        NewsList(
                            list = news(),
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(size = 32.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(size = 32.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.updates_no_changelog),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    navController: NavController,
    showUpdateNotice: () -> Boolean
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(id = R.string.updates),
                fontSize = TextUnit(TextStylingConstants.EXTRA_EXTRA_LARGE_TEXT_SIZE, TextUnitType.Sp)
            )
        },
        navigationIcon = {
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
            val context = LocalContext.current
            val resources = LocalResources.current
            var showDialog by remember { mutableStateOf(false) }

            LaunchedEffect(showUpdateNotice()) {
                if (showUpdateNotice()) {
                    showDialog = true

                    context.appModule.settings.versions.setShowUpdateNotice(false)
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
    updateState: () -> UpdateState
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .padding(all = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        val containerColor by animateColorAsState(
            targetValue =
                if (updateState() == UpdateState.Available) LocalExtraColorsPalette.current.success
                else MaterialTheme.colorScheme.errorContainer,
            animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
        )

        val contentColor by animateColorAsState(
            targetValue =
                if (updateState() == UpdateState.Available) LocalExtraColorsPalette.current.onSuccess
                else MaterialTheme.colorScheme.onErrorContainer,
            animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
        )

        val disabledContainerColor by animateColorAsState(
            targetValue =
                if (updateState() == UpdateState.NotAvailable) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f),
            animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
        )

        val disabledContentColor by animateColorAsState(
            targetValue =
                if (updateState() == UpdateState.Available) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
        )

        Button(
            onClick = {
                val intent = Intent().apply {
                    action = Intent.ACTION_VIEW
                    data = "https://github.com/kaii-lb/LavenderPhotos/releases/latest".toUri()
                }

                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = disabledContainerColor,
                disabledContentColor = disabledContentColor
            ),
            enabled = updateState() == UpdateState.Available
        ) {
            Text(
                text = when (updateState()) {
                    UpdateState.Available -> stringResource(id = R.string.updates_new_version_available)
                    UpdateState.NotAvailable -> stringResource(id = R.string.updates_latest)
                    UpdateState.Loading -> stringResource(id = R.string.updates_checking)
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .animateContentSize(
                        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
                    )
            )
        }
    }
}
