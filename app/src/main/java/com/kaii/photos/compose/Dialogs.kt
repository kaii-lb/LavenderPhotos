package com.kaii.photos.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun DialogClickableItem(text: String, iconResId: Int, position: DialogItemPosition, action: () -> Unit) {
	val buttonHeight = 40.dp

	val shape: RoundedCornerShape
	var spacerHeight = 0.dp
	
	when(position) {
		DialogItemPosition.Top -> {
			shape = RoundedCornerShape(16.dp, 16.dp, 0.dp, 0.dp)
			spacerHeight = 2.dp
		}
		DialogItemPosition.Middle -> {
			shape = RoundedCornerShape(0.dp, 0.dp, 0.dp, 0.dp)
			spacerHeight = 2.dp
		}
		DialogItemPosition.Bottom -> {
			shape = RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp)
			spacerHeight = 0.dp
		}				
	}
	
	Row (
		modifier = Modifier
			.fillMaxWidth(1f)
			.height(buttonHeight)
			.clip(shape)
			.background(CustomMaterialTheme.colorScheme.surfaceVariant)
			.wrapContentHeight(align = Alignment.CenterVertically)
			.padding(8.dp)
			.clickable {
				action()
			},
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.Start
	) {
		Icon (
			painter = painterResource(id = iconResId),
            contentDescription = "icon describing: $text",
			modifier = Modifier
				.size(20.dp)
		)

		Spacer (modifier = Modifier.width(12.dp))
		
		Text (
			text = text,
			fontSize = TextUnit(16f, TextUnitType.Sp),
			textAlign = TextAlign.Start,
		)
	}

	Spacer (
		modifier = Modifier
			.height(spacerHeight)
			.background(CustomMaterialTheme.colorScheme.surface)
	)
}

enum class DialogItemPosition {
	Top, 
	Middle,
	Bottom
}
