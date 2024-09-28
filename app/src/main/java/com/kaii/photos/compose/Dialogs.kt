package com.kaii.photos.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.kaii.photos.helpers.RowPosition

@Composable
fun DialogClickableItem(text: String, iconResId: Int, position: RowPosition, action: () -> Unit) {
	val buttonHeight = 40.dp

	val shape: RoundedCornerShape
	var spacerHeight = 0.dp
	
	when(position) {
		RowPosition.Top -> {
			shape = RoundedCornerShape(16.dp, 16.dp, 0.dp, 0.dp)
			spacerHeight = 2.dp
		}
		RowPosition.Middle -> {
			shape = RoundedCornerShape(0.dp)
			spacerHeight = 2.dp
		}
		RowPosition.Bottom -> {
			shape = RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp)
			spacerHeight = 0.dp
		}
		RowPosition.Single -> {
			shape = RoundedCornerShape(16.dp)
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
			.clickable {
				action()
			}
			.padding(8.dp),
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
