package com.kaii.photos.compose

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.provider.MediaStore
import android.os.Build
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.registerForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.kaii.photos.R
import com.kaii.photos.MainActivity.Companion.mainViewModel
import com.kaii.photos.helpers.CustomMaterialTheme
import com.kaii.photos.helpers.RowPosition
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

@Composable
fun PermissionHandler(
	continueToApp: MutableState<Boolean>
) {
    Column (
        modifier = Modifier
            .fillMaxSize()
            .background(CustomMaterialTheme.colorScheme.background)
            .padding(16.dp, 32.dp, 16.dp, 8.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val showDialog = remember { mutableStateOf(false) }
		val context = LocalContext.current

        PermissionDeniedDialog(showDialog = showDialog)

		Text (
			text = "Permissions",
			fontSize = TextUnit(22f, TextUnitType.Sp),
			fontWeight = FontWeight.Bold,
			color = CustomMaterialTheme.colorScheme.onBackground
		)

		Spacer (modifier = Modifier.height(32.dp))

		LazyColumn (
			verticalArrangement = Arrangement.spacedBy(
			    space = 4.dp,
			    alignment = Alignment.Top
			),
			horizontalAlignment = Alignment.CenterHorizontally,
			modifier = Modifier
			    .weight(1f)
		) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				item {
					// we can ignore READ_MEDIA_VIDEO since requesting both shows a single dialog
		            val readMediaImageLauncher = rememberLauncherForActivityResult(
		                contract = ActivityResultContracts.RequestPermission()
		            ) { granted ->
		                mainViewModel.onPermissionResult(
		                    permission = Manifest.permission.READ_MEDIA_IMAGES,
		                    isGranted = granted
		                )

		                mainViewModel.permissionQueue.forEach {println("PERMISSION DENIED $it")}

		                showDialog.value = !granted
		            }

		            PermissionButton(
		                name = "Read Media",
		                description = "Discover photos and videos on the device, this is a necessary permission.",
		                position = RowPosition.Top,
		                granted = !mainViewModel.permissionQueue.contains(Manifest.permission.READ_MEDIA_IMAGES)
		            ) {
		                readMediaImageLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
		            }
				}
	        } else {
	            item {
		            val readExternalStorageLauncher = rememberLauncherForActivityResult(
		                contract = ActivityResultContracts.RequestPermission()
		            ) { granted ->
		                mainViewModel.onPermissionResult(
		                    permission = Manifest.permission.READ_EXTERNAL_STORAGE,
		                    isGranted = granted
		                )

		                showDialog.value = !granted
		            }

		            PermissionButton(
		                name = "Read External Storage",
		                description = "Discover photos and videos on the device",
		                position = RowPosition.Top,
		                granted = !mainViewModel.permissionQueue.contains(Manifest.permission.READ_EXTERNAL_STORAGE)
		            ) {
		                readExternalStorageLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
		            }
	            }
	        }

	        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				item {
                    val manageMediaLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { _ ->
	                    val granted = MediaStore.canManageMedia(context)

	                    mainViewModel.onPermissionResult(
	                        permission = Manifest.permission.MANAGE_MEDIA,
	                        isGranted = granted
	                    )

	                    showDialog.value = !granted
		            }

		            PermissionButton(
		                name = "Manage Media",
		                description = "Set this app as a media manager, for better and faster trash/delete functionality",
		                position = RowPosition.Middle,
		                granted = !mainViewModel.permissionQueue.contains(Manifest.permission.MANAGE_MEDIA)
		            ) {
		                val intent = Intent(android.provider.Settings.ACTION_REQUEST_MANAGE_MEDIA)
		                manageMediaLauncher.launch(intent)
		            }
		        }
			}

			item {
            	val manageExternalStorageLauncher = rememberLauncherForActivityResult(
					contract = ActivityResultContracts.StartActivityForResult()
                ) { _ ->
                    val granted = Environment.isExternalStorageManager()

		            mainViewModel.onPermissionResult(
		                permission = Manifest.permission.MANAGE_EXTERNAL_STORAGE,
		                isGranted = granted
		            )

                    showDialog.value = !granted
	            }
		        PermissionButton(
		            name = "Manage All Files",
		            description = "Manage all files on device, used to trash, delete and edit media",
		            position = RowPosition.Bottom,
		            granted = !mainViewModel.permissionQueue.contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
		        ) {
		            val intent = Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
		            manageExternalStorageLauncher.launch(intent)
		        }
			}
		}



        Box (
            modifier = Modifier
                .fillMaxWidth(1f)
                .height(64.dp)
        ) {
            FilledTonalButton(
                onClick = {
                	(context as Activity).finish()
                },
                modifier = Modifier
                	.align(Alignment.CenterStart)
            ) {
                Text(text = "Exit")
            }

            Button(
                onClick = {
                    continueToApp.value = true
                },
                enabled = mainViewModel.checkCanPass(),
                modifier = Modifier
                	.align(Alignment.CenterEnd)
            ) {
                Text(text = "Continue")
            }
        }
    }
}

