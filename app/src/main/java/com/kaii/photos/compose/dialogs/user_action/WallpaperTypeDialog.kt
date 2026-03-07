package com.kaii.photos.compose.dialogs.user_action

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.LavenderDialogBase
import com.kaii.photos.compose.dialogs.TitleCloseRow
import com.kaii.photos.compose.pages.WallpaperSetter
import com.kaii.photos.compose.widgets.PreferencesRow
import com.kaii.photos.compose.widgets.PreferencesSwitchRow
import com.kaii.photos.helpers.RowPosition

@Composable
fun WallpaperTypeDialog(
    modifier: Modifier = Modifier,
    onSetWallpaperType: (WallpaperSetter.Type, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    LavenderDialogBase(
        onDismiss = onDismiss,
        modifier = modifier
    ) {
        TitleCloseRow(
            title = stringResource(id = R.string.set_as_wallpaper),
            closeOffset = 12.dp
        ) {
            onDismiss()
        }

        Spacer(modifier = Modifier.height(12.dp))

        var scrollable by remember { mutableStateOf(false) }

        PreferencesSwitchRow(
            title = stringResource(id = R.string.wallpaper_scrollable),
            iconResID = R.drawable.vertical_split,
            position = RowPosition.Single,
            checked = scrollable
        ) {
            scrollable = it
        }

        Spacer(modifier = Modifier.height(8.dp))

        PreferencesRow(
            title = stringResource(id = R.string.wallpaper_homescreen),
            position = RowPosition.Top,
            iconResID = R.drawable.mobile,
            modifier = Modifier
                .height(36.dp)
        ) {
            onSetWallpaperType(WallpaperSetter.Type.HomeScreen, scrollable)
            onDismiss()
        }

        PreferencesRow(
            title = stringResource(id = R.string.wallpaper_lockscreen),
            position = RowPosition.Middle,
            iconResID = R.drawable.mobile_lock,
            modifier = Modifier
                .height(36.dp)
        ) {
            onSetWallpaperType(WallpaperSetter.Type.LockScreen, scrollable)
            onDismiss()
        }

        PreferencesRow(
            title = stringResource(id = R.string.wallpaper_Both),
            position = RowPosition.Bottom,
            iconResID = R.drawable.mobile_lock_alternate,
            modifier = Modifier
                .height(36.dp)
        ) {
            onSetWallpaperType(WallpaperSetter.Type.Both, scrollable)
            onDismiss()
        }
    }
}