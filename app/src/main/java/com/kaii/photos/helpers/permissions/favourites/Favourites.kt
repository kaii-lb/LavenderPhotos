package com.kaii.photos.helpers.permissions.favourites

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.mediastore.isFavourited
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.ExperimentalTime

// private const val TAG = "com.kaii.photos.helpers.permissions.favourites.Favourites"

class FavouritesState(
    uri: Uri,
    private val context: Context,
    private val onSuccess: (isFavourited: Boolean) -> Unit
) {
    internal var launcher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>? = null

    private var nextState = false

    private val _state = MutableStateFlow(false)
    val state = _state.asStateFlow()

    init {
        _state.value = context.contentResolver.isFavourited(uri = uri)
    }

    fun setFavourite(
        uri: Uri,
        favourite: Boolean
    ) {
        nextState = favourite

        if (launcher == null) {
            nextState = false
            return
        }

        val favRequest = MediaStore.createFavoriteRequest(
            context.contentResolver,
            listOf(uri),
            favourite
        )

        launcher?.launch(IntentSenderRequest.Builder(favRequest.intentSender).build())
    }

    @OptIn(ExperimentalTime::class)
    fun onResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            onSuccess(nextState)
            _state.value = nextState
        } else {
            _state.value = false
        }
    }
}

@Composable
fun rememberFavouritesState(
    media: MediaStoreData,
    onFavourited: (isFavourited: Boolean) -> Unit = {}
): FavouritesState {
    val context = LocalContext.current
    val state = remember(media) {
        FavouritesState(
            uri = media.uri.toUri(),
            context = context,
            onSuccess = onFavourited
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = state::onResult
    )

    DisposableEffect(launcher, state) {
        state.launcher = launcher

        onDispose {
            state.launcher == null
        }
    }

    return state
}

class ListFavouritesState(
    private val context: Context,
    private val onSuccess: (isFavourited: Boolean) -> Unit
) {
    internal var launcher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>? = null

    private var nextState = false

    fun setFavourite(
        list: List<Uri>,
        favourite: Boolean
    ) {
        nextState = favourite

        if (launcher == null) {
            nextState = false
            return
        }

        val favRequest = MediaStore.createFavoriteRequest(
            context.contentResolver,
            list,
            favourite
        )

        launcher?.launch(IntentSenderRequest.Builder(favRequest.intentSender).build())
    }

    @OptIn(ExperimentalTime::class)
    fun onResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            onSuccess(nextState)
        }
    }
}

@Composable
fun rememberListFavouritesState(
    onFavourited: (isFavourited: Boolean) -> Unit = {}
): ListFavouritesState {
    val context = LocalContext.current
    val state = remember {
        ListFavouritesState(
            context = context,
            onSuccess = onFavourited
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = state::onResult
    )

    DisposableEffect(launcher, state) {
        state.launcher = launcher

        onDispose {
            state.launcher == null
        }
    }

    return state
}