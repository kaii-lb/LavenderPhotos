package com.kaii.photos.presentation.ui.theme

import android.os.Build

data class ThemeConfiguration(
    val theme: LavenderThemes.Theme,
    val style: LavenderThemes.Style,
    val dynamic: Boolean
) {
    companion object {
        val Default = ThemeConfiguration(
            theme = LavenderThemes.Theme.Apple,
            style = LavenderThemes.Style.System,
            dynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        )
    }

    constructor(serial: Int) : this(
        theme = LavenderThemes.Theme.entries[serial / 100 % 10],
        style = LavenderThemes.Style.entries[serial / 10 % 10],
        dynamic = serial % 10 == 1
    )


    fun serialize(): Int {
        val dyn = if (dynamic) 1 else 0

        return theme.ordinal * 100 + style.ordinal * 10 + dyn
    }
}
