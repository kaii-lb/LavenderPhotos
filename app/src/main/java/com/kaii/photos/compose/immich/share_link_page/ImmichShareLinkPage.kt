package com.kaii.photos.compose.immich.share_link_page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.immich.ImmichCopyShareLinkDialog
import com.kaii.photos.compose.dialogs.user_action.ConfirmationDialog
import com.kaii.photos.compose.widgets.ClearableTextField
import com.kaii.photos.compose.widgets.DropDownSwitchRow
import com.kaii.photos.compose.widgets.SwitchRow
import com.kaii.photos.compose.widgets.TextDropDownSwitchRow
import com.kaii.photos.compose.widgets.date_time.DateTimePicker
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.TopBarDetailsFormat
import com.kaii.photos.helpers.expiryDate
import com.kaii.photos.models.immich_share_album_page.CreateLinkState
import com.kaii.photos.models.immich_share_album_page.ImmichShareAlbumViewModel
import com.kaii.photos.widgets.rememberDateTimePickerState
import io.github.kaii_lb.lavender.immichintegration.serialization.shared_links.SharedLinkResponseDto
import io.github.kaii_lb.lavender.immichintegration.serialization.shared_links.SharedLinkType
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Preview
@Composable
private fun ImmichShareLinkPagePreview() {
    ImmichShareLinkPageImpl(
        latestImage = "",
        albumTitle = "Album Title",
        itemCount = 256,
        customSlug = { null },
        password = { null },
        description = { "" },
        allowUploads = { false },
        allowDownloads = { true },
        usesExpiryDate = { false },
        links = {
            buildList {
                repeat(5) {
                    add(
                        SharedLinkResponseDto(
                            album = null,
                            allowDownload = true,
                            allowUpload = false,
                            assets = emptyList(),
                            createdAt = "May 16, 2026",
                            description = "description",
                            expiresAt = "May 17, 2026",
                            id = Uuid.fromLongs(0L, it.toLong()),
                            key = "",
                            password = "some password",
                            showMetadata = false,
                            slug = "kitty",
                            type = SharedLinkType.Album,
                            userId = Uuid.NIL
                        )
                    )
                }
            }
        },
        navController = rememberNavController(),
        modifier = Modifier,
        setCustomSlug = {},
        setPassword = {},
        setDescription = {},
        setExpiryDate = {},
        setAllowUploads = {},
        setAllowDownloads = {},
        removeLink = {},
        showLink = { _, _ -> },
        onConfirm = {}
    )
}

@Composable
fun ImmichShareLinkPage(
    latestImage: String,
    albumTitle: String,
    itemCount: Int,
    viewModel: ImmichShareAlbumViewModel,
    modifier: Modifier = Modifier
) {
    val sharedLinkState = viewModel.shareLinkState
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        ImmichCopyShareLinkDialog(
            state = { state },
            onDismiss = {
                viewModel.dismiss()
                showDialog = false
            }
        )
    }

    LaunchedEffect(state) {
        if (state !is CreateLinkState.Idle) {
            showDialog = true
        }
    }

    var linkId by remember { mutableStateOf("false") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val links by viewModel.links.collectAsStateWithLifecycle()

    if (showDeleteDialog) {
        ConfirmationDialog(
            title = stringResource(id = R.string.immich_share_album_delete_link),
            confirmButtonLabel = stringResource(id = R.string.media_delete),
            action = {
                viewModel.removeLink(linkId)
            },
            onDismiss = {
                showDeleteDialog = false
            }
        )
    }

    ImmichShareLinkPageImpl(
        latestImage = latestImage,
        albumTitle = albumTitle,
        itemCount = itemCount,
        customSlug = { sharedLinkState.customSlug },
        password = { sharedLinkState.password },
        description = { sharedLinkState.description },
        allowUploads = { sharedLinkState.allowUploads },
        allowDownloads = { sharedLinkState.allowDownloads },
        usesExpiryDate = { sharedLinkState.expiryDate != null },
        links = { links },
        navController = LocalNavController.current,
        modifier = modifier,
        setCustomSlug = { sharedLinkState.customSlug = it },
        setPassword = { sharedLinkState.password = it },
        setDescription = { sharedLinkState.description = it },
        setExpiryDate = { sharedLinkState.expiryDate = it },
        setAllowUploads = { sharedLinkState.allowUploads = it },
        setAllowDownloads = { sharedLinkState.allowDownloads = it },
        removeLink = { id ->
            linkId = id
            showDeleteDialog = true
        },
        showLink = viewModel::showLink,
        onConfirm = viewModel::createLink
    )
}

