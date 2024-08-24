package com.kaii.photos.activities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Environment
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.kaii.photos.R
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import java.io.File
import java.io.FileFilter

const val TAG = "GRID_PHOTO_VIEW"

@OptIn(ExperimentalLayoutApi::class, ExperimentalGlideComposeApi::class)
@Composable
fun PhotoGridView() {
	// start loading photos for the love of god
	val dcimDir = File(
		Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toURI()
	)

	// var fileList = listOf(
	// 	"/storage/emulated/0/DCIM/IMG-20240724-WA0041.jpg","/storage/emulated/0/DCIM/IMG-20240724-WA0040.jpg","/storage/emulated/0/DCIM/IMG-20240724-WA0037.jpg","/storage/emulated/0/DCIM/IMG-20240724-WA0036.jpg","/storage/emulated/0/DCIM/IMG-20240724-WA0039.jpg","/storage/emulated/0/DCIM/IMG-20240724-WA0038.jpg","/storage/emulated/0/DCIM/IMG-20240724-WA0035.jpg","/storage/emulated/0/DCIM/IMG-20240725-WA0005.jpg","/storage/emulated/0/DCIM/IMG-20240724-WA0041.jpg","/storage/emulated/0/DCIM/IMG-20240724-WA0040.jpg","/storage/emulated/0/DCIM/IMG-20240724-WA0037.jpg","/storage/emulated/0/DCIM/IMG-20240724-WA0036.jpg","/storage/emulated/0/DCIM/IMG-20240724-WA0039.jpg","/storage/emulated/0/DCIM/IMG-20240724-WA0038.jpg","/storage/emulated/0/DCIM/IMG-20240724-WA0035.jpg","/storage/emulated/0/DCIM/IMG-20240725-WA0005.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240406_185051_825.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240406_185057_247.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240129_131857.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240104_125243.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240126_230200.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240120_123907.jpg","/storage/emulated/0/DCIM/Camera/IMG_20231230_221030.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240127_195847.jpg","/storage/emulated/0/DCIM/Camera/IMG_20231230_220539.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240110_125415.jpg","/storage/emulated/0/DCIM/Camera/IMG_20231229_085610.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240202_000907.jpg","/storage/emulated/0/DCIM/Camera/IMG_20231230_221045.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240128_160941.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240127_195844.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240104_125231.jpg","/storage/emulated/0/DCIM/Camera/IMG_20231230_221038.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240105_130126.jpg","/storage/emulated/0/DCIM/Camera/IMG_20231230_221025.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240105_130211.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240122_150219.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240101_160430.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240201_081139.jpg","/storage/emulated/0/DCIM/Camera/IMG_20231230_220536.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240201_081207.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240104_125307.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240105_130209.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240114_190353.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240101_160404.jpg","/storage/emulated/0/DCIM/Camera/IMG_20231230_221048.jpg","/storage/emulated/0/DCIM/Camera/IMG_20231231_000008.jpg","/storage/emulated/0/DCIM/Camera/IMG_20231230_221021.jpg","/storage/emulated/0/DCIM/Camera/IMG_20231231_000030.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240203_113312.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240110_125337.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240120_123912.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240201_081159.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240202_000900.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240129_131846.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240203_113256.jpg","/storage/emulated/0/DCIM/Camera/IMG_20231231_000020.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240201_081148.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240105_124916.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240202_000915.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240202_000909.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240104_125320.jpg","/storage/emulated/0/DCIM/Camera/IMG_20231230_220516.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240104_125239.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240131_163050.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240131_113538.jpg","/storage/emulated/0/DCIM/Camera/IMG_20240101_160113.jpg"
	// )

	var fileList = dcimDir?.listFiles()?.toMutableList()
	dcimDir?.walkTopDown()?.forEach {
		if (acceptFileAsImage(it)) {
			fileList?.add(it)
		}
	}

	println(fileList)
	val totalNumberOfPics = fileList?.size ?: 0
	val leftOver = totalNumberOfPics % 3

	val lastRowLastPicIndex = totalNumberOfPics - 1
	val secondToLastRowLastPicIndex = lastRowLastPicIndex - leftOver

	val secondToLastPicIndex = lastRowLastPicIndex - 1
	val lastRowFirstPicIndex = lastRowLastPicIndex - 2 - leftOver
	
	val firstRowFirstPicIndex = 0
	val firstRowLastPicIndex = firstRowFirstPicIndex + 2

	println("leftOver $leftOver\nlastRowLastPic $lastRowLastPicIndex\nlastRowFirstPic $lastRowFirstPicIndex\nfirstRowFirstPic $firstRowFirstPicIndex\nfirstRowLastPic $firstRowLastPicIndex")

	val rounding = 16.dp
	val context = LocalContext.current

	var size by remember { mutableStateOf(112.dp) }
	val localDensity = LocalDensity.current
	
	LazyColumn (
        modifier = Modifier
           .fillMaxSize(1f)
           .onGloballyPositioned { coordinates ->
               size = with(localDensity) { coordinates.size.height.toDp() / 5.088f }
           }
	) {
		item {
		    FlowRow (
		        horizontalArrangement = Arrangement.Start,
		        verticalArrangement = Arrangement.Center,
		        maxItemsInEachRow = 3,
		        modifier = Modifier
		            .fillMaxSize(1f)
		    ) {
				
		        for (i in 0..<totalNumberOfPics) {
					val shape = if (totalNumberOfPics < 6) RoundedCornerShape(rounding) else when (i) {
						lastRowLastPicIndex -> {
							if (leftOver == 1) {
								RoundedCornerShape(0.dp, 0.dp, rounding, rounding)
							} else {
								RoundedCornerShape(0.dp, 0.dp, rounding, 0.dp)
							}
						}
						lastRowFirstPicIndex -> {
							if (leftOver >= 1) {
								RoundedCornerShape(0.dp, 0.dp, 0.dp, 0.dp)
							} else {
								RoundedCornerShape(0.dp, 0.dp, 0.dp, rounding)
							}
						}
						firstRowFirstPicIndex -> {
							RoundedCornerShape(rounding, 0.dp, 0.dp, 0.dp)
						}
						firstRowLastPicIndex -> {
							RoundedCornerShape(0.dp, rounding, 0.dp, 0.dp)
						}
						secondToLastRowLastPicIndex -> {
							if (secondToLastRowLastPicIndex != lastRowLastPicIndex) {
								RoundedCornerShape(0.dp, 0.dp, rounding, 0.dp)
							} else {
								RoundedCornerShape(0.dp, 0.dp, 0.dp, 0.dp)	
							}
						}
						secondToLastPicIndex -> {
							if (leftOver == 2) {
								RoundedCornerShape(0.dp, 0.dp, 0.dp, rounding)
							} else {
								RoundedCornerShape(0.dp, 0.dp, 0.dp, 0.dp)	
							}
						}
						else -> {
							RoundedCornerShape(0.dp, 0.dp, 0.dp, 0.dp)
						}
					}

					Column (
	                    modifier = Modifier
							.fillMaxWidth(1f/3f)
							.aspectRatio(1f)
	                        .padding(4.dp)
	                        .clip(shape),
	                    verticalArrangement = Arrangement.SpaceEvenly,
	                    horizontalAlignment = Alignment.CenterHorizontally
	                ) {		            
		            	GlideImage(
		            		model = fileList?.get(i),
		            		contentDescription = "loaded image from internal storage",
		            		contentScale = ContentScale.Fit,
		            		modifier = Modifier.fillParentMaxSize(1f)
		            	)
		            }
		        }
		    }
		}
	}
}

fun acceptFileAsImage(file: File) : Boolean {
	if (!file.name.startsWith(".") && file.absolutePath.endsWith(".png"))  return true
	else if (!file.name.startsWith(".") && file.absolutePath.endsWith(".jpg"))  return true
	else if (!file.name.startsWith(".") && file.absolutePath.endsWith(".jpeg")) return true
	else if (!file.name.startsWith(".") && file.absolutePath.endsWith(".webp")) return true
	return false
}
