package com.kaii.photos.models.immich_info_page

import android.content.Context
import android.net.Uri
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.R
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.di.appModule
import com.kaii.photos.repositories.ImmichInfoRepository
import io.github.kaii_lb.lavender.immichintegration.Auth
import io.github.kaii_lb.lavender.immichintegration.serialization.LoginStatus
import io.github.kaii_lb.lavender.immichintegration.state_managers.LoginStateManager
import io.github.kaii_lb.lavender.immichintegration.state_managers.ServerState
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class ImmichInfoViewModel(
    context: Context
) : ViewModel() {
    private val settings = context.applicationContext.appModule.settings

    val info = settings.immich.getImmichBasicInfo().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = ImmichBasicInfo.Empty
    )

    val apiClient = context.appModule.apiClient
    val repo = ImmichInfoRepository(
        serverState = ServerState(apiClient),
        loginState = LoginStateManager(apiClient),
        settings = settings.immich,
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

    fun setInfo(info: ImmichBasicInfo) = settings.immich.setImmichBasicInfo(info)

    fun refresh() {
        viewModelScope.launch {
            repo.refresh()
        }
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
            val state = repo.login(email, password)

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

            if (state is LoginStatus.LoggedIn) {
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
                info = if (state is LoginStatus.LoggedIn) {
                    ImmichBasicInfo(
                        endpoint = info.value.endpoint,
                        auth = Auth.AccessToken(accessToken = state.accessToken),
                        username = state.name,
                        userId = state.userId,
                        updatedAt = ""
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