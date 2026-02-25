package com.kaii.photos.compose.widgets

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaii.photos.R
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.helpers.toPascalCase
import com.kaii.photos.repositories.SearchMode
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month

@Preview
@Composable
private fun SearchTextFieldPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(top = 64.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        SearchTextField(
            onQueryChange = {},
            onSearchModeChange = {},
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchTextField(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onQueryChange: (text: String) -> Unit,
    onSearchModeChange: (mode: SearchMode) -> Unit
) {
    var searchQuery by retain { mutableStateOf("") }
    var searchMode by retain { mutableStateOf(SearchMode.Name) }
    var searchingForTags by remember { mutableStateOf(false) }

    BackHandler(
        enabled = searchQuery.isNotEmpty()
    ) {
        searchQuery = ""
    }

    val resources = LocalResources.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val placeholdersList = remember(searchMode) {
        val month = Month.entries.random().name.toPascalCase()
        val day = DayOfWeek.entries.random().name.toPascalCase()
        val date = (1..31).random().toString()
        val year = (2016..2024).random().toString()

        when (searchMode) {
            SearchMode.Name, SearchMode.Tag -> listOf(
                resources.getString(R.string.search_photo_name)
            )

            SearchMode.Date -> listOf(
                "$month $year",
                "$day $month $year",
                "$date $month $year",
                year, month, day
            )
        }
    }

    TextField(
        value = searchQuery,
        onValueChange = {
            searchQuery = it
            onQueryChange(it)
        },
        maxLines = 1,
        singleLine = true,
        placeholder = {
            Text(
                text =
                    if (searchingForTags) stringResource(id = R.string.search_photo_tag)
                    else placeholdersList.random(),
                fontSize = TextStylingConstants.MEDIUM_TEXT_SIZE.sp
            )
        },
        leadingIcon = {
            var expanded by remember { mutableStateOf(false) }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                },
                shape = RoundedCornerShape(24.dp),
                offset = DpOffset(x = 0.dp, y = 8.dp)
            ) {
                SearchMode.entries.forEachIndexed { index, mode ->
                    val backgroundColor by animateColorAsState(
                        targetValue =
                            if (searchMode == mode) MaterialTheme.colorScheme.primary
                            else MenuDefaults.itemColors().containerColor
                    )

                    Column(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(id = mode.nameId),
                                    fontSize = TextStylingConstants.SMALL_TEXT_SIZE.sp,
                                    color = MaterialTheme.colorScheme.contentColorFor(backgroundColor),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                )
                            },
                            onClick = {
                                searchMode = mode
                                onSearchModeChange(mode)
                                expanded = false
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(backgroundColor)
                        )

                        Spacer(
                            modifier = Modifier
                                .height(if (index == SearchMode.entries.size - 1) 0.dp else 4.dp)
                        )
                    }
                }
            }

            Button(
                onClick = {
                    expanded = true
                },
                modifier = Modifier
                    .padding(start = 9.dp, end = 4.dp)
            ) {
                Text(
                    text = stringResource(id = searchMode.nameId),
                    fontSize = TextStylingConstants.SMALL_TEXT_SIZE.sp
                )
            }
        },
        suffix = {
            if (searchQuery.isNotEmpty()) {
                Icon(
                    painter = painterResource(id = R.drawable.close),
                    contentDescription = "Clear search query",
                    tint = contentColor,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            searchQuery = ""
                            onQueryChange("")
                        }
                )
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedTextColor = contentColor,
            unfocusedTextColor = contentColor,
            focusedPlaceholderColor = contentColor.copy(alpha = 0.4f),
            unfocusedPlaceholderColor = contentColor.copy(alpha = 0.4f),
            unfocusedIndicatorColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent
        ),
        keyboardOptions = KeyboardOptions(
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = {
                keyboardController?.hide()
            }
        ),
        shape = CircleShape,
        modifier = modifier
    )

    val keyboardVisible by rememberUpdatedState(WindowInsets.ime.getBottom(LocalDensity.current) > 0)
    AnimatedVisibility(
        visible = keyboardVisible && searchMode == SearchMode.Tag,
        enter = scaleIn(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(), transformOrigin = TransformOrigin(0.5f, 0f)),
        exit = fadeOut() + scaleOut(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(), transformOrigin = TransformOrigin(0.5f, 0f)),
        modifier = Modifier
            .offset(y = 64.dp)
    ) {
        TagDisplay(
            tags = emptyList(), // TODO
            selectedTags = emptyList(), // TODO
            searchingForTags = searchingForTags,
            searchQuery = searchQuery,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxWidth()
                .dropShadow(
                    shape = RoundedCornerShape(24.dp),
                    shadow = Shadow(
                        radius = 8.dp,
                        color = Color.Black.copy(alpha = 0.6f)
                    )
                ),
            onTagClick = {

            },
            onTagRemove = {

            },
            onToggleSearchingForTags = {
                searchingForTags = it
            }
        )
    }
}