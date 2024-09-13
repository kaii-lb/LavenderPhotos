package com.kaii.photos.compose

import android.annotation.SuppressLint
import android.view.Window
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.kaii.photos.MainActivity
import com.kaii.photos.R
import com.kaii.photos.helpers.single_image_functions.ImageFunctions
import com.kaii.photos.helpers.single_image_functions.operateOnImage
import com.kaii.photos.mediastore.MediaStoreData

@OptIn(ExperimentalGlideComposeApi::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SingleHiddenPhotoView(navController: NavHostController, window: Window) {
    val mainViewModel = MainActivity.mainViewModel

    val mediaItem = mainViewModel.selectedMediaData.collectAsState(initial = null).value ?: return

    var systemBarsShown by remember { mutableStateOf(true) }
    var appBarAlpha by remember { mutableFloatStateOf(1f) }

    Scaffold (
        topBar =  { TopBar(navController, mediaItem, appBarAlpha) },
        bottomBar = { BottomBar(navController, appBarAlpha, mediaItem) },
        containerColor = CustomMaterialTheme.colorScheme.background,
        contentColor = CustomMaterialTheme.colorScheme.onBackground
    ) { _ ->
        Column (
            modifier = Modifier
                .padding(0.dp)
                .background(CustomMaterialTheme.colorScheme.background)
                .fillMaxSize(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())

            GlideImage(
                model = mediaItem.uri.path,
                contentDescription = "selected image",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize(1f)
                    .clickable {
                        if (systemBarsShown) {
                            windowInsetsController.apply {
                                hide(WindowInsetsCompat.Type.systemBars())
                                systemBarsBehavior =
                                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                            }
                            window.setDecorFitsSystemWindows(false)
                            systemBarsShown = false
                            appBarAlpha = 0f
                        } else {
                            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                            window.setDecorFitsSystemWindows(false)
                            systemBarsShown = true
                            appBarAlpha = 1f
                        }
                    },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(navController: NavHostController, mediaItem: MediaStoreData?, alpha: Float) {
    TopAppBar(
        modifier = Modifier.alpha(alpha),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = CustomMaterialTheme.colorScheme.surfaceContainer
        ),
        navigationIcon = {
            IconButton(
                onClick = { navController.popBackStack() },
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.back_arrow),
                    contentDescription = "Go back to previous page",
                    tint = CustomMaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(24.dp)
                )
            }
        },
        title = {
            val mediaTitle = if (mediaItem != null) {
                mediaItem.displayName ?: mediaItem.type.name
            } else {
                "Media"
            }

            Spacer (modifier = Modifier.width(8.dp))

            Text(
                text = mediaTitle,
                fontSize = TextUnit(18f, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .width(160.dp)
            )
        },
        actions = {
            IconButton(
                onClick = { /* TODO */ },
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.more_options),
                    contentDescription = "show more options",
                    tint = CustomMaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(24.dp)
                )
            }
        }
    )
}

@Composable
private fun BottomBar(navController: NavHostController, alpha: Float, item: MediaStoreData) {
    val context = LocalContext.current

    BottomAppBar(
        containerColor = CustomMaterialTheme.colorScheme.surfaceContainer,
        contentColor = CustomMaterialTheme.colorScheme.onBackground,
        contentPadding = PaddingValues(0.dp),
        actions = {
            Row (
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .padding(12.dp, 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = {
                        operateOnImage(item.absolutePath, item.id, ImageFunctions.MoveOutOfLockedFolder, context)
                        navController.popBackStack()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Row (
                        modifier = Modifier.fillMaxWidth(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Icon (
                            painter = painterResource(id = R.drawable.favorite),
                            contentDescription = "Restore Image Button",
                            tint = CustomMaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(22.dp)
                                .padding(0.dp, 2.dp, 0.dp, 0.dp)
                        )

                        Spacer (
                            modifier = Modifier
                                .width(8.dp)
                        )

                        Text(
                            text = "Restore",
                            fontSize = TextUnit(16f, TextUnitType.Sp),
                            textAlign = TextAlign.Center,
                            color = CustomMaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .fillMaxWidth(1f)
                        )
                    }
                }

                Spacer (modifier = Modifier.width(8.dp))

                var showDialog by remember { mutableStateOf(false) }

                if (showDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            showDialog = false
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showDialog = false
                                    operateOnImage(item.absolutePath, item.id, ImageFunctions.PermaDeleteImage, context)
                                    navController.popBackStack()
                                }
                            ) {
                                Text(
                                    text = "Delete",
                                    fontSize = TextUnit(14f, TextUnitType.Sp)
                                )
                            }
                        },
                        title = {
                            Text(
                                text = "Permanently delete this ${item.type.name}?",
                                fontSize = TextUnit(16f, TextUnitType.Sp)
                            )
                        },
                        dismissButton = {
                            Button(
                                onClick = {
                                    showDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CustomMaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = CustomMaterialTheme.colorScheme.onTertiaryContainer
                                )
                            ) {
                                Text(
                                    text = "Cancel",
                                    fontSize = TextUnit(14f, TextUnitType.Sp)
                                )
                            }
                        },
                        shape = RoundedCornerShape(32.dp)
                    )
                }

                OutlinedButton(
                    onClick = {
                        showDialog = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Row (
                        modifier = Modifier.fillMaxWidth(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Icon (
                            painter = painterResource(id = R.drawable.trash),
                            contentDescription = "Permanently Delete Image Button",
                            tint = CustomMaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(22.dp)
                                .padding(0.dp, 2.dp, 0.dp, 0.dp)
                        )

                        Spacer (
                            modifier = Modifier
                                .width(8.dp)
                        )

                        Text(
                            text = "Delete",
                            fontSize = TextUnit(16f, TextUnitType.Sp),
                            textAlign = TextAlign.Center,
                            color = CustomMaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .fillMaxWidth(1f)
                        )
                    }
                }
            }
        },
        modifier = Modifier.alpha(alpha)
    )
}
