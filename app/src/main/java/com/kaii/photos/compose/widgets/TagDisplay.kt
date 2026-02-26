package com.kaii.photos.compose.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
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
import com.kaii.photos.R
import com.kaii.photos.compose.animatePlacement
import com.kaii.photos.database.entities.Tag
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.TextStylingConstants
import kotlin.random.Random

@Preview
@Composable
private fun TagDisplayPreview() {
    val tags = remember {
        mutableStateListOf<Tag>().apply {
            addAll(
                (0..20).map {
                    Tag(
                        id = it,
                        name = CharArray(8) {
                            CharRange('a', 'z').random()
                        }.concatToString(),
                        description = "",
                        color = Color(red = Random.nextFloat(), green = Random.nextFloat(), blue = Random.nextFloat())
                    )
                }
            )
        }
    }

    TagDisplay(
        tags = tags.toList(),
        selectedTags = tags.subList(0, 4),
        searchingForTags = false,
        searchQuery = "",
        onTagClick = {},
        onTagRemove = {
            tags.remove(it)
        },
        onToggleSearchingForTags = {}
    )
}

@Composable
fun TagDisplay(
    tags: List<Tag>,
    selectedTags: List<Tag>,
    searchingForTags: Boolean,
    searchQuery: String,
    modifier: Modifier = Modifier,
    onTagClick: (tag: Tag) -> Unit,
    onTagRemove: (tag: Tag) -> Unit,
    onToggleSearchingForTags: (checked: Boolean) -> Unit
) {
    Column(
        modifier = modifier
            .heightIn(max = 280.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = Alignment.Top
        ),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Tags",
                fontSize = TextStylingConstants.MEDIUM_TEXT_SIZE.sp,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            )

            FilledIconToggleButton(
                checked = searchingForTags,
                onCheckedChange = onToggleSearchingForTags,
                enabled = tags.isNotEmpty()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.search),
                    contentDescription = stringResource(id = R.string.search_photo_tag)
                )
            }
        }

        if (tags.isEmpty()) {
            Text(
                text = stringResource(id = R.string.search_no_tags_found),
                fontSize = TextStylingConstants.MEDIUM_TEXT_SIZE.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )
        } else {
            TagFlowRow(
                tags = tags,
                selectedTags = selectedTags,
                searchingForTags = searchingForTags,
                searchQuery = searchQuery,
                onTagClick = onTagClick,
                onTagRemove = onTagRemove
            )
        }
    }
}

@Composable
private fun TagFlowRow(
    tags: List<Tag>,
    selectedTags: List<Tag>,
    searchingForTags: Boolean,
    searchQuery: String,
    modifier: Modifier = Modifier,
    onTagClick: (tag: Tag) -> Unit,
    onTagRemove: (tag: Tag) -> Unit
) {
    FlowRow(
        modifier = modifier
            .verticalScroll(state = rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = Alignment.Top
        ),
        horizontalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = Alignment.CenterHorizontally
        )
    ) {
        tags.filter { it.name.contains(searchQuery) || !searchingForTags }
            .sortedBy { it.name }
            .sortedBy { !selectedTags.contains(it) }
            .apply {
                forEach { tag ->
                    key(tag.id) {
                        AnimatedVisibility(
                            visible = contains(tag),
                            enter = fadeIn() + scaleIn(animationSpec = AnimationConstants.defaultSpring()),
                            exit = fadeOut() + scaleOut(animationSpec = AnimationConstants.defaultSpring()),
                            modifier = Modifier
                                .animatePlacement()
                                .padding(4.dp)
                        ) {
                            TagItem(
                                tag = tag,
                                selected = selectedTags.contains(tag),
                                onClick = { onTagClick(tag) },
                                onRemove = { onTagRemove(tag) }
                            )
                        }
                    }
                }
            }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TagItem(
    tag: Tag,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val animatedBorder by animateDpAsState(
        targetValue = if (selected) 2.dp else 0.dp,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
    )

    Box(
        modifier = Modifier
            .then(
                if (animatedBorder > 0.dp) {
                    Modifier.border(
                        width = animatedBorder,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
                } else Modifier
            )
            .padding(animatedBorder * 2)
    ) {
        Row(
            modifier = modifier
                .wrapContentWidth()
                .clip(CircleShape)
                .background(tag.color)
                .clickable(onClick = onClick)
                .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = tag.name,
                fontSize = TextStylingConstants.SMALL_TEXT_SIZE.sp
            )

            Spacer(modifier = Modifier.width(4.dp))

            Icon(
                painter = painterResource(id = R.drawable.close),
                contentDescription = stringResource(id = R.string.media_delete),
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onRemove)
                    .padding(4.dp)
            )
        }
    }
}