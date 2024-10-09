package com.kaii.photos.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.kaii.photos.R

@Composable
fun FolderDoesntExist() {
    Column (
        modifier = Modifier
            .fillMaxSize(1f)
            .background(CustomMaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text (
            text = "This folder doesn't exist!",
            fontSize = TextUnit(18f, TextUnitType.Sp),
            modifier = Modifier
                .wrapContentSize()
        )

        Spacer (modifier = Modifier.height(16.dp))

        Text (
            text = "you should probably remove it from the list",
            fontSize = TextUnit(14f, TextUnitType.Sp),
            modifier = Modifier
                .wrapContentSize()
        )
    }
}

@Composable
fun FolderIsEmpty(emptyText: String, emptyIconResId: Int = R.drawable.broken_image) {
    Column (
        modifier = Modifier
            .fillMaxSize(1f)
            .background(CustomMaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = emptyIconResId),
            contentDescription = emptyText,
            modifier = Modifier
                .size(48.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text (
            text = emptyText,
            fontSize = TextUnit(18f, TextUnitType.Sp),
            modifier = Modifier
                .wrapContentSize()
        )
    }
}