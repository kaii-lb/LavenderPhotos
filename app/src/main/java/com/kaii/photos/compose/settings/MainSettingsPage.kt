package com.kaii.photos.compose.settings

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.PreferencesRow
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.TextStylingConstants

@Composable
fun MainSettingsPage() {
    Scaffold(
        topBar = {
            MainSettingsTopBar()
        },
        contentWindowInsets = WindowInsets.systemBars
    ) { innerPadding ->
        val navController = LocalNavController.current

        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                PreferencesRow(
                    title = stringResource(id = R.string.settings_general),
                    summary = stringResource(id = R.string.settings_general_desc),
                    iconResID = R.drawable.settings,
                    position = RowPosition.Top,
                    showBackground = false,
                    titleTextSize = TextStylingConstants.EXTRA_LARGE_TEXT_SIZE,
                    modifier = Modifier
                    	.padding(0.dp, 6.dp)
                ) {
                    navController.navigate(MultiScreenViewType.SettingsGeneralView.name)
                }
            }

            item {
                PreferencesRow(
                    title = stringResource(id = R.string.settings_privacy),
                    summary = stringResource(id = R.string.settings_privacy_desc),
                    iconResID = R.drawable.privacy_policy,
                    position = RowPosition.Middle,
                    showBackground = false,
                    titleTextSize = TextStylingConstants.EXTRA_LARGE_TEXT_SIZE,
                    modifier = Modifier
                    	.padding(0.dp, 6.dp)
                ) {
                    navController.navigate(MultiScreenViewType.PrivacyAndSecurity.name)
                }
            }

            item {
                PreferencesRow(
                    title = stringResource(id = R.string.settings_look_and_feel),
                    summary = stringResource(id = R.string.settings_look_and_feel_desc),
                    iconResID = R.drawable.palette,
                    position = RowPosition.Middle,
                    showBackground = false,
                    titleTextSize = TextStylingConstants.EXTRA_LARGE_TEXT_SIZE,
                    modifier = Modifier
                    	.padding(0.dp, 6.dp)
                ) {
                    navController.navigate(MultiScreenViewType.SettingsLookAndFeelView.name)
                }
            }

            item {
                PreferencesRow(
                    title = stringResource(id = R.string.settings_memory_storage),
                    summary = stringResource(id = R.string.settings_storage_desc),
                    iconResID = R.drawable.privacy_policy,
                    position = RowPosition.Middle,
                    showBackground = false,
                    titleTextSize = TextStylingConstants.EXTRA_LARGE_TEXT_SIZE,
                    modifier = Modifier
                    	.padding(0.dp, 6.dp)
                ) {
                    navController.navigate(MultiScreenViewType.SettingsMemoryAndStorageView.name)
                }
            }

            item {
                PreferencesRow(
                    title = stringResource(id = R.string.debugging),
                    summary = stringResource(id = R.string.debugging_desc),
                    iconResID = R.drawable.memory,
                    position = RowPosition.Bottom,
                    showBackground = false,
                    titleTextSize = TextStylingConstants.EXTRA_LARGE_TEXT_SIZE,
                    modifier = Modifier
                    	.padding(0.dp, 6.dp)
                ) {
                    navController.navigate(MultiScreenViewType.SettingsDebuggingView.name)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainSettingsTopBar() {
    val navController = LocalNavController.current

    val localConfig = LocalConfiguration.current
    var isLandscape by remember { mutableStateOf(localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) }

    LaunchedEffect(localConfig) {
        isLandscape = localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    TopAppBar(
        title = {
            Text(
                text = stringResource(id = R.string.settings),
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
        scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}
