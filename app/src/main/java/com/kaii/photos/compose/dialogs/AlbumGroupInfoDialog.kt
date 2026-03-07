package com.kaii.photos.compose.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastMap
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.user_action.ConfirmationDialog
import com.kaii.photos.compose.dialogs.user_action.TextEntryDialog
import com.kaii.photos.datastore.AlbumGroup
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.TextStylingConstants
import kotlinx.coroutines.runBlocking

@Preview
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumGroupInfoDialogPreview() {
    val state = rememberModalBottomSheetState()
    runBlocking { state.expand() }

    AlbumGroupInfoDialog(
        sheetState = state,
        group = { AlbumGroup("", "Group", false, emptyList()) },
        groups = { emptyList() },
        editGroup = { _, _, _ -> },
        deleteGroup = {},
        onDismiss = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumGroupInfoDialog(
    sheetState: SheetState,
    group: () -> AlbumGroup,
    groups: () -> List<AlbumGroup>,
    modifier: Modifier = Modifier,
    editGroup: (id: String, name: String, pinned: Boolean) -> Unit,
    deleteGroup: (id: String) -> Unit,
    onDismiss: () -> Unit
) {
    // remove (weird) drag handle ripple
    CompositionLocalProvider(
        LocalRippleConfiguration provides
                RippleConfiguration(
                    color = Color.Transparent,
                    rippleAlpha = RippleAlpha(0f, 0f, 0f, 0f)
                )
    ) {
        ModalBottomSheet(
            sheetState = sheetState,
            tonalElevation = 16.dp,
            shape = RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp),
            containerColor = MaterialTheme.colorScheme.background,
            onDismissRequest = onDismiss,
            contentWindowInsets = {
                WindowInsets.systemBars
            },
            modifier = modifier
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // reset ripple for normal buttons
            CompositionLocalProvider(
                LocalRippleConfiguration provides RippleConfiguration()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp, start = 16.dp, bottom = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(
                        space = 16.dp,
                        alignment = Alignment.Top
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(id = R.string.album_group),
                        fontSize = TextStylingConstants.MEDIUM_TEXT_SIZE.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconContent(
                        group = group,
                        groups = groups,
                        editGroup = editGroup,
                        deleteGroup = deleteGroup
                    )

                    InfoContent(
                        group = group
                    )
                }
            }
        }
    }
}

@Composable
private fun IconContent(
    group: () -> AlbumGroup,
    groups: () -> List<AlbumGroup>,
    modifier: Modifier = Modifier,
    editGroup: (id: String, name: String, pinned: Boolean) -> Unit,
    deleteGroup: (id: String) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            space = 12.dp,
            alignment = Alignment.CenterHorizontally
        )
    ) {
        val isPinned = groups().find {
            it.id == group().id
        }?.pinned ?: false

        IconButton(
            onClick = {
                val info = group()
                editGroup(info.id, info.name, !isPinned)
            },
            modifier = modifier
                .height(48.dp)
                .weight(1f)
        ) {
            Icon(
                painter = painterResource(
                    id = if (isPinned) R.drawable.keep_off else R.drawable.keep
                ),
                contentDescription =
                    if (isPinned) stringResource(id = R.string.albums_unpin)
                    else stringResource(id = R.string.albums_pin)
            )
        }

        var showRenameDialog by remember { mutableStateOf(false) }
        if (showRenameDialog) {
            TextEntryDialog(
                title = stringResource(id = R.string.media_rename),
                placeholder = group().name,
                startValue = group().name,
                errorMessage = stringResource(id = R.string.albums_rename_failure),
                onConfirm = { new ->
                    editGroup(group().id, new, group().pinned)
                    showRenameDialog = false
                    true
                },
                onValueChange = { new ->
                    new.isNotEmpty() && new !in groups().fastMap { it.name }
                },
                onDismiss = {
                    showRenameDialog = false
                }
            )
        }

        IconButton(
            onClick = {
                showRenameDialog = true
            },
            modifier = modifier
                .height(48.dp)
                .weight(1f)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.name),
                contentDescription = stringResource(id = R.string.media_rename)
            )
        }

        val showDeleteDialog = remember { mutableStateOf(false) }
        ConfirmationDialog(
            showDialog = showDeleteDialog,
            dialogTitle = stringResource(id = R.string.album_group_delete),
            confirmButtonLabel = stringResource(id = R.string.media_delete)
        ) {
            deleteGroup(group().id)
        }

        IconButton(
            onClick = {
                showDeleteDialog.value = true
            },
            modifier = modifier
                .height(48.dp)
                .weight(1f)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.delete),
                contentDescription = stringResource(id = R.string.album_group_delete)
            )
        }
    }
}

@Composable
private fun InfoContent(
    group: () -> AlbumGroup,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resources = LocalResources.current

    LazyColumn(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
    ) {
        item {
            TallDialogInfoRow(
                title = stringResource(id = R.string.album_group_dialog_name),
                info = group().name,
                icon = R.drawable.label,
                position = RowPosition.Top,
                onClick = {
                    val clipboardManager =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clipData = ClipData.newPlainText(
                        resources.getString(R.string.album_group_dialog_name),
                        group().name
                    )
                    clipboardManager.setPrimaryClip(clipData)
                }
            )
        }

        item {
            TallDialogInfoRow(
                title = stringResource(id = R.string.album_group_item_count),
                info = group().albumIds.size.toString(),
                icon = R.drawable.data,
                position = RowPosition.Bottom,
                onClick = {
                    val clipboardManager =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clipData = ClipData.newPlainText(
                        resources.getString(R.string.album_group_item_count),
                        group().albumIds.size.toString()
                    )
                    clipboardManager.setPrimaryClip(clipData)
                }
            )
        }
    }
}