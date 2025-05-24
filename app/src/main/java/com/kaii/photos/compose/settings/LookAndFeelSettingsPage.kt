package com.kaii.photos.compose.settings

import androidx.compose.foundation.background
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.LocalNavController
import com.kaii.photos.MainActivity.Companion.mainViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.PreferencesSeparatorText
import com.kaii.photos.compose.PreferencesThreeStateSwitchRow
import com.kaii.photos.datastore.LookAndFeel
import com.kaii.photos.helpers.RowPosition

@Composable
fun LookAndFeelSettingsPage() {
	Scaffold (
		topBar = {
			DebuggingSettingsTopBar()
		}
	) { innerPadding ->
        LazyColumn (
            modifier = Modifier
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
        	item {
        		PreferencesSeparatorText(stringResource(id = R.string.settings_theme))
        	}

        	item {
                val followDarkMode by mainViewModel.settings.LookAndFeel.getFollowDarkMode().collectAsStateWithLifecycle(initialValue = 0)

                PreferencesThreeStateSwitchRow(
                    title = stringResource(id = R.string.settings_dark_theme),
                    summary = stringResource(id = DarkThemeSetting.entries[followDarkMode].descriptionId),
                    iconResID = R.drawable.palette,
                    currentPosition = followDarkMode,
                    trackIcons = listOf(
                        R.drawable.theme_auto,
                        R.drawable.theme_dark,
                        R.drawable.theme_light
                    ),
                    position = RowPosition.Single,
                    showBackground = false
                ) {
					mainViewModel.settings.LookAndFeel.setFollowDarkMode(it)
                }
        	}
        }
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebuggingSettingsTopBar() {
	val navController = LocalNavController.current

	TopAppBar(
        title = {
            Text(
                text = stringResource(id = R.string.settings_look_and_feel),
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

enum class DarkThemeSetting(val descriptionId: Int) {
    FollowSystem(R.string.settings_theme_follow),
    ForceDark(R.string.settings_theme_dark),
    ForceLight(R.string.settings_theme_light)
}
