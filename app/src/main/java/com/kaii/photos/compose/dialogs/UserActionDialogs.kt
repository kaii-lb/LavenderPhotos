package com.kaii.photos.compose.dialogs

import android.content.Context
import android.content.res.Configuration
import android.os.storage.StorageManager
import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.lavender.immichintegration.serialization.LoginCredentials
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.LocalAppDatabase
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.MainActivity.Companion.immichViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.FullWidthDialogButton
import com.kaii.photos.compose.WallpaperSetter
import com.kaii.photos.compose.widgets.CheckBoxButtonRow
import com.kaii.photos.compose.widgets.ClearableTextField
import com.kaii.photos.compose.widgets.PreferencesRow
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.AlbumsList
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.helpers.createDirectoryPicker
import com.kaii.photos.helpers.findMinParent
import com.kaii.photos.helpers.getParentFromPath
import com.kaii.photos.helpers.toBasePath
import com.kaii.photos.helpers.toRelativePath
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

private const val TAG = "com.kaii.photos.compose.dialogs.UserActionDialogs"

@Composable
fun ConfirmationDialog(
    showDialog: MutableState<Boolean>,
    dialogTitle: String,
    confirmButtonLabel: String,
    action: () -> Unit
) {
    val localConfig = LocalConfiguration.current
    var isLandscape by remember { mutableStateOf(localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) }

    LaunchedEffect(localConfig) {
        isLandscape = localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    val modifier = if (isLandscape)
        Modifier.width(256.dp)
    else
        Modifier

    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = {
                showDialog.value = false
            },
            modifier = modifier,
            confirmButton = {
                Button(
                    onClick = {
                        showDialog.value = false
                        action()
                    }
                ) {
                    Text(
                        text = confirmButtonLabel,
                        fontSize = TextUnit(14f, TextUnitType.Sp)
                    )
                }
            },
            title = {
                Text(
                    text = dialogTitle,
                    fontSize = TextUnit(16f, TextUnitType.Sp)
                )
            },
            dismissButton = {
                Button(
                    onClick = {
                        showDialog.value = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.media_cancel),
                        fontSize = TextUnit(14f, TextUnitType.Sp)
                    )
                }
            },
            shape = RoundedCornerShape(32.dp)
        )
    }
}

@Composable
fun ConfirmationDialogWithBody(
    showDialog: MutableState<Boolean>,
    dialogTitle: String,
    dialogBody: String,
    confirmButtonLabel: String,
    showCancelButton: Boolean = true,
    action: () -> Unit
) {
    val localConfig = LocalConfiguration.current
    var isLandscape by remember { mutableStateOf(localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) }

    LaunchedEffect(localConfig) {
        isLandscape = localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    val modifier = if (isLandscape)
        Modifier.width(256.dp)
    else
        Modifier

    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = {
                showDialog.value = false
            },
            modifier = modifier,
            confirmButton = {
                Button(
                    onClick = {
                        showDialog.value = false
                        action()
                    }
                ) {
                    Text(
                        text = confirmButtonLabel,
                        fontSize = TextUnit(14f, TextUnitType.Sp)
                    )
                }
            },
            title = {
                Text(
                    text = dialogTitle,
                    fontSize = TextUnit(16f, TextUnitType.Sp)
                )
            },
            text = {
                Text(
                    text = dialogBody,
                    fontSize = TextUnit(14f, TextUnitType.Sp)
                )
            },
            dismissButton = {
                if (showCancelButton) {
                    Button(
                        onClick = {
                            showDialog.value = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Text(
                            text = stringResource(id = R.string.media_cancel),
                            fontSize = TextUnit(14f, TextUnitType.Sp)
                        )
                    }
                }
            },
            shape = RoundedCornerShape(32.dp)
        )
    }
}