@Composable
fun PermissionButton(
    name: String,
    description: String,
    position: RowPosition,
    granted: Boolean = false,
    onClick: () -> Unit
) {
	val (shape, _) = getDefaultShapeSpacerForPosition(position, 32.dp)

	// val clickModifier = if (!granted) Modifier.clickable { if (!granted) onClick() } else Modifier
	val clickModifier = Modifier.clickable { onClick() }

    Box (
        modifier = Modifier
            .fillMaxWidth(1f)
            .height(104.dp)
            .clip(shape)
            .background(if (!granted) CustomMaterialTheme.colorScheme.surfaceContainer else CustomMaterialTheme.colorScheme.primary)
            .then(clickModifier)
            .padding(16.dp, 12.dp)
    ) {
        Column (
            modifier = Modifier
                .wrapContentWidth()
                .fillMaxHeight(1f)
                .align(Alignment.CenterStart),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = name,
                color = if (!granted) CustomMaterialTheme.colorScheme.onSurface else CustomMaterialTheme.colorScheme.onPrimary,
                fontSize = TextUnit(18f, TextUnitType.Sp),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                color = if (!granted) CustomMaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else CustomMaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                fontSize = TextUnit(14f, TextUnitType.Sp),
                fontWeight = FontWeight.Bold
            )
        }

		if (granted) {
			Column (
				modifier = Modifier
					.fillMaxHeight(1f)
					.width(32.dp)
					.background(CustomMaterialTheme.colorScheme.primary)
					.align(Alignment.CenterEnd),
				verticalArrangement = Arrangement.Center,
				horizontalAlignment = Alignment.CenterHorizontally
			) {
		        Icon(
		            painter = painterResource(id = R.drawable.file_is_selected_foreground),
		            contentDescription = name,
		            tint = CustomMaterialTheme.colorScheme.onPrimary,
		            modifier = Modifier
		                .size(28.dp)
		        )
			}
		}
    }
}

@Composable
fun PermissionDeniedDialog(
    showDialog: MutableState<Boolean>
) {
    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = {
                showDialog.value = false
            },
            confirmButton = {
                PermissionDeniedDialogButton(
                    text = "Grant Permission",
                    color = CustomMaterialTheme.colorScheme.primary,
                    textColor = CustomMaterialTheme.colorScheme.onPrimary,
                    position = RowPosition.Top
                ) {
                    showDialog.value = false
                }
            },
            dismissButton = {
                PermissionDeniedDialogButton(
                    text = "Exit App",
                    color = CustomMaterialTheme.colorScheme.surface,
                    textColor = CustomMaterialTheme.colorScheme.onSurface,
                    position = RowPosition.Bottom
                ) {
                    showDialog.value = false
                }
            },
            title = {
                Text(
                    text = "Permission Required",
                    fontSize = TextUnit(16f, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold,
                    color = CustomMaterialTheme.colorScheme.onBackground
                )
            },
            text = {
                Text(
                    text = "This permission is necessary for app functionality",
                    fontSize = TextUnit(14f, TextUnitType.Sp),
                    color = CustomMaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.wrapContentSize()
                )
            },
            containerColor = CustomMaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(32.dp),
            properties = DialogProperties(
                dismissOnClickOutside = true,
                dismissOnBackPress = true
            ),
        )
    }
}

@Composable
fun PermissionDeniedDialogButton(
    text: String,
    color: Color,
    textColor: Color,
    position: RowPosition,
    onClick: () -> Unit
) {
    val (shape, _) = getDefaultShapeSpacerForPosition(position, cornerRadius = 24.dp)

    Row (
        modifier = Modifier
            .fillMaxWidth(1f)
            .height(64.dp)
            .clip(shape)
            .background(color)
            .clickable {
                onClick()
            }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = TextUnit(16f, TextUnitType.Sp)
        )
    }

    Spacer(modifier = Modifier.height(8.dp))
}