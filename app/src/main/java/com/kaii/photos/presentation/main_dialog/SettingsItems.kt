package com.kaii.photos.presentation.main_dialog

import com.kaii.photos.R
import com.kaii.photos.helpers.Screens

enum class SettingsItems(
    val title: Int,
    val icon: Int,
    val screen: Screens
) {
    General(
        title = R.string.settings_general,
        icon = R.drawable.settings,
        screen = Screens.Settings.MainPage.General
    ),

    PrivacyAndSecurity(
        title = R.string.settings_privacy,
        icon = R.drawable.shield_lock,
        screen = Screens.Settings.MainPage.PrivacyAndSecurity
    ),

    LookAndFeel(
        title = R.string.settings_look_and_feel,
        icon = R.drawable.paintbrush,
        screen = Screens.Settings.MainPage.LookAndFeel
    ),

    Behaviour(
        title = R.string.settings_behaviour,
        icon = R.drawable.graph,
        screen = Screens.Settings.MainPage.Behaviour
    ),

    MemoryAndStorage(
        title = R.string.settings_memory_storage,
        icon = R.drawable.storage,
        screen = Screens.Settings.MainPage.MemoryAndStorage
    )
}