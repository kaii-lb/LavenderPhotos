package com.kaii.photos.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import com.kaii.photos.R
import com.kaii.photos.LocalNavController
import com.kaii.photos.compose.PreferencesRow
import com.kaii.photos.helpers.CustomMaterialTheme
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.MultiScreenViewType

@Composable
fun MainSettingsPage() {
    Scaffold (
		topBar = {
			MainSettingsTopBar()
		}
    ) { innerPadding ->
    	val navController = LocalNavController.current ?: return@Scaffold

        LazyColumn (
            modifier = Modifier
                .padding(innerPadding)
                .background(CustomMaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                PreferencesRow(
                    title = "General",
                    summary = "App preferences and customizations",
                    iconResID = R.drawable.settings,
                    position = RowPosition.Top,
                    showBackground = false,
                    height = 80.dp,
                    titleTextSize = 20f
                ) {
					navController.navigate(MultiScreenViewType.SettingsGeneralView.name)
                }
            }

            item {
                PreferencesRow(
                    title = "Privacy & Security",
                    summary = "Fine grained control over your data",
                    iconResID = R.drawable.privacy_policy,
                    position = RowPosition.Middle,
                    showBackground = false,
                    height = 80.dp,
                    titleTextSize = 20f
                ) {

                }
            }

            item {
                PreferencesRow(
                    title = "Look & Feel",
                    summary = "Change how the app looks",
                    iconResID = R.drawable.palette,
                    position = RowPosition.Middle,
                    showBackground = false,
                    height = 80.dp,
                    titleTextSize = 20f
                ) {

                }
            }

			item {
                PreferencesRow(
                    title = "Memory & Storage",
                    summary = "Performance and space options",
                    iconResID = R.drawable.privacy_policy,
                    position = RowPosition.Middle,
                    showBackground = false,
                    height = 80.dp,
                    titleTextSize = 20f
                ) {

                }
            }

            item {
                PreferencesRow(
                    title = "Debugging",
                    summary = "Tools for debugging issues",
                    iconResID = R.drawable.memory,
                    position = RowPosition.Bottom,
                    showBackground = false,
                    height = 80.dp,
                    titleTextSize = 20f
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
	val navController = LocalNavController.current ?: return

	TopAppBar(
        title = {
            Text(
                text = "Settings",
                fontSize = TextUnit(22f, TextUnitType.Sp)
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
                    contentDescription = "Go back to previous page",
                    tint = CustomMaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(24.dp)
                )
            }
        },
        scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = CustomMaterialTheme.colorScheme.background
        ),
    )
}
