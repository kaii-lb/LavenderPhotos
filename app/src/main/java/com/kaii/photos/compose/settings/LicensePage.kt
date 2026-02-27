package com.kaii.photos.compose.settings

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.PreferencesSeparatorText
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.TextStylingConstants
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.util.author
import com.mikepenz.aboutlibraries.ui.compose.util.htmlReadyLicenseContent

private data class CreditsLicense(
    val title: String,
    val license: String,
    val link: String
)

private const val APACHE_V2 = "Apache License, Version 2.0"
private const val MIT = "MIT License"
private const val AGPL_V3 = "GNU Affero General Public License v3.0"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensePage() {
    val licenses = remember {
        listOf(
            CreditsLicense(
                title = "Android Jetpack",
                license = APACHE_V2,
                link = "https://developer.android.com/jetpack"
            ),
            CreditsLicense(
                title = "Kotlin",
                license = APACHE_V2,
                link = "https://kotlinlang.org/"
            ),
            CreditsLicense(
                title = "Material Design 3",
                license = APACHE_V2,
                link = "https://m3.material.io/"
            ),
            CreditsLicense(
                title = "Fuel",
                license = MIT,
                link = "https://github.com/kittinunf/fuel"
            ),
            CreditsLicense(
                title = "Glide",
                license = APACHE_V2,
                link = "https://github.com/bumptech/glide"
            ),
            CreditsLicense(
                title = "Immich",
                license = AGPL_V3,
                link = "https://immich.app/"
            ),
            CreditsLicense(
                title = "Ktor",
                license = APACHE_V2,
                link = "https://ktor.io/"
            ),
            CreditsLicense(
                title = "Telephoto",
                license = APACHE_V2,
                link = "https://saket.github.io/telephoto/"
            )
        )
    }

    Scaffold(
        topBar = {
            val navController = LocalNavController.current

            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.licenses),
                        fontSize = TextUnit(TextStylingConstants.EXTRA_EXTRA_LARGE_TEXT_SIZE, TextUnitType.Sp)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.popBackStack()
                        },
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            item {
                PreferencesSeparatorText(
                    text = stringResource(id = R.string.special_thanks_to)
                )
            }

            items(
                count = licenses.size
            ) { index ->
                val item = licenses[index]

                LicenseCard(
                    title = item.title,
                    license = item.license,
                    link = item.link
                )
            }

            item {
                val navController = LocalNavController.current

                Row(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .wrapContentHeight()
                        .clickable {
                            navController.navigate(Screens.Settings.Misc.ExtendedLicensePage)
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Column(
                        modifier = Modifier
                            .wrapContentHeight()
                            .weight(1f),
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = stringResource(id = R.string.licenses_extra),
                            fontSize = TextUnit(TextStylingConstants.MEDIUM_TEXT_SIZE, TextUnitType.Sp),
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Start,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Text(
                            text = stringResource(id = R.string.licenses_extra_desc),
                            fontSize = TextUnit(TextStylingConstants.SMALL_TEXT_SIZE, TextUnitType.Sp),
                            textAlign = TextAlign.Start,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Icon(
                        painter = painterResource(id = R.drawable.other_page_indicator),
                        contentDescription = "this item leads to another page",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LicenseCard(
    title: String,
    license: String,
    link: String
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth(1f)
            .height(64.dp)
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = link.toUri()
                }

                context.startActivity(intent)
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            fontSize = TextUnit(TextStylingConstants.MEDIUM_TEXT_SIZE, TextUnitType.Sp),
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = license,
            fontSize = TextUnit(TextStylingConstants.SMALL_TEXT_SIZE, TextUnitType.Sp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtendedLicensePage() {
    Scaffold(
        topBar = {
            val navController = LocalNavController.current

            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.licenses_extra),
                        fontSize = TextUnit(TextStylingConstants.EXTRA_EXTRA_LARGE_TEXT_SIZE, TextUnitType.Sp)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.popBackStack()
                        },
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        val libraries by produceLibraries(R.raw.aboutlibraries)

        LibrariesContainer(
            libraries = libraries,
            name = { name ->
                Text(
                    text = name,
                    fontSize = TextUnit(TextStylingConstants.MEDIUM_TEXT_SIZE, TextUnitType.Sp),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(end = 16.dp)
                )
            },
            license = { license ->
                Text(
                    text = license.name,
                    fontSize = TextUnit(TextStylingConstants.SMALL_TEXT_SIZE, TextUnitType.Sp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            },
            version = { version ->
                Text(
                    text = version,
                    fontSize = TextUnit(TextStylingConstants.EXTRA_SMALL_TEXT_SIZE, TextUnitType.Sp),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxHeight(1f)
                )
            },
            author = { author ->
                Text(
                    text = author,
                    fontSize = TextUnit(TextStylingConstants.SMALL_TEXT_SIZE, TextUnitType.Sp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            },
            licenseDialogBody = { library, modifier ->
                Column(
                    modifier = modifier
                        .fillMaxHeight(0.8f)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = library.name,
                        fontSize = TextUnit(TextStylingConstants.LARGE_TEXT_SIZE, TextUnitType.Sp),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = library.author,
                        fontSize = TextUnit(TextStylingConstants.MEDIUM_TEXT_SIZE, TextUnitType.Sp),
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = AnnotatedString.fromHtml(
                            htmlString = library.htmlReadyLicenseContent
                        ),
                        fontSize = TextUnit(TextStylingConstants.SMALL_TEXT_SIZE, TextUnitType.Sp)
                    )
                }
            },
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        )
    }
}