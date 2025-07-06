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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
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
import com.kaii.photos.datastore.Versions
import com.kaii.photos.helpers.CheckUpdateState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "UPDATE_PAGE"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesPage() {
	val showLoadingSpinner = remember { mutableStateOf(true) }
	val runRefreshAction = remember { mutableStateOf(true) }

	Scaffold(
		topBar = {
			TopBar()
		},
		bottomBar = {
			BottomBar(
				showLoadingSpinner = showLoadingSpinner,
				runRefreshAction = runRefreshAction
			)
		},
		contentWindowInsets = WindowInsets.systemBars
	) { innerPadding ->
		val coroutineScope = rememberCoroutineScope()

		PullToRefreshBox (
			isRefreshing = showLoadingSpinner.value,
			onRefresh = {
				coroutineScope.launch {
					runRefreshAction.value = false
					delay(100)
					runRefreshAction.value = true
				}
			},
			modifier = Modifier
				.fillMaxSize(1f)
				.padding(innerPadding)
				.background(MaterialTheme.colorScheme.background)
		) {
			Column (
				modifier = Modifier
					.fillMaxSize(1f)
					.padding(8.dp),
				verticalArrangement = Arrangement.Top,
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				Text (
					text = stringResource(id = R.string.updates_whats_new),
					fontSize = TextUnit(18f, TextUnitType.Sp),
					fontWeight = FontWeight.Bold,
					modifier = Modifier
						.clip(CircleShape)
						.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
						.padding(16.dp)
				)

				Spacer (modifier = Modifier.height(16.dp))

				val mainViewModel = LocalMainViewModel.current
				val changelog = mainViewModel.updater.getChangelog().split("\n")

				LazyColumn (
					modifier = Modifier
						.fillMaxWidth(1f)
						.wrapContentHeight()
						.clip(RoundedCornerShape(32.dp))
						.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
						.padding(12.dp)
				) {
					if (changelog.size > 1) {
						item {
							Text (
								text = "${mainViewModel.updater.githubVersionName.value}:",
								fontSize = TextUnit(16f, TextUnitType.Sp),
								modifier = Modifier
									.padding(2.dp, 2.dp, 2.dp, 8.dp)
							)
						}
					}

					items(
						count = changelog.size
					) { index ->
						val item = changelog[index]

						Text(
							text = item,
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
				fontSize = TextUnit(22f, TextUnitType.Sp)
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
			val context = LocalContext.current
			val showUpdateNotice by mainViewModel.settings.Versions.getShowUpdateNotice().collectAsStateWithLifecycle(false)
			val showDialog = remember { mutableStateOf(false) }

			LaunchedEffect(showUpdateNotice) {
				if (showUpdateNotice) {
					showDialog.value = true

					mainViewModel.settings.Versions.setShowUpdateNotice(false)
				}
			}

			val htmlString = remember {
				context.resources.getString(R.string.updates_notice_desc).trimIndent()
			}

			if (showDialog.value) {
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
					),
					showDialog = showDialog
				)
			}

			IconButton(
				onClick = {
					showDialog.value = true
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
	showLoadingSpinner: MutableState<Boolean>,
	runRefreshAction: MutableState<Boolean>
) {
	val context = LocalContext.current
	val mainViewModel = LocalMainViewModel.current

	Column (
		modifier = Modifier
			.fillMaxWidth(1f)
			.wrapContentHeight()
			.systemBarsPadding(),
		verticalArrangement = Arrangement.Center,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		val newVersionExists by remember {
			mainViewModel.updater.hasUpdates
		}
		var isDownloading by rememberSaveable { mutableStateOf(false) }
		var progress by rememberSaveable { mutableFloatStateOf(0f) }
		var updateStatusText by remember { mutableStateOf(context.resources.getString(R.string.updates_not_found))}

		// refresh on start
		LaunchedEffect(runRefreshAction.value) {
			if (!runRefreshAction.value) return@LaunchedEffect

			mainViewModel.updater.refresh { state ->
				when (state) {
					CheckUpdateState.Succeeded -> {
						showLoadingSpinner.value = false

						updateStatusText =
							if (newVersionExists) context.resources.getString(R.string.updates_new_version_available)
							else context.resources.getString(R.string.updates_latest)
					}

					CheckUpdateState.Failed -> {
						showLoadingSpinner.value = false
						updateStatusText = context.resources.getString(R.string.updates_failed)
					}

					CheckUpdateState.Checking -> {
						showLoadingSpinner.value = true
						updateStatusText = context.resources.getString(R.string.updates_checking)
					}
				}
			}
		}

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
				Column (
					modifier = Modifier
						.fillMaxWidth(1f)
						.wrapContentHeight(),
					verticalArrangement = Arrangement.Center,
					horizontalAlignment = Alignment.CenterHorizontally
				) {
					Text(
						text = updateStatusText,
						fontSize = TextUnit(14f, TextUnitType.Sp),
						color = MaterialTheme.colorScheme.onBackground,
						modifier = Modifier
							.clip(CircleShape)
							.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
							.padding(12.dp, 8.dp)
					)

					Spacer (modifier = Modifier.height(8.dp))

					if (newVersionExists) {
						Button (
							onClick = {
								if (!isDownloading) {
									mainViewModel.updater.startUpdate(
										progress = { percent ->
											isDownloading = true
											progress = percent / 100f
										},

										onDownloadStopped = { success ->
											isDownloading = false

											if (success) {
												Log.d(TAG, "Download succeeded, installing...")
												mainViewModel.updater.installUpdate()
											} else {
												Log.d(TAG, "Download failed")
												updateStatusText = "Couldn't download update"
											}
										}
									)
								}
							}
						) {
							Text(
								text = stringResource(id = R.string.updates_start),
								fontSize = TextUnit(14f, TextUnitType.Sp),
							)
						}
					}
				}
			}
		}
	}
}
