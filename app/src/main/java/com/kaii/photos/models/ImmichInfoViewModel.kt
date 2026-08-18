package com.kaii.photos.models

import android.content.Context
import android.net.Uri
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.R
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.preferences.SettingsAlbumsListImpl
import com.kaii.photos.datastore.preferences.SettingsImmichImpl
import com.kaii.photos.domain.immich.ImmichLoginState
import com.kaii.photos.repositories.ImmichInfoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.kaii_lb.lavender.immichintegration.Auth
import io.github.kaii_lb.lavender.immichintegration.clients.LoginClient
import io.github.kaii_lb.lavender.immichintegration.clients.ServerClient
import io.github.kaii_lb.lavender.immichintegration.clients.UserClient
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class ImmichInfoViewModel @Inject constructor(
    albums: SettingsAlbumsListImpl,
    loginClient: LoginClient,
    userClient: UserClient,
    serverClient: ServerClient,
    private val immich: SettingsImmichImpl
) : ViewModel() {
    val info = immich.getImmichBasicInfo().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ImmichBasicInfo.Empty
    )

    private val repo = ImmichInfoRepository(
        loginClient = loginClient,
        userClient = userClient,
        serverClient = serverClient,
        immichSettings = immich,
        albumSettings = albums,
        scope = viewModelScope
    )

    val serverInfo = repo.serverInfo
    val userInfo = repo.userInfo
    val operationStatus = repo.operationStatus
    val refreshStatus = repo.refreshStatus

    private var canRefresh = true
    private var isRefreshing = true

    init {
        loopRefresh()
    }

    private fun loopRefresh() {
        viewModelScope.launch {
            while (canRefresh) {
                refresh()
                delay(30.seconds)
            }
        }
    }

    fun setCanRefresh(value: Boolean) {
        canRefresh = value

        if (value && !isRefreshing) {
            isRefreshing = true
            loopRefresh()
        } else if (!value) {
            isRefreshing = false
        }
    }

    fun setInfo(info: ImmichBasicInfo) = immich.setImmichBasicInfo(info)

    fun refresh() {
        repo.refresh()
    }

    fun logout() {
        viewModelScope.launch {
            repo.logout()
        }
    }

    fun removeServer() {
        logout()
        setInfo(info = ImmichBasicInfo.Empty)
    }

    fun login(
        email: String,
        password: String,
        context: Context
    ) {
        viewModelScope.launch {
            val eventTitle =
                mutableStateOf(context.resources.getString(R.string.immich_login_ongoing))
            val isLoading = mutableStateOf(true)

            LavenderSnackbarController.pushEvent(
                LavenderSnackbarEvent.LoadingEvent(
                    message = eventTitle.value,
                    icon = R.drawable.account_circle,
                    isLoading = isLoading
                )
            )

            val state = repo.login(email, password)

            if (state != null) {
                eventTitle.value = context.resources.getString(R.string.immich_login_successful)
                isLoading.value = false
            } else {
                eventTitle.value = context.resources.getString(R.string.immich_login_failed)
                LavenderSnackbarController.pushEvent(
                    LavenderSnackbarEvent.MessageEvent(
                        message = context.resources.getString(R.string.immich_login_failed),
                        duration = SnackbarDuration.Short,
                        icon = R.drawable.error_2
                    )
                )
            }

            setInfo(
                info = if (state != null) {
                    ImmichBasicInfo(
                        endpoint = info.value.endpoint,
                        auth = Auth.AccessToken(accessToken = state.accessToken),
                        username = state.name,
                        userId = state.userId.toString(),
                        updatedAt = Clock.System.now().format(
                            DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET
                        )
                    )
                } else {
                    ImmichBasicInfo.Empty.copy(endpoint = info.value.endpoint)
                }
            )

            refresh()
        }
    }

    fun authenticate(
        apiKey: String,
        context: Context
    ) {
        viewModelScope.launch {
            val eventTitle =
                mutableStateOf(context.resources.getString(R.string.immich_login_ongoing))
            val isLoading = mutableStateOf(true)

            LavenderSnackbarController.pushEvent(
                LavenderSnackbarEvent.LoadingEvent(
                    message = eventTitle.value,
                    icon = R.drawable.account_circle,
                    isLoading = isLoading
                )
            )

            val state = repo.authenticate(apiKey)

            if (state is ImmichLoginState.LoggedIn) {
                eventTitle.value = context.resources.getString(R.string.immich_login_successful)
                isLoading.value = false
            } else {
                eventTitle.value = context.resources.getString(R.string.immich_login_failed)
                LavenderSnackbarController.pushEvent(
                    LavenderSnackbarEvent.MessageEvent(
                        message = context.resources.getString(R.string.immich_login_failed),
                        duration = SnackbarDuration.Short,
                        icon = R.drawable.error_2
                    )
                )
            }

            setInfo(
                info = if (state is ImmichLoginState.LoggedIn) {
                    ImmichBasicInfo(
                        endpoint = info.value.endpoint,
                        auth = Auth.ApiKey(apiKey = apiKey),
                        username = state.user.name,
                        userId = state.user.id.toString(),
                        updatedAt = Clock.System.now().format(
                            DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET
                        )
                    )
                } else {
                    ImmichBasicInfo.Empty.copy(endpoint = info.value.endpoint)
                }
            )

            refresh()
        }
    }

    suspend fun ping(address: String) = repo.ping(address)
    fun validateServerAddress(address: String) = repo.validateServerAddress(address)

    fun updateInfo(
        context: Context,
        email: String? = null,
        username: String? = null
    ) {
        viewModelScope.launch {
            if (!repo.updateInfo(email, username)) {
                LavenderSnackbarController.pushEvent(
                    LavenderSnackbarEvent.MessageEvent(
                        message = context.resources.getString(
                            if (email != null) R.string.immich_account_change_email_failed
                            else R.string.immich_account_change_username_failed
                        ),
                        duration = SnackbarDuration.Short,
                        icon = R.drawable.error_2
                    )
                )
            }
        }
    }

    fun changePassword(
        context: Context,
        currentPassword: String,
        newPassword: String
    ) {
        viewModelScope.launch {
            if (!repo.changePassword(currentPassword, newPassword)) {
                LavenderSnackbarController.pushEvent(
                    LavenderSnackbarEvent.MessageEvent(
                        message = context.resources.getString(R.string.immich_account_change_password_failed),
                        duration = SnackbarDuration.Short,
                        icon = R.drawable.error_2
                    )
                )
            }
        }
    }

    fun changeProfilePicture(
        uri: Uri?,
        context: Context
    ) {
        viewModelScope.launch {
            val status = repo.changeProfilePicture(uri, context)

            if (status == true) {
                refresh()

                LavenderSnackbarController.pushEvent(
                    LavenderSnackbarEvent.MessageEvent(
                        message = context.resources.getString(R.string.immich_account_change_pfp_succeeeded),
                        icon = R.drawable.account_circle,
                        duration = SnackbarDuration.Short
                    )
                )
            } else if (status == false) {
                LavenderSnackbarController.pushEvent(
                    LavenderSnackbarEvent.MessageEvent(
                        message = context.resources.getString(R.string.immich_account_change_pfp_failed),
                        icon = R.drawable.error_2,
                        duration = SnackbarDuration.Short
                    )
                )
            }
        }
    }
}