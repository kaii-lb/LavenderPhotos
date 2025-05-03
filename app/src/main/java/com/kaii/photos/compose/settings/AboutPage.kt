package com.kaii.photos.compose.settings

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.kaii.photos.compose.PreferencesRow
import com.kaii.photos.compose.dialogs.ExplanationDialog
import com.kaii.photos.compose.dialogs.FeatureNotAvailableDialog
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.RowPosition

private const val TAG = "ABOUT_PAGE"

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun AboutPage(popBackStack: () -> Unit) {
    val context = LocalContext.current
    val showVersionInfoDialog = remember { mutableStateOf(false) }

    VersionInfoDialog(
        showDialog = showVersionInfoDialog,
        changelog = stringResource(id = R.string.changelog)
    )

    Column(
        modifier = Modifier
            .fillMaxSize(1f)
            .padding(8.dp)
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
                    .padding(0.dp, 24.dp, 0.dp, 0.dp)
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
                        contentDescription = "return to previous page",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .size(24.dp)
                    )
                }
            }

            GlideImage(
                model = R.drawable.lavender,
                contentDescription = "app icon",
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(128.dp)
            )

            Text(
                text = "Lavender Photos",
                textAlign = TextAlign.Center,
                fontSize = TextUnit(22f, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                style = LocalTextStyle.current.copy(
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxSize(1f)
                .padding(8.dp)
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PreferencesRow(
                title = "Developer",
                summary = "kaii-lb",
                iconResID = R.drawable.code,
                position = RowPosition.Top
            ) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setData("https://github.com/kaii-lb".toUri())
                }

                context.startActivity(intent)
            }

            val showPrivacyPolicy = remember { mutableStateOf(false) }
            if (showPrivacyPolicy.value) {
                ExplanationDialog(
                    title = "Privacy Policy",
                    explanation = "There isn't one! None of your data goes anywhere but this device. No AI is trained on it, no algorithms to harvest information, nothing. Lavender Photos has and always will be a privacy focused gallery app.",
                    showDialog = showPrivacyPolicy,
                )
            }

            PreferencesRow(
                title = "Privacy Policy",
                summary = "we really don't use your data",
                iconResID = R.drawable.privacy_policy,
                position = RowPosition.Middle
            ) {
                showPrivacyPolicy.value = true
            }

            val showNotImplDialog = remember { mutableStateOf(false) }
            if (showNotImplDialog.value) {
                FeatureNotAvailableDialog(showDialog = showNotImplDialog)
            }

            val navController = LocalNavController.current
            PreferencesRow(
                title = "Updates",
                summary = "keep the app up to date",
                iconResID = R.drawable.update,
                position = RowPosition.Middle,
                goesToOtherPage = true
            ) {
                navController.navigate(MultiScreenViewType.UpdatesPage.name)
            }

            PreferencesRow(
                title = "Support & Donations",
                summary = "help me keep the app alive",
                iconResID = R.drawable.donation,
                position = RowPosition.Middle
            ) {
                showNotImplDialog.value = true
            }

            val versionName = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (e: Throwable) {
                Log.e(TAG, e.toString())
                "Couldn't get version number"
            }
            PreferencesRow(
                title = "Version Info",
                summary = versionName,
                iconResID = R.drawable.info,
                position = RowPosition.Bottom,
            ) {
                showVersionInfoDialog.value = true
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
                        fontSize = TextUnit(14f, TextUnitType.Sp),
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
                        fontSize = TextUnit(18f, TextUnitType.Sp),
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
                                fontSize = TextUnit(14f, TextUnitType.Sp),
                            )
                        }
                    }
                }
            }
        )
    }
}
