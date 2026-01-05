package com.kaii.photos.helpers.permissions.favourites

import android.content.Context
import android.net.Uri
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.LocalAppDatabase
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.datastore.Versions
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.permissions.MediaPermissionsState
import com.kaii.photos.helpers.permissions.rememberMediaPermissionsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class MigrationState {
    NeedsPermission,
    InProgress,
    Done,
    Declined
}

class FavouritesMigrationState(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    appDatabase: MediaDatabase,
    private val navigate: () -> Unit,
    private val onDone: () -> Job
) {
    internal var permissionsState: MediaPermissionsState? = null
    private val _state = MutableStateFlow(MigrationState.NeedsPermission)
    val state = _state.asStateFlow()

    private var favourites = emptyList<Uri>()
    private val scheduler = FavouritesMigrationSchedulingManager()

    init {
        coroutineScope.launch(Dispatchers.IO) {
            favourites = appDatabase.favouritedItemEntityDao().getAll().first().map { it.uri.toUri() }

            if (favourites.isEmpty()) _state.value = MigrationState.Done
        }
    }

    fun step() = coroutineScope.launch(Dispatchers.IO) {
        if (favourites.isEmpty()) {
            coroutineScope.launch {
                LavenderSnackbarController.pushEvent(
                    LavenderSnackbarEvents.MessageEvent(
                        message = context.resources.getString(R.string.favourites_migration_none),
                        icon = R.drawable.lists,
                        duration = SnackbarDuration.Short
                    )
                )
            }

            _state.value = MigrationState.Done
            onDone()
        }

        if (_state.value == MigrationState.NeedsPermission) {
            permissionsState?.getPermissionsFor(uris = favourites)
        } else if (_state.value == MigrationState.Done) {
            navigate()
        }
    }

    fun onGranted() {
        coroutineScope.launch {
            LavenderSnackbarController.pushEvent(
                LavenderSnackbarEvents.MessageEvent(
                    message = context.resources.getString(R.string.favourites_migration_started),
                    icon = R.drawable.database_upload,
                    duration = SnackbarDuration.Short
                )
            )
        }

        _state.value = MigrationState.InProgress

        coroutineScope.launch {
            delay(3000) // just to look pretty
            val taskId = scheduler.scheduleTask(context = context)

            WorkManager.getInstance(context)
                .getWorkInfoByIdFlow(taskId)
                .collect { workInfo ->
                    if (workInfo?.state == WorkInfo.State.SUCCEEDED) {
                        _state.value = MigrationState.Done

                        LavenderSnackbarController.pushEvent(
                            LavenderSnackbarEvents.MessageEvent(
                                message = context.resources.getString(R.string.favourites_migration_done),
                                icon = R.drawable.checkmark_thin,
                                duration = SnackbarDuration.Short
                            )
                        )

                        onDone()
                    }
                }
        }
    }

    fun onFailed() {
        _state.value = MigrationState.Declined
    }
}

@Composable
fun rememberFavouritesMigrationState(): FavouritesMigrationState {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val appDatabase = LocalAppDatabase.current
    val coroutineScope = rememberCoroutineScope()
    val mainViewModel = LocalMainViewModel.current

    val state = remember {
        FavouritesMigrationState(
            context = context,
            appDatabase = appDatabase,
            coroutineScope = coroutineScope,
            navigate = {
                coroutineScope.launch(Dispatchers.Main) {
                    navController.popBackStack(MultiScreenViewType.FavouritesMigrationPage.name, true)
                    navController.navigate(MultiScreenViewType.FavouritesGridView.name)
                }
            },
            onDone = {
                mainViewModel.settings.Versions.setUpdateFav(false)
            }
        )
    }

    val permissionsState = rememberMediaPermissionsState(
        onGranted = state::onGranted,
        onFailed = state::onFailed
    )

    DisposableEffect(Unit) {
        state.permissionsState = permissionsState

        onDispose {
            state.permissionsState = null
        }
    }

    return state
}