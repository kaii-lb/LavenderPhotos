package com.kaii.photos.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.kaii.photos.R
import com.kaii.photos.helpers.CustomMaterialTheme
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.darkenColor

@Composable
fun PreferencesRow(
    title: String,
    summary: String,
    iconResID: Int,
    position: RowPosition,
    goesToOtherPage: Boolean = false,
    action: (() -> Unit)? = null
) {
    val spacerHeight: Dp
    val shape: RoundedCornerShape

    when (position) {
        RowPosition.Top -> {
            spacerHeight = 2.dp
            shape = RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp)
        }

        RowPosition.Middle -> {
            spacerHeight = 2.dp
            shape = RoundedCornerShape(0.dp)
        }

        RowPosition.Bottom -> {
            spacerHeight = 0.dp
            shape = RoundedCornerShape(0.dp, 0.dp, 24.dp, 24.dp)
        }

        RowPosition.Single -> {
            spacerHeight = 0.dp
            shape = RoundedCornerShape(24.dp)
        }
    }

    val clickable = if (action != null) {
        Modifier.clickable {
            action()
        }
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth(1f)
            .height(72.dp)
            .clip(shape)
            .wrapContentHeight(align = Alignment.CenterVertically)
            .background(CustomMaterialTheme.colorScheme.surfaceVariant)
            .then(clickable)
            .padding(16.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconResID),
            contentDescription = "an icon describing: $title",
            tint = CustomMaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .size(32.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier
                .height(72.dp)
                .weight(1f),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                fontSize = TextUnit(18f, TextUnitType.Sp),
                textAlign = TextAlign.Start,
                color = CustomMaterialTheme.colorScheme.onSurface
            )

            Text(
                text = summary,
                fontSize = TextUnit(14f, TextUnitType.Sp),
                textAlign = TextAlign.Start,
                color = darkenColor(CustomMaterialTheme.colorScheme.onSurface, 0.1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (goesToOtherPage) {
            Icon(
                painter = painterResource(id = R.drawable.other_page_indicator),
                contentDescription = "this preference row leads to another page",
                tint = CustomMaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .size(28.dp)
            )
        }
    }

    Spacer(
        modifier = Modifier
            .height(spacerHeight)
            .background(CustomMaterialTheme.colorScheme.background)
    )
}