@Composable
private fun ImmichShareLinkPageImpl(
    latestImage: String,
    albumTitle: String,
    itemCount: Int,
    customSlug: () -> String?,
    password: () -> String?,
    description: () -> String,
    allowUploads: () -> Boolean,
    allowDownloads: () -> Boolean,
    usesExpiryDate: () -> Boolean,
    links: () -> List<SharedLinkResponseDto>,
    navController: NavController,
    modifier: Modifier,
    setCustomSlug: (slug: String?) -> Unit,
    setPassword: (password: String?) -> Unit,
    setDescription: (description: String) -> Unit,
    setExpiryDate: (dateTime: Instant?) -> Unit,
    setAllowUploads: (Boolean) -> Unit,
    setAllowDownloads: (Boolean) -> Unit,
    removeLink: (id: String) -> Unit,
    showLink: (slug: String?, id: String) -> Unit,
    onConfirm: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            ImmichLoginPageTopbar(
                navController = navController,
                onConfirm = onConfirm
            )
        },
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeContent
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(
                space = 4.dp,
                alignment = Alignment.Top
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                ImmichShareLinkBanner(
                    image = latestImage,
                    albumTitle = albumTitle,
                    itemCount = itemCount
                )

                Spacer(modifier = Modifier.height(4.dp))
            }

            item {
                Text(
                    text = stringResource(id = R.string.immich_share_album_customize_link),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))
            }

            item {
                ClearableTextField(
                    value = description(),
                    onValueChange = setDescription,
                    placeholder = stringResource(id = R.string.immich_share_album_description),
                    icon = R.drawable.description,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    onClear = {
                        setDescription("")
                    },
                    onConfirm = {
                        focusManager.moveFocus(FocusDirection.Down)
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))
            }

            item {
                TextDropDownSwitchRow(
                    text = stringResource(id = R.string.immich_share_album_use_custom_url),
                    checked = { customSlug() != null },
                    onCheckedChange = { checked ->
                        setCustomSlug("".takeIf { checked })
                    },
                    icon = R.drawable.link,
                    placeholder = stringResource(id = R.string.immich_share_album_custom_url),
                    textFieldValue = customSlug,
                    shape = RoundedCornerShape(
                        topStart = 32.dp, topEnd = 32.dp,
                        bottomStart = 8.dp, bottomEnd = 8.dp
                    ),
                    onTextFieldValueChange = setCustomSlug,
                    onConfirm = {
                        focusManager.moveFocus(FocusDirection.Down)
                    }
                )
            }

            item {
                TextDropDownSwitchRow(
                    text = stringResource(id = R.string.immich_share_album_require_password),
                    checked = { password() != null },
                    onCheckedChange = { checked ->
                        setPassword("".takeIf { checked })
                    },
                    icon = R.drawable.password,
                    placeholder = stringResource(id = R.string.immich_auth_password),
                    textFieldValue = password,
                    shape = RoundedCornerShape(
                        topStart = 8.dp, topEnd = 8.dp,
                        bottomStart = 8.dp, bottomEnd = 8.dp
                    ),
                    onTextFieldValueChange = setPassword,
                    onConfirm = {
                        focusManager.moveFocus(FocusDirection.Down)
                    }
                )
            }

            item {
                SwitchRow(
                    text = stringResource(id = R.string.immich_share_album_allow_uploads),
                    checked = allowUploads,
                    position = RowPosition.Middle,
                    padding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    showBackground = true,
                    onCheckedChange = setAllowUploads
                )
            }

            item {
                SwitchRow(
                    text = stringResource(id = R.string.immich_share_album_allow_downloads),
                    checked = allowDownloads,
                    position = RowPosition.Middle,
                    padding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    showBackground = true,
                    onCheckedChange = setAllowDownloads
                )
            }

            item {
                var showDateTimePicker by remember { mutableStateOf(false) }
                val state = rememberDateTimePickerState(
                    initialDateTime = Clock.System.now()
                        .apply {
                            plus(1.days)
                        }
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                )

                if (showDateTimePicker) {
                    DateTimePicker(
                        state = state,
                        onConfirm = setExpiryDate,
                        onDismiss = {
                            showDateTimePicker = false
                        }
                    )
                }

                DropDownSwitchRow(
                    text = stringResource(id = R.string.immich_share_album_use_expiry_date),
                    checked = usesExpiryDate,
                    onCheckedChange = { checked ->
                        showDateTimePicker = checked

                        if (!checked) {
                            setExpiryDate(null)
                        }
                    },
                    shape = RoundedCornerShape(
                        topStart = 8.dp, topEnd = 8.dp,
                        bottomStart = 32.dp, bottomEnd = 32.dp
                    ),
                    content = {
                        val context = LocalContext.current
                        Text(
                            text = TopBarDetailsFormat.DateTime
                                .format(context, "", state.getDateTime().epochSeconds),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .fillMaxWidth()
                                .clip(CircleShape)
                                .clickable {
                                    showDateTimePicker = true
                                }
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .padding(all = 8.dp)
                        )
                    }
                )
            }

            if (links().isNotEmpty()) {
                item(
                    key = "other_links"
                ) {
                    Text(
                        text = stringResource(id = R.string.immich_share_album_other_links),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .animateItem()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                }

                items(
                    count = links().size,
                    key = { index ->
                        links().getOrNull(index)?.id ?: index
                    }
                ) { index ->
                    val link = links().getOrNull(index)
                    if (link != null) {
                        LinkItem(
                            title = link.description ?: link.slug ?: albumTitle,
                            summary = getLinkItemDescription(link = link),
                            expiresAt = link.expiryDate(context = LocalContext.current),
                            position =
                                when {
                                    links().size == 1 -> RowPosition.Single
                                    index == 0 -> RowPosition.Top
                                    index == links().size - 1 -> RowPosition.Bottom
                                    else -> RowPosition.Middle
                                },
                            modifier = Modifier
                                .animateItem(),
                            copyLink = {
                                showLink(link.slug, link.id.toString())
                            },
                            deleteLink = {
                                removeLink(link.id.toString())
                            }
                        )
                    }
                }
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImmichLoginPageTopbar(
    navController: NavController,
    onConfirm: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(id = R.string.immich_share_album),
                style = MaterialTheme.typography.titleMedium
            )
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    navController.popBackStack()
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.back_arrow),
                    contentDescription = stringResource(id = R.string.return_to_previous_page),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        },
        actions = {
            Button(
                onClick = onConfirm
            ) {
                Text(
                    text = stringResource(id = R.string.immich_share_album_create_link),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}