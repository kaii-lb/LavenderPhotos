package com.kaii.photos.repositories

import android.content.Context
import android.net.Uri
import android.util.Patterns
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.preferences.SettingsImmichImpl
import com.kaii.photos.mediastore.getMediaStoreDataFromUri
import com.kaii.photos.models.OperationStatus
import io.github.kaii_lb.lavender.immichintegration.Auth
import io.github.kaii_lb.lavender.immichintegration.serialization.LoginStatus
import io.github.kaii_lb.lavender.immichintegration.state_managers.LoginState
import io.github.kaii_lb.lavender.immichintegration.state_managers.LoginStateManager
import io.github.kaii_lb.lavender.immichintegration.state_managers.ServerInfoState
import io.github.kaii_lb.lavender.immichintegration.state_managers.ServerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class ImmichInfoRepository(
    private val serverState: ServerState,
    private val loginState: LoginStateManager,
    private val settings: SettingsImmichImpl,
    scope: CoroutineScope
) {
    private val _serverInfo = MutableStateFlow<ServerInfoState>(ServerInfoState.Unavailable)
    val serverInfo = _serverInfo.asStateFlow()

    private val _userInfo = MutableStateFlow<LoginState>(LoginState.ServerUnreachable)
    val userInfo = _userInfo.asStateFlow()

    // we use a channel here as to not double-fire events in case of recomposition
    private val _operationChannel = Channel<OperationStatus>()
    val operationStatus = _operationChannel.receiveAsFlow()

    // separate from [_operationChannel] since the two can happen simultaneously
    private val _refreshChannel = Channel<OperationStatus>()
    val refreshStatus = _refreshChannel.receiveAsFlow()

    private var auth: Auth = Auth.None
    private var endpoint = ""

    init {
        scope.launch {
            settings.getImmichBasicInfo().collectLatest { info ->
                auth = info.auth
                endpoint = info.endpoint

                serverState.setAuth(info.auth)
                loginState.setAuth(info.auth)

                serverState.setEndpoint(info.endpoint)
                loginState.setEndpoint(info.endpoint)

                refresh()
            }
        }
    }

    suspend fun refresh() {
        // don't cause broken UX and other issues because there isn't
        // even a server to connect to
        if (endpoint.isBlank() || auth.asString().isBlank()) {
            _refreshChannel.trySend(OperationStatus.Failed)
            _userInfo.value = LoginState.LoggedOut
            return
        }

        _refreshChannel.trySend(OperationStatus.Loading)

        _serverInfo.value = serverState.fetch()
        _userInfo.value = loginState.refresh()

        when (_userInfo.value) {
            is LoginState.LoggedIn -> {
                val info = (_userInfo.value as LoginState.LoggedIn).user
                settings.setUpdatedAt(date = info.updatedAt)
                settings.setUsername(name = info.name)
                _refreshChannel.trySend(OperationStatus.Successful)
            }

            is LoginState.LoggedOut -> {
                _refreshChannel.trySend(OperationStatus.Failed)
                settings.setImmichBasicInfo(ImmichBasicInfo.Empty.copy(endpoint = endpoint))
            }

            else -> {
                _refreshChannel.trySend(OperationStatus.Failed)
            }
        }
    }

    suspend fun login(
        email: String,
        password: String
    ): LoginStatus {
        _operationChannel.trySend(OperationStatus.Loading)
        val userAgent = System.getProperty("http.agent") ?: ""
        val state = loginState.login(email, password, userAgent)

        _operationChannel.trySend(
            if (state is LoginStatus.LoggedIn) OperationStatus.Successful
            else OperationStatus.Failed
        )

        return state
    }

    suspend fun authenticate(
        apiKey: String
    ): LoginState {
        auth = Auth.ApiKey(apiKey)
        serverState.setAuth(auth)
        loginState.setAuth(auth)

        _operationChannel.trySend(OperationStatus.Loading)
        val state = loginState.refresh()

        _operationChannel.trySend(
            if (state is LoginState.LoggedIn) OperationStatus.Successful
            else OperationStatus.Failed
        )

        return state
    }

    suspend fun logout() {
        loginState.logout().let { success ->
            if (success) {
                _userInfo.value = LoginState.LoggedOut

                val info = settings.getImmichBasicInfo().first()
                settings.setImmichBasicInfo(ImmichBasicInfo.Empty.copy(endpoint = info.endpoint))
            }
        }
    }

    suspend fun ping(address: String) = serverState.ping(address = address)
    fun validateServerAddress(address: String) = Patterns.WEB_URL.matcher(address).matches()

    suspend fun updateInfo(
        email: String?,
        username: String?
    ): Boolean {
        _operationChannel.trySend(OperationStatus.Loading)

        loginState.updateInfo(
            name = username,
            email = email
        ).let {
            _operationChannel.trySend(
                if (it) OperationStatus.Successful
                else OperationStatus.Failed
            )

            if (it) {
                refresh()
            }

            return it
        }
    }

    suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): Boolean {
        _operationChannel.trySend(OperationStatus.Loading)

        loginState.changePassword(
            currentPassword = currentPassword,
            newPassword = newPassword
        ).let { success ->
            _operationChannel.trySend(
                if (success) OperationStatus.Successful
                else OperationStatus.Failed
            )

            refresh()

            return success
        }
    }

    suspend fun changeProfilePicture(uri: Uri?, context: Context): Boolean? {
        if (uri == null) return null

        _operationChannel.trySend(OperationStatus.Loading)
        val displayName = context.contentResolver.getMediaStoreDataFromUri(uri)?.displayName ?: return null
        val bytes = context.contentResolver.openInputStream(uri)?.readBytes() ?: return null

        val success = loginState.uploadPfp(
            bytes = bytes,
            filename = displayName
        ) != null

        _operationChannel.trySend(
            if (success) OperationStatus.Successful
            else OperationStatus.Failed
        )

        return success
    }
}