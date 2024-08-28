package com.kaii.photos.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp

@Composable
fun AlbumGridView() {
    Column (
        modifier = Modifier
            .fillMaxSize(1f)
            .padding(8.dp, 0.dp)
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize(1f),
            horizontalArrangement = Arrangement.Start,
            verticalArrangement = Arrangement.Top
        ) {
        	item (
        		span = { GridItemSpan(maxLineSpan) }
        	) {
        		CategoryList()
        	}
        	
            items(
                count = 5
            ) { index ->
                AlbumGridItem(
                	if (index == 0) "Camera" else if (index == 1) "Downloads" else if (index == 2) "Whatsapp Images"
                		else if (index == 3) "Pins" else "Pictures"
                )
            }
        }
    }
}

@Composable
fun AlbumGridItem(title: String) {
	Column (
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth(1f)
            .padding(6.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {	
	    Column (
	        modifier = Modifier
	            .fillMaxSize(1f)
	            .padding(8.dp, 8.dp, 8.dp, 4.dp)
	            .clip(RoundedCornerShape(16.dp)),
	        verticalArrangement = Arrangement.SpaceEvenly,
	        horizontalAlignment = Alignment.CenterHorizontally
	    ) {
	        Image(
	            painter = painterResource(id = com.kaii.photos.R.drawable.cat_picture),
	            contentDescription = "Album Cover Image",
	            contentScale = ContentScale.Crop,
	            modifier = Modifier
	                .aspectRatio(1f)
	                .clip(RoundedCornerShape(16.dp))
	        )

	        Text(
	            text = " $title",
	            fontSize = TextUnit(14f, TextUnitType.Sp),
	            textAlign = TextAlign.Start,
	            color = MaterialTheme.colorScheme.onSurface,
	            maxLines = 1,
	            modifier = Modifier
	            	.fillMaxWidth(1f)
	            	.padding(2.dp)
	        )
	    }
    }
}

@Composable
fun CategoryList() {
	Row (
        modifier = Modifier
            .fillMaxWidth(1f)
            .wrapContentHeight()
            .padding(8.dp)
            .background(MaterialTheme.colorScheme.background),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            onClick = { /*TODO*/ },
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
        ) {
            Text(
            	text = "Favorites",
         	fontSize = TextUnit(18f, TextUnitType.Sp),
         	textAlign = TextAlign.Start,
         	color = MaterialTheme.colorScheme.onPrimary,           	
         	modifier = Modifier
         		.fillMaxWidth(1f)
           	)
        }

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = { /*TODO*/ },
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
        ) {
            Text(
            	text = "Trash",
	         	fontSize = TextUnit(18f, TextUnitType.Sp),                
	          	textAlign = TextAlign.Start,
	         	color = MaterialTheme.colorScheme.onPrimary,
	         	modifier = Modifier
	         		.fillMaxWidth(1f)
           	)
        }
    }	
}
