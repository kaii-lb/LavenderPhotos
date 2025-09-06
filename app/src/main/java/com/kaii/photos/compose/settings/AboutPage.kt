package com.kaii.photos.compose.settings

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.ExplanationDialog
import com.kaii.photos.compose.dialogs.FeatureNotAvailableDialog
import com.kaii.photos.compose.widgets.PreferencesRow
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.TextStylingConstants

private const val TAG = "com.kaii.photos.compose.settings.AboutPage"

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun AboutPage(popBackStack: () -> Unit) {
    val context = LocalContext.current
    val showVersionInfoDialog = remember { mutableStateOf(false) }

    VersionInfoDialog(
        showDialog = showVersionInfoDialog,
        changelog = stringResource(id = R.string.changelog)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(1f)
            .padding(8.dp)
            .windowInsetsPadding(WindowInsets.systemBars)
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .wrapContentHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .wrapContentHeight(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(
                        onClick = {
                            popBackStack()
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
                }

                GlideImage(
                    model = R.drawable.lavender,
                    contentDescription = stringResource(id = R.string.app_icon),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(108.dp)
                )

                Text(
                    text = stringResource(id = R.string.app_name_full),
                    textAlign = TextAlign.Center,
                    fontSize = TextUnit(TextStylingConstants.EXTRA_EXTRA_LARGE_TEXT_SIZE, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold,
                    style = LocalTextStyle.current.copy(
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxSize(1f)
                    .padding(8.dp)
                    .background(MaterialTheme.colorScheme.background),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PreferencesRow(
                    title = stringResource(id = R.string.dev_title),
                    summary = stringResource(id = R.string.dev_name),
                    iconResID = R.drawable.code,
                    position = RowPosition.Top
                ) {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = "https://github.com/kaii-lb".toUri()
                    }

                    context.startActivity(intent)
                }

                var showPrivacyPolicy by remember { mutableStateOf(false) }
                if (showPrivacyPolicy) {
                    ExplanationDialog(
                        title = stringResource(id = R.string.privacy_policy_title),
                        explanation = stringResource(id = R.string.privacy_policy)
                    ) {
                        showPrivacyPolicy = false
                    }
                }

                PreferencesRow(
                    title = stringResource(id = R.string.privacy_policy_title),
                    summary = stringResource(id = R.string.i_swear_were_not_bad),
                    iconResID = R.drawable.privacy_policy,
                    position = RowPosition.Middle
                ) {
                    showPrivacyPolicy
                }

                var showNotImplDialog by remember { mutableStateOf(false) }
                if (showNotImplDialog) {
                    FeatureNotAvailableDialog {
                        showNotImplDialog = false
                    }
                }

                val navController = LocalNavController.current
                PreferencesRow(
                    title = stringResource(id = R.string.updates),
                    summary = stringResource(id = R.string.updates_desc),
                    iconResID = R.drawable.update,
                    position = RowPosition.Middle,
                    goesToOtherPage = true
                ) {
                    navController.navigate(MultiScreenViewType.UpdatesPage.name)
                }

                PreferencesRow(
                    title = stringResource(id = R.string.support),
                    summary = stringResource(id = R.string.support_desc),
                    iconResID = R.drawable.donation,
                    position = RowPosition.Middle
                ) {
                    showNotImplDialog
                }

                PreferencesRow(
                    title = stringResource(id = R.string.translation),
                    summary = stringResource(id = R.string.translation_desc),
                    iconResID = R.drawable.globe,
                    position = RowPosition.Middle
                ) {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = "https://hosted.weblate.org/projects/lavender-photos/".toUri()
                    }

                    context.startActivity(intent)
                }

                val versionName = remember {
                    try {
                        context.packageManager.getPackageInfo(context.packageName, 0).versionName
                    } catch (e: Throwable) {
                        Log.e(TAG, e.toString())
                        "Couldn't get version number"
                    }
                }
                PreferencesRow(
                    title = stringResource(id = R.string.version_info),
                    summary = versionName,
                    iconResID = R.drawable.info,
                    position = RowPosition.Middle,
                ) {
                    showVersionInfoDialog.value = true
                }

                PreferencesRow(
                    title = stringResource(id = R.string.licenses),
                    summary = stringResource(id = R.string.licenses_desc),
                    iconResID = R.drawable.license,
                    position = RowPosition.Bottom,
                    goesToOtherPage = true
                ) {
                    navController.navigate(MultiScreenViewType.LicensePage.name)
                }
            }
        }
    }
}

@Composable
fun VersionInfoDialog(
    showDialog: MutableState<Boolean>,
    changelog: String
) {
    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = {
                showDialog.value = false
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog.value = false
                    }
                ) {
                    Text(
                        text = "Close",
                        fontSize = TextUnit(TextStylingConstants.SMALL_TEXT_SIZE, TextUnitType.Sp),
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Changelog",
                        fontSize = TextUnit(TextStylingConstants.LARGE_TEXT_SIZE, TextUnitType.Sp),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .height(320.dp),
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item {
                            Text(
                                text = changelog,
                                fontSize = TextUnit(TextStylingConstants.SMALL_TEXT_SIZE, TextUnitType.Sp),
                            )
                        }
                    }
                }
            }
        )
    }
}
