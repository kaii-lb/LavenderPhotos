package com.kaii.photos.compose.settings

import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.R
import com.kaii.photos.LocalNavController
import com.kaii.photos.compose.PreferencesSwitchRow
import com.kaii.photos.compose.PreferencesSeparatorText
import com.kaii.photos.helpers.CustomMaterialTheme
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.MainActivity.Companion.mainViewModel
import com.kaii.photos.datastore.Debugging

@Composable
fun DebuggingSettingsPage() {
	Scaffold (
		topBar = {
			DebuggingSettingsTopBar()
		}
	) { innerPadding ->
        LazyColumn (
            modifier = Modifier
                .padding(innerPadding)
                .background(CustomMaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
        	item {
        		PreferencesSeparatorText("Logs")
        	}

        	item {
                val shouldRecordLogs by mainViewModel.settings.Debugging.getRecordLogs().collectAsStateWithLifecycle(initialValue = false)

                PreferencesSwitchRow(
                    title = "Record Logs",
                    summary = "Store logs in 'Internal Storage/LavenderPhotos/logs.txt'",
                    iconResID = R.drawable.logs,
                    checked = shouldRecordLogs,
                    position = RowPosition.Single,
                    showBackground = false
                ) {
					mainViewModel.settings.Debugging.setRecordLogs(it)
                }
        	}
        }
	}
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DebuggingSettingsTopBar() {
	val navController = LocalNavController.current ?: return

	TopAppBar(
        title = {
            Text(
                text = "Debugging",
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
        )
    )
}
