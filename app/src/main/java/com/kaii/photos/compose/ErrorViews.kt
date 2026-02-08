package com.kaii.photos.compose

import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.kaii.photos.R
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.helpers.Screens

enum class ViewProperties(
    val emptyText: Int,
    val emptyIconResId: Int,
    val prefix: Int?,
    val navigate: (AlbumInfo, Int, Long?) -> Screens
) {
    Trash(
        emptyText = R.string.error_views_trash_empty,
        emptyIconResId = R.drawable.delete,
        prefix = R.string.error_views_trash_prefix,
        navigate = { _, index, _ ->
            Screens.Trash.SinglePhoto(
                index = index
            )
        }
    ),
    Album(
        emptyText = R.string.error_views_album_empty,
        emptyIconResId = R.drawable.error,
        prefix = null,
        navigate = { albumInfo, index, next ->
            Screens.Album.SinglePhoto(
                albumInfo = albumInfo,
                index = index,
                nextMediaItemId = next
            )
        }
    ),
    CustomAlbum(
        emptyText = R.string.error_views_album_empty,
        emptyIconResId = R.drawable.error,
        prefix = null,
        navigate = { albumInfo, index, next ->
            Screens.CustomAlbum.SinglePhoto(
                albumInfo = albumInfo,
                index = index,
                nextMediaItemId = next
            )
        }
    ),
    SearchLoading(
        emptyText = R.string.error_views_search_empty,
        emptyIconResId = R.drawable.search,
        prefix = null,
        navigate = { _, index, next ->
            Screens.MainPages.Search.SinglePhoto(
                index = index,
                nextMediaItemId = next
            )
        }
    ),
    SearchNotFound(
        emptyText = R.string.error_views_search_not_found,
        emptyIconResId = R.drawable.error,
        prefix = null,
        navigate = { _, index, next ->
            Screens.MainPages.Search.SinglePhoto(
                index = index,
                nextMediaItemId = next
            )
        }
    ),
    SecureFolder(
        emptyText = R.string.error_views_secure_empty,
        emptyIconResId = R.drawable.secure_folder,
        prefix = R.string.error_views_secure_prefix,
        navigate = { _, index, _ ->
            Screens.SecureFolder.SinglePhoto(
                index = index
            )
        }
    ),
    Favourites(
        emptyText = R.string.error_views_favourites_empty,
        emptyIconResId = R.drawable.favourite,
        prefix = null,
        navigate = { _, index, next ->
            Screens.Favourites.SinglePhoto(
                index = index,
                nextMediaItemId = next
            )
        }
    ),
    Immich(
        emptyText = R.string.immich_server_not_loading,
        emptyIconResId = R.drawable.cloud_off,
        prefix = null,
        navigate = { albumInfo, index, next ->
            Screens.Immich.SinglePhoto(
                albumInfo = albumInfo,
                index = index,
                nextMediaItemId = next
            )
        }
    ),
    Main(
        emptyText = R.string.error_views_main_empty,
        emptyIconResId = R.drawable.photogrid,
        prefix = null,
        navigate = { albumInfo, index, next ->
            Screens.MainPages.MainGrid.SinglePhoto(
                albumInfo = albumInfo,
                index = index,
                nextMediaItemId = next
            )
        }
    );

    companion object {
        fun getText(id: Int, resources: Resources) = resources.getString(id)
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun FolderIsEmpty(
    emptyText: String,
    emptyIconResId: Int,
    backgroundColor: Color = MaterialTheme.colorScheme.background
) {
    Column(
        modifier = Modifier
            .fillMaxSize(1f)
            .background(backgroundColor),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GlideImage(
            model = emptyIconResId,
            contentDescription = stringResource(id = R.string.error_views_folder_non_existent),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(56.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = emptyText,
            fontSize = TextUnit(16f, TextUnitType.Sp),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .wrapContentSize()
        )
    }
}