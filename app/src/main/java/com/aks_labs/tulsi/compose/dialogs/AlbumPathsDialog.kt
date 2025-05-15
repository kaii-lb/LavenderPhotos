package com.aks_labs.tulsi.compose.dialogs

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aks_labs.tulsi.MainActivity.Companion.mainViewModel
import com.aks_labs.tulsi.R
import com.aks_labs.tulsi.compose.ConfirmCancelRow
import com.aks_labs.tulsi.compose.HorizontalSeparator
import com.aks_labs.tulsi.compose.TitleCloseRow
import com.aks_labs.tulsi.datastore.AlbumInfo
import com.aks_labs.tulsi.datastore.MainPhotosView
import com.aks_labs.tulsi.helpers.getParentFromPath

private const val TAG = "ALBUM_PATHS_DIALOG"

@Composable
fun AlbumPathsDialog(
    albumInfo: AlbumInfo,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val selectedPaths = remember { mutableStateListOf<String>() }
    
    // Initialize with current album paths
    LaunchedEffect(albumInfo) {
        selectedPaths.clear()
        selectedPaths.addAll(albumInfo.paths)
    }
    
    // Get all available paths from settings
    val allPaths by mainViewModel.settings.MainPhotosView.getAvailablePaths()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    
    // Group paths by parent directory
    val groupedPaths = remember(allPaths) {
        allPaths.groupBy { getParentFromPath(it) }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        ),
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface),
        title = {
            TitleCloseRow(
                title = "Album Paths",
                onClose = onDismiss
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 0.dp)
            ) {
                Text(
                    text = "Select folders to include in this album",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    groupedPaths.forEach { (parent, paths) ->
                        item {
                            // Parent directory header
                            Text(
                                text = parent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            
                            // Check if all paths in this parent are selected
                            val allPathsInParentSelected = paths.all { it in selectedPaths }
                            val somePathsInParentSelected = paths.any { it in selectedPaths }
                            
                            // Parent directory checkbox row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (allPathsInParentSelected) {
                                            // Deselect all paths in this parent
                                            selectedPaths.removeAll(paths)
                                        } else {
                                            // Select all paths in this parent
                                            selectedPaths.addAll(paths.filter { it !in selectedPaths })
                                        }
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = allPathsInParentSelected,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            selectedPaths.addAll(paths.filter { it !in selectedPaths })
                                        } else {
                                            selectedPaths.removeAll(paths)
                                        }
                                    }
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Icon(
                                    painter = painterResource(id = R.drawable.folder),
                                    contentDescription = "Folder",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Text(
                                    text = "Include entire folder",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            HorizontalSeparator()
                        }
                        
                        // Individual paths within this parent
                        items(paths) { path ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp) // Indent to show hierarchy
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (path in selectedPaths) {
                                            selectedPaths.remove(path)
                                        } else {
                                            selectedPaths.add(path)
                                        }
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = path in selectedPaths,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            selectedPaths.add(path)
                                        } else {
                                            selectedPaths.remove(path)
                                        }
                                    }
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Text(
                                    text = path.substringAfter("$parent/"),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            ConfirmCancelRow(
                onConfirm = {
                    Log.d(TAG, "Selected paths: $selectedPaths")
                    onConfirm(selectedPaths.toList())
                    onDismiss()
                },
                onCancel = onDismiss
            )
        }
    )
}
