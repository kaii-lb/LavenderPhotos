package com.kaii.photos.repositories

import android.content.Context
import android.net.Uri
import android.util.Patterns
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.preferences.SettingsImmichImpl
import com.kaii.photos.mediastore.getMediaStoreDataFromUri
import com.kaii.photos.models.OperationStatus
import io.github.kaii_lb.lavender.immichintegration.Auth
import io.github.kaii_lb.lavender.immichintegration.clients.LoginClient
import io.github.kaii_lb.lavender.immichintegration.clients.ServerClient
import io.github.kaii_lb.lavender.immichintegration.serialization.FullUserResponse
import io.github.kaii_lb.lavender.immichintegration.serialization.LoginResponse
import io.github.kaii_lb.lavender.immichintegration.serialization.UsageByUserDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface LoginState {
    data class LoggedIn(val user: FullUserResponse) : LoginState

    object ServerUnreachable : LoginState

    object LoggedOut : LoginState
}

data class ServerInfo(
    val version: String,
    val build: String?,
    val online: Boolean,
    val diskSize: String,
    val diskUsed: String,
    val diskUsedPercentage: Float,
    val perUserStorage: List<UsageByUserDto>,
    val newVersion: String?
)

class ImmichInfoRepository(
    private val serverClient: ServerClient,
    private val loginClient: LoginClient,
    private val settings: SettingsImmichImpl,
    scope: CoroutineScope
) {
    private val _serverInfo = MutableStateFlow<ServerInfo?>(null)
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

                serverClient.setAuth(info.auth)
                loginClient.setAuth(info.auth)

                serverClient.setEndpoint(info.endpoint)
                loginClient.setEndpoint(info.endpoint)

                refresh()
            }
        }
    }

    private suspend fun getLoginState() = withContext(Dispatchers.IO) {
        if (!loginClient.ping()) return@withContext LoginState.ServerUnreachable
        if (!loginClient.validate()) return@withContext LoginState.LoggedOut

        loginClient.getMe()?.let {
            LoginState.LoggedIn(user = it)
        } ?: LoginState.LoggedOut
    }

    private suspend fun getServerState() = withContext(Dispatchers.IO) {
        val online = async { serverClient.ping() }
        val storage = async { serverClient.getStorage() }
        val info = async { serverClient.getVersionInfo() }
        val perUserStorage = async { serverClient.getUsagePerUser() }
        val versionCheck = async { serverClient.checkVersion() }

        // fetch in parallel
        val serverInfo = info.await()
        val storageInfo = storage.await()
        val perUserInfo = perUserStorage.await()

        if (serverInfo == null || storageInfo == null || perUserInfo == null) {
            cancel("Could not fetch all required data")
            return@withContext null
        }

        return@withContext ServerInfo(
            version = serverInfo.version,
            build = serverInfo.build,
            online = online.await(),
            diskSize = storageInfo.diskSize,
            diskUsed = storageInfo.diskUse,
            diskUsedPercentage = storageInfo.diskUsagePercentage / 100f,
            perUserStorage = perUserInfo.usageByUser,
            newVersion = versionCheck.await()?.releaseVersion
        )
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

        _serverInfo.value = getServerState()
        _userInfo.value = getLoginState()

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
    ): LoginResponse? {
        _operationChannel.trySend(OperationStatus.Loading)
        val userAgent = System.getProperty("http.agent") ?: ""
        val state = loginClient.login(email, password, userAgent)

        _operationChannel.trySend(
            if (state != null) OperationStatus.Successful
            else OperationStatus.Failed
        )

        return state
    }

    suspend fun authenticate(
        apiKey: String
    ): LoginState {
        auth = Auth.ApiKey(apiKey)
        serverClient.setAuth(auth)
        loginClient.setAuth(auth)

        _operationChannel.trySend(OperationStatus.Loading)
        val state = getLoginState()

        _operationChannel.trySend(
            if (state is LoginState.LoggedIn) OperationStatus.Successful
            else OperationStatus.Failed
        )

        return state
    }

    suspend fun logout() {
        loginClient.logout().let { success ->
            if (success) {
                _userInfo.value = LoginState.LoggedOut

                val info = settings.getImmichBasicInfo().first()
                settings.setImmichBasicInfo(ImmichBasicInfo.Empty.copy(endpoint = info.endpoint))
            }
        }
    }

    suspend fun ping(address: String) = serverClient.ping(address = address)
    fun validateServerAddress(address: String) = Patterns.WEB_URL.matcher(address).matches()

    suspend fun updateInfo(
        email: String?,
        username: String?
    ): Boolean {
        _operationChannel.trySend(OperationStatus.Loading)

        loginClient.updateInfo(
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

        loginClient.changePassword(
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

        val success = loginClient.uploadPfp(
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