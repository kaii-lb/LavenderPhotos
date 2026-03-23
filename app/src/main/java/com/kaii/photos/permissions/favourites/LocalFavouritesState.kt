package com.kaii.photos.permissions.favourites

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.net.Uri
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


class LocalFavouritesState(
    uri: Uri,
    context: Context,
    private val setFavourite: (favourite: Boolean) -> PendingIntent?
) : GenericFavouritesState {
    internal var launcher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>? = null

    private var nextState = false

    private val _state = MutableStateFlow(false)
    override val state = _state.asStateFlow()

    init {
        _state.value = context.contentResolver.isFavourited(uri = uri)
    }

    override suspend fun favourite(
        favourite: Boolean
    ) {
        nextState = favourite

        val intent = setFavourite(favourite)

        if (launcher == null && intent != null) {
            nextState = false
            return
        }

        if (intent != null) {
            launcher?.launch(IntentSenderRequest.Builder(intent.intentSender).build())
        }
    }

    @OptIn(ExperimentalTime::class)
    fun onResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            _state.value = nextState
        } else {
            _state.value = false
        }
    }
}

@Composable
fun rememberLocalFavouritesState(
    media: MediaStoreData,
    setFavourite: (favourite: Boolean) -> PendingIntent?
): LocalFavouritesState {
    val context = LocalContext.current
    val state = remember(media) {
        LocalFavouritesState(
            uri = media.uri.toUri(),
            context = context,
            setFavourite = setFavourite
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = state::onResult
    )

    DisposableEffect(launcher, state) {
        state.launcher = launcher

        onDispose {
            state.launcher = null
        }
    }

    return state
}