@Composable
fun TextEntryDialog(
    title: String,
    placeholder: String? = null,
    onConfirm: (text: String) -> Boolean,
    onValueChange: (text: String) -> Boolean,
    onDismiss: () -> Unit
) {
    LavenderDialogBase(
        onDismiss = onDismiss
    ) {
        Text(
            text = title,
            fontSize = TextUnit(18f, TextUnitType.Sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.wrapContentSize()
        )

        Spacer(modifier = Modifier.height(24.dp))

        val keyboardController = LocalSoftwareKeyboardController.current
        var text by remember { mutableStateOf("") }
        var showError by remember { mutableStateOf(false) }

        TextField(
            value = text,
            onValueChange = {
                text = it
                showError = !onValueChange(it)
            },
            maxLines = 1,
            singleLine = true,
            placeholder = {
                if (placeholder != null) {
                    Text(
                        text = placeholder,
                        fontSize = TextUnit(16f, TextUnitType.Sp)
                    )
                }
            },
            suffix = {
                if (showError) {
                    Row {
                        val coroutineScope = rememberCoroutineScope()
                        val resources = LocalResources.current

                        Icon(
                            painter = painterResource(id = R.drawable.error_2),
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable {
                                    coroutineScope.launch {
                                        LavenderSnackbarController.pushEvent(
                                            LavenderSnackbarEvents.MessageEvent(
                                                message = resources.getString(R.string.paths_should_be_relative),
                                                icon = R.drawable.error_2,
                                                duration = SnackbarDuration.Short
                                            )
                                        )
                                    }
                                }
                        )
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboardController?.hide()
                }
            ),
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth(1f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        FullWidthDialogButton(
            text = stringResource(id = R.string.media_confirm),
            color = MaterialTheme.colorScheme.primary,
            textColor = MaterialTheme.colorScheme.onPrimary,
            position = RowPosition.Single,
            enabled = !showError
        ) {
            showError = !onConfirm(text)
        }
    }
}

@Composable
fun ExplanationDialog(
    title: String,
    explanation: String,
    showPreviousDialog: MutableState<Boolean>? = null,
    onDismiss: () -> Unit
) {
    ExplanationDialogBase(
        title = title,
        body = {
            Text(
                text = explanation,
                fontSize = TextUnit(14f, TextUnitType.Sp),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.wrapContentSize()
            )
        },
        onDismiss = onDismiss,
        showPreviousDialog = showPreviousDialog
    )
}

@Composable
fun AnnotatedExplanationDialog(
    title: String,
    annotatedExplanation: AnnotatedString,
    showPreviousDialog: MutableState<Boolean>? = null,
    onDismiss: () -> Unit
) {
    ExplanationDialogBase(
        title = title,
        body = {
            Text(
                text = annotatedExplanation,
                fontSize = TextUnit(14f, TextUnitType.Sp),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.wrapContentSize()
            )
        },
        showPreviousDialog = showPreviousDialog,
        onDismiss = onDismiss
    )
}

@Composable
private fun ExplanationDialogBase(
    title: String,
    body: @Composable () -> Unit,
    showPreviousDialog: MutableState<Boolean>? = null,
    onDismiss: () -> Unit
) {
    showPreviousDialog?.value = false

    LavenderDialogBase(
        modifier = Modifier
            .animateContentSize(),
        onDismiss = onDismiss
    ) {
        Text(
            text = title,
            fontSize = TextUnit(18f, TextUnitType.Sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.wrapContentSize()
        )

        Spacer(modifier = Modifier.height(24.dp))

        body()

        Spacer(modifier = Modifier.height(24.dp))

        FullWidthDialogButton(
            text = stringResource(id = R.string.media_okay),
            color = MaterialTheme.colorScheme.primary,
            textColor = MaterialTheme.colorScheme.onPrimary,
            position = RowPosition.Single
        ) {
            showPreviousDialog?.value = true

            onDismiss()
        }
    }
}

@Composable
fun AlbumPathsDialog(
    albumInfo: AlbumInfo,
    onConfirm: (selectedPaths: List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedPaths = remember { albumInfo.paths.toMutableStateList() }

    LavenderDialogBase(
        onDismiss = onDismiss
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(1f)
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(0.dp, 0.dp, 0.dp, 4.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.close),
                    contentDescription = "Close this dialog",
                    modifier = Modifier
                        .size(24.dp)
                )
            }

            Text(
                text = albumInfo.name,
                fontSize = TextUnit(18f, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.Center)
            )

            val activityLauncher = createDirectoryPicker { path, basePath ->
                if (path != null && basePath != null) {
                    val absolutePath = basePath + path

                    if (!selectedPaths.contains(absolutePath)) selectedPaths.add(absolutePath)

                    Log.d(TAG, "Path $absolutePath and selected $selectedPaths")
                }
            }

            IconButton(
                onClick = {
                    activityLauncher.launch(null)
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(0.dp, 0.dp, 0.dp, 4.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.add),
                    contentDescription = "add a new album",
                    modifier = Modifier
                        .size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        var volumes by remember {
            mutableStateOf(
                selectedPaths.groupBy {
                    it.toBasePath()
                }
            )
        }

        LaunchedEffect(selectedPaths.size) {
            volumes = selectedPaths.groupBy {
                it.toBasePath()
            }
        }

        LazyColumn(
            modifier = Modifier
                .heightIn(max = 250.dp)
                .fillMaxWidth(1f)
                .animateContentSize()
        ) {
            itemsIndexed(
                items = volumes.keys.toList()
            ) { index, volume ->
                val rowPosition = when {
                    volumes.size == 1 -> {
                        RowPosition.Single
                    }

                    index == 0 -> {
                        RowPosition.Top
                    }

                    index == volumes.size - 1 -> {
                        RowPosition.Bottom
                    }

                    else -> {
                        RowPosition.Middle
                    }
                }

                val expanded = remember { mutableStateOf(volumes.size == 1) }
                val context = LocalContext.current
                val externalVolumes = remember {
                    val manager =
                        context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
                    manager.storageVolumes
                }

                DialogExpandableItem(
                    text = externalVolumes.find {
                        it.directory?.absolutePath == volume.removeSuffix(
                            "/"
                        )
                    }?.getDescription(context) ?: "Some Place",
                    iconResId = R.drawable.drop_down_arrow,
                    position = rowPosition,
                    expanded = expanded
                ) {
                    val children = volumes[volume]!!

                    data class PathItem(
                        val path: String,
                        val children: List<PathItem>?
                    )

                    val relative = children
                        .sortedBy { it.length }

                    val uniques = run {
                        val total = mutableListOf<String>()

                        relative
                            .groupBy { it.toRelativePath(true).substringBefore("/") }
                            .forEach { group ->
                                val min = findMinParent(group.value)
                                val grouped = min
                                    .groupBy { it.getParentFromPath() }
                                    .map { (key, value) ->
                                        if (value.size > 1) key
                                        else value.first()
                                    }

                                total.addAll(grouped)
                            }

                        total
                    }

                    Log.d(TAG, "Uniques are $uniques")

                    val hierarchy = run {
                        val list = mutableListOf<PathItem>()

                        fun buildHierarchy(path: String): PathItem {
                            val possibleChildren = children.filter {
                                it.toRelativePath(true).getParentFromPath() == path.toRelativePath(
                                    true
                                )
                            }.toMutableList()

                            val possibleSubChildren = children.filter {
                                it.toRelativePath(true)
                                    .getParentFromPath()
                                    .startsWith(path.toRelativePath(true))
                            }
                            if (possibleSubChildren.isNotEmpty()) {
                                possibleChildren.addAll(
                                    possibleSubChildren.filter { child ->
                                        child !in possibleChildren && !possibleChildren.any {
                                            it.endsWith(
                                                child.toRelativePath(true)
                                                    .getParentFromPath()
                                            )
                                        }
                                    }
                                )
                            }

                            return PathItem(
                                path = path,
                                children = possibleChildren.map { buildHierarchy(it) }
                            )
                        }

                        uniques.sortedBy { it.length }.forEach { path ->
                            val item = buildHierarchy(path)
                            list.add(item)
                        }

                        list
                    }

                    @Composable
                    fun getChildren(item: PathItem, parent: PathItem) {
                        Row(
                            modifier = Modifier
                                .wrapContentSize(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            if (parent.children != null) {
                                Icon(
                                    painter = painterResource(id = R.drawable.subdirectory_arrow_right),
                                    contentDescription = "Subdirectory icon",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .offset(x = 8.dp, y = 4.dp)
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .wrapContentSize()
                                    .offset(x = (-12).dp)
                            ) {
                                CheckBoxButtonRow(
                                    text = item.path.replace(parent.path, "").removePrefix("/"),
                                    checked = selectedPaths.contains(item.path),
                                    checkBoxTextSpacing = 0.dp,
                                    height = 32.dp
                                ) {
                                    if (selectedPaths.contains(item.path)) {
                                        selectedPaths.remove(item.path)
                                    } else {
                                        selectedPaths.add(item.path)
                                    }
                                }

                                if (item.children != null) {
                                    Column(
                                        modifier = Modifier
                                            .padding(start = 20.dp)
                                    ) {
                                        item.children.forEach { child ->
                                            getChildren(child, item)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    hierarchy.forEach {
                        getChildren(it, PathItem(it.path.toBasePath(), null))
                    }
                }
            }
        }


        Spacer(modifier = Modifier.height(24.dp))

        FullWidthDialogButton(
            text = stringResource(id = R.string.media_confirm),
            color = MaterialTheme.colorScheme.primary,
            textColor = MaterialTheme.colorScheme.onPrimary,
            position = RowPosition.Single
        ) {
            onConfirm(selectedPaths)
            onDismiss()
        }
    }
}

@Composable
fun AlbumAddChoiceDialog(
    onDismiss: () -> Unit = {}
) {
    LavenderDialogBase(
        onDismiss = onDismiss
    ) {
        TitleCloseRow(
            title = stringResource(id = R.string.albums_type),
            onClose = onDismiss,
            closeOffset = 8.dp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth(1f)
                .wrapContentHeight()
                .padding(8.dp, 0.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val mainViewModel = LocalMainViewModel.current
            val activityLauncher = createDirectoryPicker { path, basePath ->
                if (path != null && basePath != null) mainViewModel.settings.AlbumsList.addToAlbumsList(
                    AlbumInfo(
                        id = (basePath + path).hashCode(),
                        name = path.split("/").last(),
                        paths = listOf(basePath + path)
                    )
                )
            }

            PreferencesRow(
                title = stringResource(id = R.string.albums_folder),
                summary = stringResource(id = R.string.albums_folder_desc),
                position = RowPosition.Top,
                iconResID = R.drawable.albums
            ) {
                activityLauncher.launch(null)
            }

            var showCustomAlbumDialog by remember { mutableStateOf(false) }
            if (showCustomAlbumDialog) {
                AddCustomAlbumDialog(
                    onDismissPrev = onDismiss,
                    onDismiss = {
                        showCustomAlbumDialog = false
                    }
                )
            }

            PreferencesRow(
                title = stringResource(id = R.string.albums_custom),
                summary = stringResource(id = R.string.albums_custom_desc),
                position = RowPosition.Bottom,
                iconResID = R.drawable.art_track
            ) {
                showCustomAlbumDialog = true
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun AddCustomAlbumDialog(
    onDismiss: () -> Unit,
    onDismissPrev: () -> Unit
) {
    val mainViewModel = LocalMainViewModel.current
    val appDatabase = LocalAppDatabase.current
    val autoDetectAlbums by mainViewModel.settings.AlbumsList.getAutoDetect()
        .collectAsStateWithLifecycle(initialValue = true)
    val displayDateFormat by mainViewModel.displayDateFormat.collectAsStateWithLifecycle()

    val albums by if (autoDetectAlbums) {
        mainViewModel.settings.AlbumsList.getAutoDetectedAlbums(displayDateFormat, appDatabase)
            .collectAsStateWithLifecycle(initialValue = emptyList())
    } else {
        mainViewModel.settings.AlbumsList.getNormalAlbums()
            .collectAsStateWithLifecycle(initialValue = emptyList())
    }

    var name by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .padding(8.dp, 0.dp)
    ) {
        TextEntryDialog(
            title = stringResource(id = R.string.albums_custom),
            placeholder = stringResource(id = R.string.albums_name),
            onDismiss = onDismiss,
            onValueChange = { text ->
                name = text
                name in albums.map { it.name }
            },
            onConfirm = { text ->
                if (text in albums.map { it.name }) false
                else {
                    val albumInfo = AlbumInfo(
                        id = text.hashCode(),
                        paths = emptyList(),
                        name = text,
                        isCustomAlbum = true
                    )

                    mainViewModel.settings.AlbumsList.addToAlbumsList(albumInfo)
                    onDismissPrev()

                    true
                }
            }
        )
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun ImmichLoginDialog(
    endpointBase: String,
    onDismiss: () -> Unit,
) {
    LavenderDialogBase(
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp, 0.dp, 8.dp, 0.dp),
            verticalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                alignment = Alignment.CenterVertically
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val email = remember { mutableStateOf("") }
            val password = remember { mutableStateOf("") }

            val focusManager = LocalFocusManager.current

            TitleCloseRow(
                title = stringResource(id = R.string.immich_login),
                closeOffset = 16.dp,
                onClose = onDismiss
            )

            ClearableTextField(
                text = email,
                placeholder = stringResource(id = R.string.immich_auth_email),
                modifier = Modifier,
                icon = R.drawable.mail,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                keyboardOptions =
                    KeyboardOptions(
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Search
                    ),
                onConfirm = {
                    focusManager.moveFocus(FocusDirection.Down)
                },
                onClear = {
                    email.value = ""
                }
            )

            val coroutineScope = rememberCoroutineScope()
            val resources = LocalResources.current

            suspend fun login() {
                val eventTitle =
                    mutableStateOf(resources.getString(R.string.immich_login_ongoing))
                val isLoading = mutableStateOf(true)

                LavenderSnackbarController.pushEvent(
                    LavenderSnackbarEvents.LoadingEvent(
                        message = eventTitle.value,
                        icon = R.drawable.account_circle,
                        isLoading = isLoading
                    )
                )

                immichViewModel.loginUser(
                    credentials = LoginCredentials(
                        email = email.value.trim(),
                        password = password.value.trim()
                    ),
                    endpointBase = endpointBase
                ).let { success ->
                    if (success) {
                        eventTitle.value = resources.getString(R.string.immich_login_successful)
                        isLoading.value = false

                        onDismiss()
                    } else {
                        password.value = ""

                        eventTitle.value = resources.getString(R.string.immich_login_failed)
                        LavenderSnackbarController.pushEvent(
                            LavenderSnackbarEvents.MessageEvent(
                                message = resources.getString(R.string.immich_login_failed),
                                duration = SnackbarDuration.Short,
                                icon = R.drawable.error_2
                            )
                        )
                    }
                }
            }

            ClearableTextField(
                text = password,
                placeholder = stringResource(id = R.string.immich_auth_passowrd),
                modifier = Modifier,
                icon = R.drawable.password,
                onConfirm = {
                    coroutineScope.launch {
                        login()
                    }
                },
                visualTransformation = PasswordVisualTransformation(mask = '\u2B24'),
                keyboardOptions =
                    KeyboardOptions(
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Search
                    ),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                onClear = {
                    password.value = ""
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            FullWidthDialogButton(
                text = stringResource(id = R.string.immich_login_confirm),
                color = MaterialTheme.colorScheme.primary,
                textColor = MaterialTheme.colorScheme.onPrimary,
                position = RowPosition.Single
            ) {
                coroutineScope.launch {
                    login()
                }
            }
        }
    }
}

@Composable
fun ConfirmationDialogWithCustomBody(
    showDialog: MutableState<Boolean>,
    title: String,
    confirmButtonLabel: String,
    action: () -> Unit,
    body: @Composable (() -> Unit)
) {
    val localConfig = LocalConfiguration.current
    var isLandscape by remember { mutableStateOf(localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) }

    LaunchedEffect(localConfig) {
        isLandscape = localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    val modifier = if (isLandscape)
        Modifier.width(256.dp)
    else
        Modifier

    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = {
                showDialog.value = false
            },
            modifier = modifier,
            confirmButton = {
                Button(
                    onClick = {
                        showDialog.value = false
                        action()
                    }
                ) {
                    Text(
                        text = confirmButtonLabel,
                        fontSize = TextUnit(14f, TextUnitType.Sp)
                    )
                }
            },
            title = {
                Text(
                    text = title,
                    fontSize = TextUnit(16f, TextUnitType.Sp)
                )
            },
            text = body,
            dismissButton = {
                Button(
                    onClick = {
                        showDialog.value = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.media_cancel),
                        fontSize = TextUnit(14f, TextUnitType.Sp)
                    )
                }
            },
            shape = RoundedCornerShape(32.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SliderDialog(
    title: (Float) -> String,
    steps: Int = 0,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    startsAt: Float = 1f,
    onSetValue: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(startsAt) }

    LavenderDialogBase(
        onDismiss = onDismiss
    ) {
        Text(
            text = title(sliderValue),
            fontSize = TextUnit(TextStylingConstants.LARGE_TEXT_SIZE, TextUnitType.Sp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = sliderValue,
            valueRange = range,
            steps = steps,
            onValueChange = {
                sliderValue = it
            },
            track = { state ->
                SliderDefaults.Track(
                    sliderState = state,
                    drawTick = { offset, color -> },
                    modifier = Modifier
                        .height(40.dp)
                )
            },
            modifier = Modifier
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                alignment = Alignment.End,
                space = 8.dp
            )
        ) {
            FilledTonalButton(
                onClick = onDismiss
            ) {
                Text(
                    text = stringResource(id = R.string.media_cancel),
                    fontSize = TextUnit(TextStylingConstants.SMALL_TEXT_SIZE, TextUnitType.Sp)
                )
            }

            Button(
                onClick = {
                    onSetValue(sliderValue)
                    onDismiss()
                }
            ) {
                Text(
                    text = stringResource(id = R.string.media_confirm),
                    fontSize = TextUnit(TextStylingConstants.SMALL_TEXT_SIZE, TextUnitType.Sp)
                )
            }
        }
    }
}

@Composable
fun WallpaperTypeDialog(
    modifier: Modifier = Modifier,
    onSetWallpaperType: (WallpaperSetter.Type) -> Unit,
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

        PreferencesRow(
            title = stringResource(id = R.string.wallpaper_homescreen),
            position = RowPosition.Top,
            iconResID = R.drawable.mobile
        ) {
            onSetWallpaperType(WallpaperSetter.Type.HomeScreen)
            onDismiss()
        }

        PreferencesRow(
            title = stringResource(id = R.string.wallpaper_lockscreen),
            position = RowPosition.Middle,
            iconResID = R.drawable.mobile_lock
        ) {
            onSetWallpaperType(WallpaperSetter.Type.LockScreen)
            onDismiss()
        }

        PreferencesRow(
            title = stringResource(id = R.string.wallpaper_Both),
            position = RowPosition.Bottom,
            iconResID = R.drawable.mobile_lock_alternate
        ) {
            onSetWallpaperType(WallpaperSetter.Type.Both)
            onDismiss()
        }
    }
}