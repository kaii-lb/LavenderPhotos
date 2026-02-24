package com.kaii.photos.compose.widgets

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaii.photos.R
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.helpers.toPascalCase
import com.kaii.photos.repositories.SearchMode
import kotlinx.coroutines.delay
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchTextField(
    query: String,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onQueryChange: (text: String) -> Unit,
    onSearchModeChange: (mode: SearchMode) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var searchQuery by retain { mutableStateOf("") }
    var searchMode by retain { mutableStateOf(SearchMode.Name) }

    val updatedQuery by rememberUpdatedState(query)
    LaunchedEffect(updatedQuery) {
        delay(1000)
        searchQuery = updatedQuery
        onQueryChange(searchQuery)
    }

    val resources = LocalResources.current
    val placeholdersList = remember(searchMode) {
        val month = Month.entries.random().name.toPascalCase()
        val day = DayOfWeek.entries.random().name.toPascalCase()
        val date = (1..31).random().toString()
        val year = (2016..2024).random().toString()

        when (searchMode) {
            SearchMode.Name -> listOf(
                resources.getString(R.string.search_photo_name),
                resources.getString(R.string.search_photo_date)
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
                text = placeholdersList.random(),
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
        visualTransformation = visualTransformation,
        shape = CircleShape,
        modifier = modifier
    )
}