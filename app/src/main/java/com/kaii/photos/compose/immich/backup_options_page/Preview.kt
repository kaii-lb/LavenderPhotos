package com.kaii.photos.compose.immich.backup_options_page

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.bumptech.glide.signature.ObjectKey
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.state.AlbumGridState
import com.kaii.photos.screens.ImmichBackupOptionsStateImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

@Preview
@Composable
private fun ImmichBackupOptionsPagePreview() {
    ImmichBackupOptionsPage(
        state = remember {
            object : ImmichBackupOptionsStateImpl {
                private val selectedAlbumIds = mutableStateListOf<String>()
                private val queryFlow = MutableStateFlow("")
                private val albumsFlow = flowOf(
                    buildList {
                        repeat(5) { index ->
                            add(
                                AlbumGridState.Album.Single(
                                    info = AlbumGridState.Info(
                                        album = AlbumType.PlaceHolder,
                                        thumbnail = AlbumGridState.Info.Thumbnail(
                                            uri = "",
                                            signature = ObjectKey(0),
                                            albumId = "",
                                            date = 0L
                                        )
                                    ),
                                    id = index.toString(),
                                    name = "Test Album $index",
                                    summary = "/storage/emulated/0/Pictures".takeIf { index % 2 == 0 },
                                    date = 0L,
                                    pinned = false,
                                )
                            )
                        }
                    }
                )

                override val query = queryFlow.asStateFlow()
                override val assetCount = flowOf(1234)
                override val albums = albumsFlow.combine(query) { list, query ->
                    list.filter { album ->
                        album.name.contains(query)
                    }
                }

                override fun selectedCount() = selectedAlbumIds.size
                override fun selected(id: String) = selectedAlbumIds.contains(id)

                override fun toggle(id: String) {
                    if (selectedAlbumIds.contains(id)) selectedAlbumIds.remove(id)
                    else selectedAlbumIds.add(id)
                }

                override fun search(query: String) {
                    queryFlow.value = query
                }

                override suspend fun confirm(context: Context) = false
            }
        },
        navController = rememberNavController(),
        modifier = Modifier
    )
}