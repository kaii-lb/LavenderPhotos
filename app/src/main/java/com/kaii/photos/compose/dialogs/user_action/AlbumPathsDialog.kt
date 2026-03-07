package com.kaii.photos.compose.dialogs.user_action

import android.content.Context
import android.os.storage.StorageManager
import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.DialogExpandableItem
import com.kaii.photos.compose.dialogs.LavenderDialogBase
import com.kaii.photos.compose.pages.FullWidthDialogButton
import com.kaii.photos.compose.widgets.CheckBoxButtonRow
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.createDirectoryPicker
import com.kaii.photos.helpers.findMinParent
import com.kaii.photos.helpers.parent
import com.kaii.photos.helpers.toBasePath
import com.kaii.photos.helpers.toRelativePath

private const val TAG = "com.kaii.photos.dialogs.user_action.AlbumPathsDialog"

@Composable
fun AlbumPathsDialog(
    albumInfo: AlbumType.Folder,
    onConfirm: (selectedPaths: Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedPaths = remember { albumInfo.paths.toMutableStateList() }

    LavenderDialogBase(
        onDismiss = onDismiss
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(1f)
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(0.dp, 0.dp, 0.dp, 4.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.close),
                    contentDescription = "Close this dialog",
                    modifier = Modifier
                        .size(24.dp)
                )
            }

            Text(
                text = albumInfo.name,
                fontSize = TextUnit(18f, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.Center)
            )

            val activityLauncher = createDirectoryPicker { path, basePath ->
                if (path != null && basePath != null) {
                    val absolutePath = basePath + path

                    if (!selectedPaths.contains(absolutePath)) selectedPaths.add(absolutePath)

                    Log.d(TAG, "Path $absolutePath and selected $selectedPaths")
                }
            }

            IconButton(
                onClick = {
                    activityLauncher.launch(null)
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(0.dp, 0.dp, 0.dp, 4.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.add),
                    contentDescription = "add a new album",
                    modifier = Modifier
                        .size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        var volumes by remember {
            mutableStateOf(
                selectedPaths.groupBy {
                    it.toBasePath()
                }
            )
        }

        LaunchedEffect(selectedPaths.size) {
            volumes = selectedPaths.groupBy {
                it.toBasePath()
            }
        }

        LazyColumn(
            modifier = Modifier
                .heightIn(max = 250.dp)
                .fillMaxWidth(1f)
                .animateContentSize()
        ) {
            itemsIndexed(
                items = volumes.keys.toList()
            ) { index, volume ->
                val rowPosition = when {
                    volumes.size == 1 -> {
                        RowPosition.Single
                    }

                    index == 0 -> {
                        RowPosition.Top
                    }

                    index == volumes.size - 1 -> {
                        RowPosition.Bottom
                    }

                    else -> {
                        RowPosition.Middle
                    }
                }

                val expanded = remember { mutableStateOf(volumes.size == 1) }
                val context = LocalContext.current
                val externalVolumes = remember {
                    val manager =
                        context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
                    manager.storageVolumes
                }

                DialogExpandableItem(
                    text = externalVolumes.find {
                        it.directory?.absolutePath == volume.removeSuffix(
                            "/"
                        )
                    }?.getDescription(context) ?: "Some Place",
                    iconResId = R.drawable.drop_down_arrow,
                    position = rowPosition,
                    expanded = expanded
                ) {
                    val children = volumes[volume]!!

                    data class PathItem(
                        val path: String,
                        val children: List<PathItem>?
                    )

                    val relative = children
                        .sortedBy { it.length }

                    val uniques = run {
                        val total = mutableListOf<String>()

                        relative
                            .groupBy { it.toRelativePath(true).substringBefore("/") }
                            .forEach { group ->
                                val min = findMinParent(group.value)
                                val grouped = min
                                    .groupBy { it.parent() }
                                    .map { (key, value) ->
                                        if (value.size > 1) key
                                        else value.first()
                                    }

                                total.addAll(grouped)
                            }

                        total
                    }

                    Log.d(TAG, "Uniques are $uniques")

                    val hierarchy = run {
                        val list = mutableListOf<PathItem>()

                        fun buildHierarchy(path: String): PathItem {
                            val possibleChildren = children.filter {
                                it.toRelativePath(true).parent() == path.toRelativePath(
                                    true
                                )
                            }.toMutableList()

                            val possibleSubChildren = children.filter {
                                it.toRelativePath(true)
                                    .parent()
                                    .startsWith(path.toRelativePath(true))
                            }
                            if (possibleSubChildren.isNotEmpty()) {
                                possibleChildren.addAll(
                                    possibleSubChildren.filter { child ->
                                        child !in possibleChildren && !possibleChildren.any {
                                            it.endsWith(
                                                child.toRelativePath(true)
                                                    .parent()
                                            )
                                        }
                                    }
                                )
                            }

                            return PathItem(
                                path = path,
                                children = possibleChildren.map { buildHierarchy(it) }
                            )
                        }

                        uniques.sortedBy { it.length }.forEach { path ->
                            val item = buildHierarchy(path)
                            list.add(item)
                        }

                        list
                    }

                    @Composable
                    fun getChildren(item: PathItem, parent: PathItem) {
                        Row(
                            modifier = Modifier
                                .wrapContentSize(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            if (parent.children != null) {
                                Icon(
                                    painter = painterResource(id = R.drawable.subdirectory_arrow_right),
                                    contentDescription = "Subdirectory icon",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .offset(x = 8.dp, y = 4.dp)
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .wrapContentSize()
                                    .offset(x = (-12).dp)
                            ) {
                                CheckBoxButtonRow(
                                    text = item.path.replace(parent.path, "").removePrefix("/"),
                                    checked = selectedPaths.contains(item.path),
                                    checkBoxTextSpacing = 0.dp,
                                    height = 32.dp
                                ) {
                                    if (selectedPaths.contains(item.path)) {
                                        selectedPaths.remove(item.path)
                                    } else {
                                        selectedPaths.add(item.path)
                                    }
                                }

                                if (item.children != null) {
                                    Column(
                                        modifier = Modifier
                                            .padding(start = 20.dp)
                                    ) {
                                        item.children.forEach { child ->
                                            getChildren(child, item)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    hierarchy.forEach {
                        getChildren(it, PathItem(it.path.toBasePath(), null))
                    }
                }
            }
        }


        Spacer(modifier = Modifier.height(24.dp))

        FullWidthDialogButton(
            text = stringResource(id = R.string.media_confirm),
            color = MaterialTheme.colorScheme.primary,
            textColor = MaterialTheme.colorScheme.onPrimary,
            position = RowPosition.Single
        ) {
            onConfirm(selectedPaths.toSet())
            onDismiss()
        }
    }
}