package com.kaii.photos.compose.widgets.tags

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.ConfirmationDialog
import com.kaii.photos.database.entities.Tag
import com.kaii.photos.helpers.editing.random

@Preview
@Composable
private fun MediaTagManagerPreview() {
    MediaTagManager(
        tags = (0..20).map {
            Tag(
                id = it,
                name = CharArray(8) {
                    CharRange('a', 'z').random()
                }.concatToString(),
                description = "",
                color = Color.random()
            )
        },
        selectedTags = (0..4).map {
            Tag(
                id = it,
                name = CharArray(8) {
                    CharRange('a', 'z').random()
                }.concatToString(),
                description = "",
                color = Color.random()
            )
        },
        onTagAdd = {},
        onTagClick = {},
        onTagDelete = {},
        onClose = {}
    )
}

@Composable
fun MediaTagManager(
    tags: List<Tag>,
    selectedTags: List<Tag>,
    modifier: Modifier = Modifier,
    onTagAdd: (name: String) -> Unit,
    onTagClick: (tag: Tag) -> Unit,
    onTagDelete: (tag: Tag) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClose
            ),
        verticalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = Alignment.Top
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var query by remember { mutableStateOf("") }
        var searchingForTags by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
        ) {
            TagEditingPageTextField(
                value = query,
                placeholder =
                    if (searchingForTags) stringResource(id = R.string.search_photo_tag)
                    else stringResource(id = R.string.tags_add),
                onValueChange = {
                    query = it
                },
                exists = { name ->
                    !searchingForTags && tags.any { it.name == name }
                },
                addTag = onTagAdd,
                modifier = Modifier
                    .weight(1f)
            )

            FilledIconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(56.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.close),
                    contentDescription = stringResource(id = R.string.close)
                )
            }
        }

        val showDeleteDialog = remember { mutableStateOf(false) }
        var currentTag by remember { mutableStateOf<Tag?>(null) }
        if (showDeleteDialog.value) {
            ConfirmationDialog(
                showDialog = showDeleteDialog,
                dialogTitle = stringResource(id = R.string.tags_delete, currentTag?.name ?: ""),
                confirmButtonLabel = stringResource(id = R.string.media_delete)
            ) {
                onTagDelete(currentTag!!)
            }
        }


        TagDisplay(
            tags = tags,
            selectedTags = selectedTags,
            searchingForTags = false,
            searchQuery = query,
            onTagClick = onTagClick,
            onTagRemove = {
                currentTag = it
                showDeleteDialog.value = true
            },
            onToggleSearchingForTags = {
                searchingForTags = it
            }
        )
    }
}