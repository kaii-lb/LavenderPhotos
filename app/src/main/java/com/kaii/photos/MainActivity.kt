package com.kaii.photos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.MemoryCategory
import com.kaii.photos.compose.AlbumGridItem
import com.kaii.photos.compose.AlbumGridView
import com.kaii.photos.compose.PhotoGrid
import com.kaii.photos.helpers.ComposeViewType
import com.kaii.photos.ui.theme.PhotosTheme

class MainActivity : ComponentActivity() {

    companion object {
    	private var currentView = ComposeViewType.PhotoGridView
        private const val REQUEST_READ_STORAGE = 0
        private val PERMISSIONS_REQUEST =
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Glide.get(this).setMemoryCategory(MemoryCategory.HIGH)
        if (PERMISSIONS_REQUEST.any {
                ContextCompat.checkSelfPermission(
                    this,
                    it
                ) != PackageManager.PERMISSION_GRANTED
            }) {
            requestStoragePermission()
        } else {
            setContentForActivity(ComposeViewType.PhotoGridView)
        }
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this, PERMISSIONS_REQUEST, REQUEST_READ_STORAGE
        )
    }

    private fun setContentForActivity(view: ComposeViewType) {
        setContent {
            PhotosTheme {
				enableEdgeToEdge(
					navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.surfaceContainer.toArgb())
				)        
                Content(view)
                currentView = view
            }
        }
    }

    @Composable
    private fun Content(view: ComposeViewType) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize(1f),
            topBar = {
                TopBar()
            },
            bottomBar = { BottomBar() }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(0.dp, padding.calculateTopPadding(), 0.dp, padding.calculateBottomPadding())
            ) {
                Column(
                    modifier = Modifier
                        .padding(0.dp)
                ) {
                    when (view) {
                        ComposeViewType.PhotoGridView -> PhotoGrid()
                        ComposeViewType.AlbumGridView -> AlbumGridView()
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun TopBar() {
        TopAppBar(
            title = {
                Row {
                    Text(
                        text = "Lavender ",
                        fontWeight = FontWeight.Bold,
                        fontSize = TextUnit(22f, TextUnitType.Sp)
                    )
                    Text(
                        text = "Photos",
                        fontWeight = FontWeight.Normal,
                        fontSize = TextUnit(22f, TextUnitType.Sp)
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = { /*TODO*/ },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.settings),
                        contentDescription = "Settings Button",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(),
        )
    }

    @Preview
    @Composable
    private fun BottomBar() {
        BottomAppBar(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            contentPadding = PaddingValues(0.dp),
        ) {
            val buttonHeight = 56.dp
            val buttonWidth = 64.dp
            val iconSize = 24.dp
            val textSize = 14f

            Row(
                modifier = Modifier
                    .fillMaxSize(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // photo grid button
                Box(
                    modifier = Modifier
                        .width(buttonWidth)
                        .height(buttonHeight)
                        .clickable {
                            if (currentView != ComposeViewType.PhotoGridView) setContent {
                                setContentForActivity(ComposeViewType.PhotoGridView)
                            }
                        },
                ) {
                    Row(
                        modifier = Modifier
                            .height(iconSize + 8.dp)
                            .width(iconSize * 2.25f)
                            .clip(RoundedCornerShape(1000.dp))
                            .align(Alignment.TopCenter)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.photogrid_filled),
                            contentDescription = "button",
                            modifier = Modifier
                                .size(iconSize)
                        )

                    }
                    Text(
                        text = "Photos",
                        fontSize = TextUnit(textSize, TextUnitType.Sp),
                        modifier = Modifier
                            .wrapContentSize()
                            .align(Alignment.BottomCenter)
                    )
                }

                // locked folder button
                Box(
                    modifier = Modifier
                        .width(buttonWidth)
                        .height(buttonHeight),
                ) {
                    Row(
                        modifier = Modifier
                            .height(iconSize + 8.dp)
                            .width(iconSize * 2.25f)
                            .clip(RoundedCornerShape(1000.dp))
                            .align(Alignment.TopCenter),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
	                    Icon(
	                        painter = painterResource(id = R.drawable.locked_folder),
	                        contentDescription = "button",
	                        modifier = Modifier
	                            .size(iconSize)
	                    )
                    }                

                    Text(
                        text = "Secure",
                        fontSize = TextUnit(textSize, TextUnitType.Sp),
                        modifier = Modifier
                            .wrapContentSize()
                            .align(Alignment.BottomCenter)
                    )
                }

                // album grid button
                Box(
                    modifier = Modifier
                        .width(buttonWidth)
                        .height(buttonHeight)
                        .clickable {
                            if (currentView != ComposeViewType.AlbumGridView) setContent {
                                setContentForActivity(ComposeViewType.AlbumGridView)
                            }
                        },
                ) {
                    Row(
                        modifier = Modifier
                            .height(iconSize + 8.dp)
                            .width(iconSize * 2.25f)
                            .clip(RoundedCornerShape(1000.dp))
                            .align(Alignment.TopCenter),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
	                    Icon(
	                        painter = painterResource(id = R.drawable.albums),
	                        contentDescription = "button",
	                        modifier = Modifier
	                            .size(iconSize)
	                    )
                    }                

                    Text(
                        text = "Albums",
                        fontSize = TextUnit(textSize, TextUnitType.Sp),
                        modifier = Modifier
                            .wrapContentSize()
                            .align(Alignment.BottomCenter)
                    )
                }

                // search view button
                Box(
                    modifier = Modifier
                        .width(buttonWidth)
                        .height(buttonHeight),
                ) {
                    Row(
                        modifier = Modifier
                            .height(iconSize + 8.dp)
                            .width(iconSize * 2.25f)
                            .clip(RoundedCornerShape(1000.dp))
                            .align(Alignment.TopCenter),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
	                    Icon(
	                        painter = painterResource(id = R.drawable.search),
	                        contentDescription = "button",
	                        modifier = Modifier
	                            .size(iconSize)
	                    )
                    }                

                    Text(
                        text = "Search",
                        fontSize = TextUnit(textSize, TextUnitType.Sp),
                        modifier = Modifier
                            .wrapContentSize()
                            .align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_READ_STORAGE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setContentForActivity(ComposeViewType.PhotoGridView)
                } else {
                    Toast.makeText(this, "Storage permission is required", Toast.LENGTH_LONG).show()
                    requestStoragePermission()
                }
            }
        }
    }
}
