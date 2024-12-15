package com.kaii.photos.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kaii.photos.R
import com.kaii.photos.compose.PreferencesRow
import com.kaii.photos.helpers.CustomMaterialTheme
import com.kaii.photos.helpers.RowPosition

@Composable
fun MainSettingsPage(

) {
    Scaffold (

    ) { innerPadding ->
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
                    iconResID = R.drawable.settings,
                    position = RowPosition.Top,
                    showBackground = false
                ) {

                }
            }

            item {
                PreferencesRow(
                    title = "Privacy & Security",
                    iconResID = R.drawable.privacy_policy,
                    position = RowPosition.Middle,
                    showBackground = false
                ) {

                }
            }

            item {
                PreferencesRow(
                    title = "Look & Feel",
                    iconResID = R.drawable.palette,
                    position = RowPosition.Middle,
                    showBackground = false
                ) {

                }
            }

            item {
                PreferencesRow(
                    title = "Debugging",
                    iconResID = R.drawable.memory,
                    position = RowPosition.Bottom,
                    showBackground = false
                ) {

                }
            }
        }
    }
}