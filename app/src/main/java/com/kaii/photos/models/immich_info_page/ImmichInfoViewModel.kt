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
import com.kaii.photos.helpers.profilePicture
import com.kaii.photos.mediastore.getMediaStoreDataFromUri
import com.kaii.photos.models.OperationStatus
import io.github.kaii_lb.lavender.immichintegration.state_managers.LoginState
import io.github.kaii_lb.lavender.immichintegration.state_managers.LoginStateManager
import io.github.kaii_lb.lavender.immichintegration.state_managers.ServerInfoState
import io.github.kaii_lb.lavender.immichintegration.state_managers.ServerState
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class ImmichInfoViewModel(
    context: Context
) : ViewModel() {
    private val pfpPath = context.profilePicture
    private val settings = context.applicationContext.appModule.settings

    val info = settings.immich.getImmichBasicInfo().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = ImmichBasicInfo.Empty
    )

    val alwaysShow = settings.immich.getAlwaysShowUserInfo().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    private val serverState = ServerState(coroutineScope = viewModelScope)
    private val loginState = LoginStateManager(coroutineScope = viewModelScope)

    private val _serverInfo = MutableStateFlow<ServerInfoState>(ServerInfoState.Unavailable)
    val serverInfo = _serverInfo.asStateFlow()

    private val _userInfo = MutableStateFlow<LoginState>(LoginState.LoggedOut)
    val userInfo = _userInfo.asStateFlow()

    // we use a channel here as to not double-fire events in case of recomposition
    private val _operationChannel = Channel<OperationStatus>()
    val operationStatus = _operationChannel.receiveAsFlow()

    // separate from [_operationChannel] since the two can happen simultaneously
    private val _refreshChannel = Channel<OperationStatus>()
    val refreshStatus = _refreshChannel.receiveAsFlow()

    init {
        val apiClient = context.appModule.apiClient

        viewModelScope.launch {
            info.collectLatest {
                serverState.setBaseUrl(
                    baseUrl = it.endpoint,
                    apiClient = apiClient
                )

                loginState.setBaseUrl(
                    baseUrl = it.endpoint,
                    apiClient = apiClient
                )

                refresh()
            }
        }

        viewModelScope.launch {
            while (true) {
                refresh()
                delay(30.seconds)
            }
        }
    }

    fun setInfo(info: ImmichBasicInfo) = settings.immich.setImmichBasicInfo(info)

    fun setAlwaysShow(value: Boolean) = settings.immich.setAlwaysShowUserInfo(value)

    fun refresh() {
        viewModelScope.launch {
            _refreshChannel.trySend(OperationStatus.Loading)

            _serverInfo.value = serverState.fetch(accessToken = info.value.accessToken)
            _userInfo.value = loginState.refresh(
                accessToken = info.value.accessToken,
                pfpSavePath = pfpPath,
                previousPfpUrl = (userInfo.value as? LoginState.LoggedIn)?.pfpUrl ?: ""
            )

            _refreshChannel.trySend(OperationStatus.Successful)
        }
    }

    fun logout() {
        viewModelScope.launch {
            loginState.logout(info.value.accessToken).let { success ->
                if (success) {
                    _userInfo.value = LoginState.LoggedOut

                    setInfo(ImmichBasicInfo.Empty.copy(endpoint = info.value.endpoint))
                }
            }
        }
    }

    fun login(
        email: String,
        password: String,
        userAgent: String,
        context: Context
    ) {
        viewModelScope.launch {
            val state = loginState.login(email, password, userAgent)

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

            if (state is LoginState.LoggedIn) {
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
                info = if (state is LoginState.LoggedIn) {
                    ImmichBasicInfo(
                        endpoint = info.value.endpoint,
                        accessToken = state.accessToken,
                        username = state.name
                    )
                } else {
                    ImmichBasicInfo.Empty.copy(endpoint = info.value.endpoint)
                }
            )
        }
    }

    suspend fun ping(address: String) = serverState.ping(address = address)
    fun validateServerAddress(address: String) = serverState.validateServerAddress(address)

    fun updateInfo(
        context: Context,
        email: String? = null,
        username: String? = null
    ) {
        viewModelScope.launch {
            _operationChannel.trySend(OperationStatus.Loading)

            loginState.updateInfo(
                accessToken = info.value.accessToken,
                name = username,
                email = email
            ).let {
                _operationChannel.trySend(
                    if (it) OperationStatus.Successful
                    else OperationStatus.Failed
                )

                if (it) {
                    refresh()
                } else {
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
    }

    fun changePassword(
        context: Context,
        currentPassword: String,
        newPassword: String
    ) {
        viewModelScope.launch {
            _operationChannel.trySend(OperationStatus.Loading)

            loginState.changePassword(
                accessToken = info.value.accessToken,
                currentPassword = currentPassword,
                newPassword = newPassword
            ).let { success ->
                _operationChannel.trySend(
                    if (success) OperationStatus.Successful
                    else OperationStatus.Failed
                )

                if (!success) {
                    LavenderSnackbarController.pushEvent(
                        LavenderSnackbarEvent.MessageEvent(
                            message = context.resources.getString(R.string.immich_account_change_password_failed),
                            duration = SnackbarDuration.Short,
                            icon = R.drawable.error_2
                        )
                    )
                }
            }

            refresh()
        }
    }

    fun uploadPfp(
        uri: Uri?,
        context: Context
    ) {
        viewModelScope.launch {
            if (uri == null) return@launch

            _operationChannel.trySend(OperationStatus.Loading)
            val displayName = context.contentResolver.getMediaStoreDataFromUri(uri)?.displayName ?: return@launch
            val bytes = context.contentResolver.openInputStream(uri)?.readBytes() ?: return@launch

            val success = loginState.uploadPfp(
                bytes = bytes,
                filename = displayName,
                accessToken = info.value.accessToken
            ) != null

            _operationChannel.trySend(
                if (success) OperationStatus.Successful
                else OperationStatus.Failed
            )

            if (success) {
                refresh()

                LavenderSnackbarController.pushEvent(
                    LavenderSnackbarEvent.MessageEvent(
                        message = context.resources.getString(R.string.immich_account_change_pfp_succeeeded),
                        icon = R.drawable.account_circle,
                        duration = SnackbarDuration.Short
                    )
                )
            } else {
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