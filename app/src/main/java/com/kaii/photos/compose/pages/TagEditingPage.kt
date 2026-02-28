package com.kaii.photos.compose.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.ConfirmationDialog
import com.kaii.photos.compose.widgets.TagEditingPageTextField
import com.kaii.photos.compose.widgets.TagItem
import com.kaii.photos.compose.widgets.TagMediaInfo
import com.kaii.photos.database.entities.Tag
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.helpers.editing.random
import com.kaii.photos.helpers.toBasePath
import com.kaii.photos.models.tag_page.TagViewModel

@Composable
fun TagEditingPage(
    viewModel: TagViewModel,
    modifier: Modifier = Modifier
) {
    val allTags by viewModel.tags.collectAsStateWithLifecycle()
    val appliedTags by viewModel.appliedTags.collectAsStateWithLifecycle()
    val item by viewModel.item.collectAsStateWithLifecycle()

    val showDialog = remember { mutableStateOf(false) }
    var currentTag by remember { mutableStateOf<Tag?>(null) }
    ConfirmationDialog(
        showDialog = showDialog,
        dialogTitle = stringResource(id = R.string.tags_delete),
        confirmButtonLabel = stringResource(id = R.string.media_delete)
    ) {
        viewModel.deleteTag(currentTag!!)
    }

    TagEditingPageImpl(
        mediaName = item.displayName,
        mediaPath =
            item.immichUrl ?: item.absolutePath.replaceFirst(item.absolutePath.toBasePath(), ""),
        mediaType = item.mimeType,
        mediaFavourited = item.favourited,
        mediaUri = item.uri,
        navController = LocalNavController.current,
        appliedTags = appliedTags,
        allTags = allTags,
        modifier = modifier,
        onTagClick = { tag ->
            viewModel.toggleTag(tag)
        },
        onTagRemove = { tag ->
            currentTag = tag
            showDialog.value = true
        },
        addTag = { name ->
            viewModel.insertTag(
                Tag(
                    name = name,
                    description = "",
                    color = Color.random()
                )
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagEditingPageImpl(
    mediaName: String,
    mediaPath: String,
    mediaType: String,
    mediaFavourited: Boolean,
    mediaUri: String,
    navController: NavController,
    appliedTags: List<Tag>,
    allTags: List<Tag>,
    modifier: Modifier = Modifier,
    onTagClick: (tag: Tag) -> Unit,
    onTagRemove: (tag: Tag) -> Unit,
    addTag: (name: String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.tags),
                        fontSize = TextStylingConstants.LARGE_TEXT_SIZE.sp
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
                            contentDescription = stringResource(id = R.string.close)
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                alignment = Alignment.Top
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TagMediaInfo(
                mediaUri = mediaUri,
                mediaName = mediaName,
                mediaPath = mediaPath,
                mediaType = mediaType,
                mediaFavourited = mediaFavourited
            )

            var tagName by remember { mutableStateOf("") }
            TagEditingPageTextField(
                value = tagName,
                onValueChange = {
                    tagName = it
                },
                exists = { name ->
                    allTags.any { it.name == name }
                },
                addTag = addTag
            )

            Text(
                text = stringResource(id = R.string.tags_applied),
                fontSize = TextStylingConstants.LARGE_TEXT_SIZE.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(all = 12.dp),
                verticalArrangement = Arrangement.spacedBy(
                    space = 4.dp,
                    alignment = if (appliedTags.isEmpty()) Alignment.CenterVertically else Alignment.Top
                ),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (appliedTags.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.tags_none_applied),
                        fontSize = TextStylingConstants.MEDIUM_TEXT_SIZE.sp
                    )
                } else {
                    appliedTags.sortedBy { it.name }.forEach {
                        TagItem(
                            tag = it,
                            selected = true,
                            showDeleteIcon = false,
                            onClick = {
                                onTagClick(it)
                            },
                            onRemove = {
                                onTagRemove(it)
                            }
                        )
                    }
                }
            }

            Text(
                text = stringResource(id = R.string.tags_available),
                fontSize = TextStylingConstants.LARGE_TEXT_SIZE.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(all = 12.dp),
                verticalArrangement = Arrangement.spacedBy(
                    space = 8.dp,
                    alignment = if (allTags.isEmpty()) Alignment.CenterVertically else Alignment.Top
                ),
                horizontalArrangement = Arrangement.spacedBy(
                    space = 8.dp,
                    alignment = Alignment.CenterHorizontally
                )
            ) {
                val tags = (allTags - appliedTags.toSet()).sortedBy { it.name }

                if (tags.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.tags_add_some),
                        fontSize = TextStylingConstants.MEDIUM_TEXT_SIZE.sp
                    )
                } else {
                    tags.forEach {
                        TagItem(
                            tag = it,
                            selected = false,
                            showDeleteIcon = true,
                            onClick = {
                                onTagClick(it)
                            },
                            onRemove = {
                                onTagRemove(it)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun TagEditingPagePreview() {
    TagEditingPageImpl(
        mediaName = "Kittyyyy.png",
        mediaPath = "DCIM/Camera",
        mediaType = "image/png",
        mediaFavourited = true,
        mediaUri = "",
        navController = rememberNavController(),
        appliedTags = (0..5).map {
            Tag(
                id = it,
                name = CharArray(8) {
                    CharRange('a', 'z').random()
                }.concatToString(),
                description = "",
                color = Color.random()
            )
        },
        allTags = (0..20).map {
            Tag(
                id = it,
                name = CharArray(8) {
                    CharRange('a', 'z').random()
                }.concatToString(),
                description = "",
                color = Color.random()
            )
        },
        onTagClick = {},
        onTagRemove = {},
        addTag = {}
    )
}