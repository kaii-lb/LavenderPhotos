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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.TagEditingPageTextField
import com.kaii.photos.compose.widgets.TagItem
import com.kaii.photos.compose.widgets.TagMediaInfo
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.database.entities.Tag
import com.kaii.photos.helpers.TextStylingConstants
import kotlin.random.Random

@Preview
@Composable
private fun TagEditingPagePreview() {
    TagEditingPage(
        media =
            MediaStoreData.dummyItem.copy(
                displayName = "Kittyyyy.png",
                mimeType = "image/png",
                parentPath = "/storage/emulated/0/DCIM/Camera",
                favourited = true
            ),
        navController = rememberNavController(),
        appliedTags = (0..5).map {
            Tag(
                id = it,
                name = CharArray(8) {
                    CharRange('a', 'z').random()
                }.concatToString(),
                description = "",
                color = Color(red = Random.nextFloat(), green = Random.nextFloat(), blue = Random.nextFloat())
            )
        },
        allTags = (0..20).map {
            Tag(
                id = it,
                name = CharArray(8) {
                    CharRange('a', 'z').random()
                }.concatToString(),
                description = "",
                color = Color(red = Random.nextFloat(), green = Random.nextFloat(), blue = Random.nextFloat())
            )
        },
        applyAction = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagEditingPage(
    media: MediaStoreData,
    navController: NavController,
    appliedTags: List<Tag>,
    allTags: List<Tag>,
    modifier: Modifier = Modifier,
    applyAction: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tags",
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
                },
                actions = {
                    Button(
                        onClick = applyAction
                    ) {
                        Text(
                            text = stringResource(id = R.string.apply)
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
                media = media
            )

            TagEditingPageTextField(
                value = "",
                onValueChange = {

                },
                exists = { name ->
                    allTags.any { it.name == name }
                }
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
                            onClick = {},
                            onRemove = {}
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
                            onClick = {},
                            onRemove = {}
                        )
                    }
                }
            }
        }
    }
}