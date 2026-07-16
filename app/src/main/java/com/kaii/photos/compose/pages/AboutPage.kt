package com.kaii.photos.compose.pages

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.TextSeparator
import com.kaii.photos.compose.widgets.about.AppAboutItem
import com.kaii.photos.compose.widgets.about.ContributorItem
import com.kaii.photos.compose.widgets.news.NewsPopup
import com.kaii.photos.domain.about.ContributorItem
import com.kaii.photos.domain.about.majorContributors
import com.kaii.photos.helpers.ComponentViewModelScope
import com.kaii.photos.models.news.NewsViewModelFactory
import com.kaii.photos.presentation.ui.theme.ThemeConfiguration
import com.kaii.photos.ui.theme.PhotosTheme

@Preview
@Composable
private fun AboutPagePreview() {
    PhotosTheme(theme = ThemeConfiguration.Default) {
        AboutPage(
            contributors = majorContributors,
            appVersion = "v2.0.0",
            navController = rememberNavController()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutPage(
    contributors: List<ContributorItem>,
    appVersion: String,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.settings_about),
                        style = MaterialTheme.typography.titleLarge
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
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                alignment = Alignment.Top
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                var showNews by remember { mutableStateOf(false) }
                if (showNews) {
                    ComponentViewModelScope(key = "News Section") {
                        NewsPopup(
                            viewModel = viewModel(
                                factory = NewsViewModelFactory(context = context),
                                viewModelStoreOwner = LocalViewModelStoreOwner.current!!
                            ),
                            onDismiss = { showNews = false }
                        )
                    }
                }

                AppAboutItem(
                    version = appVersion,
                    showNews = {
                        showNews = true
                    }
                )
            }

            items(
                count = contributors.size,
                key = {
                    val item = contributors[it]

                    if (item is ContributorItem.Separator) item.title
                    else (item as ContributorItem.Contributor).name
                }
            ) { index ->
                when (val item = contributors[index]) {
                    is ContributorItem.Separator -> {
                        TextSeparator(
                            text = stringResource(id = item.title),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = modifier
                        )
                    }

                    is ContributorItem.Contributor -> {
                        ContributorItem(
                            name = item.name,
                            avatarUrl = item.avatarUrl,
                            title = item.title,
                            description = stringResource(id = item.description),
                            socials = item.socials,
                            openLink = { link ->
                                val intent = Intent().apply {
                                    action = Intent.ACTION_VIEW
                                    data = link.toUri()
                                }

                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
}