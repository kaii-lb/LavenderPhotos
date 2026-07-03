package com.kaii.photos.repositories

import android.content.Context
import android.net.Uri
import android.util.Patterns
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.preferences.SettingsImmichImpl
import com.kaii.photos.domain.immich.ImmichLoginState
import com.kaii.photos.domain.immich.ImmichServerInfo
import com.kaii.photos.helpers.SingleJobRunner
import com.kaii.photos.mediastore.getMediaStoreDataFromUri
import com.kaii.photos.models.OperationStatus
import io.github.kaii_lb.lavender.immichintegration.Auth
import io.github.kaii_lb.lavender.immichintegration.clients.LoginClient
import io.github.kaii_lb.lavender.immichintegration.clients.ServerClient
import io.github.kaii_lb.lavender.immichintegration.clients.UserClient
import io.github.kaii_lb.lavender.immichintegration.serialization.login.LoginResponseDto
import io.github.kaii_lb.lavender.immichintegration.serialization.user.UpdateUserInfoDto
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

class ImmichInfoRepository(
    private val serverClient: ServerClient,
    private val loginClient: LoginClient,
    private val userClient: UserClient,
    private val settings: SettingsImmichImpl,
    scope: CoroutineScope
) {
    private val _serverInfo = MutableStateFlow<ImmichServerInfo?>(null)
    val serverInfo = _serverInfo.asStateFlow()

    private val _userInfo = MutableStateFlow<ImmichLoginState>(ImmichLoginState.ServerUnreachable)
    val userInfo = _userInfo.asStateFlow()

    // we use a channel here as to not double-fire events in case of recomposition
    private val _operationChannel = Channel<OperationStatus>()
    val operationStatus = _operationChannel.receiveAsFlow()

    // separate from [_operationChannel] since the two can happen simultaneously
    private val _refreshChannel = Channel<OperationStatus>()
    val refreshStatus = _refreshChannel.receiveAsFlow()

    private var auth: Auth = Auth.None
    private var endpoint = ""
    private val jobRunner = SingleJobRunner(
        coroutineScope = scope,
        coroutineContext = Dispatchers.IO
    )

    init {
        scope.launch {
            settings.getImmichBasicInfo().collectLatest { info ->
                auth = info.auth
                endpoint = info.endpoint

                serverClient.setAuth(info.auth)
                loginClient.setAuth(info.auth)
                userClient.setAuth(info.auth)

                serverClient.setEndpoint(info.endpoint)
                loginClient.setEndpoint(info.endpoint)
                userClient.setEndpoint(info.endpoint)

                refresh()
            }
        }
    }

    private suspend fun getLoginState() = withContext(Dispatchers.IO) {
        if (!loginClient.ping()) return@withContext ImmichLoginState.ServerUnreachable
        if (!loginClient.validate()) return@withContext ImmichLoginState.LoggedOut

        userClient.getMe()?.let {
            ImmichLoginState.LoggedIn(user = it)
        } ?: ImmichLoginState.LoggedOut
    }

    private suspend fun getServerState() = withContext(Dispatchers.IO) {
        val online = serverClient.ping()
        if (!online) return@withContext null

        val storage = async { serverClient.getStorage() }
        val info = async { serverClient.getInfo() }
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

        val newVersion = run {
            val releaseVersion = versionCheck.await()?.releaseVersion

            val intReleaseVersion =
                releaseVersion
                    ?.removePrefix("v")
                    ?.replace(".", "")
                    ?.toInt()
                    ?: Int.MIN_VALUE

            val intCurrentVersion =
                serverInfo.version
                    .removePrefix("v")
                    .replace(".", "")
                    .toInt()

            releaseVersion.takeIf {
                intCurrentVersion < intReleaseVersion
            }
        }

        return@withContext ImmichServerInfo(
            version = serverInfo.version,
            build = serverInfo.build,
            online = true,
            diskSize = storageInfo.diskSize,
            diskUsed = storageInfo.diskUse,
            diskUsedPercentage = storageInfo.diskUsagePercentage / 100f,
            perUserStorage = perUserInfo.usageByUser,
            newVersion = newVersion
        )
    }

    fun refresh() = jobRunner.run {
        // don't cause broken UX and other issues because there isn't
        // even a server to connect to
        if (endpoint.isBlank() || !auth.isValid()) {
            _refreshChannel.send(OperationStatus.Failed)
            _userInfo.value = ImmichLoginState.LoggedOut
            return@run
        }

        _refreshChannel.trySend(OperationStatus.Loading)

        _userInfo.value = getLoginState()
        _serverInfo.value = getServerState()

        when (_userInfo.value) {
            is ImmichLoginState.LoggedIn -> {
                val info = (_userInfo.value as ImmichLoginState.LoggedIn).user
                settings.setUpdatedAt(date = info.updatedAt)
                settings.setUsername(name = info.name)
                _refreshChannel.trySend(OperationStatus.Successful)
            }

            is ImmichLoginState.LoggedOut -> {
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
    ): LoginResponseDto? = withContext(Dispatchers.IO) {
        _operationChannel.trySend(OperationStatus.Loading)
        val userAgent = System.getProperty("http.agent") ?: ""
        val state = loginClient.login(email, password, userAgent)

        _operationChannel.trySend(
            if (state != null) OperationStatus.Successful
            else OperationStatus.Failed
        )

        state
    }

    suspend fun authenticate(
        apiKey: String
    ): ImmichLoginState = withContext(Dispatchers.IO) {
        auth = Auth.ApiKey(apiKey)
        serverClient.setAuth(auth)
        loginClient.setAuth(auth)

        _operationChannel.trySend(OperationStatus.Loading)
        val state = getLoginState()

        _operationChannel.trySend(
            if (state is ImmichLoginState.LoggedIn) OperationStatus.Successful
            else OperationStatus.Failed
        )

        state
    }

    suspend fun logout() = withContext(Dispatchers.IO)  {
        loginClient.logout().let { success ->
            if (success) {
                _userInfo.value = ImmichLoginState.LoggedOut

                val info = settings.getImmichBasicInfo().first()
                settings.setImmichBasicInfo(ImmichBasicInfo.Empty.copy(endpoint = info.endpoint))
            }
        }
    }

    suspend fun ping(address: String): Boolean = withContext(Dispatchers.IO)  {
        serverClient.setEndpoint(address)
        serverClient.ping(address = address)
    }

    fun validateServerAddress(address: String) = Patterns.WEB_URL.matcher(address).matches()

    suspend fun updateInfo(
        email: String?,
        username: String?
    ): Boolean = withContext(Dispatchers.IO) {
        _operationChannel.trySend(OperationStatus.Loading)

        userClient.updateInfo(
            info = UpdateUserInfoDto(
                email = email,
                name = username
            )
        ).let {
            _operationChannel.trySend(
                if (it) OperationStatus.Successful
                else OperationStatus.Failed
            )

            if (it) {
                refresh()
            }

            it
        }
    }

    suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): Boolean = withContext(Dispatchers.IO)  {
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

            success
        }
    }

    suspend fun changeProfilePicture(
        uri: Uri?,
        context: Context
    ): Boolean? = withContext(Dispatchers.IO) {
        if (uri == null) return@withContext null

        _operationChannel.trySend(OperationStatus.Loading)
        val displayName = context.contentResolver.getMediaStoreDataFromUri(uri)?.displayName ?: return@withContext null
        val bytes = context.contentResolver.openInputStream(uri)?.readBytes() ?: return@withContext null

        val success = userClient.uploadPfp(
            bytes = bytes,
            filename = displayName
        ) != null

        _operationChannel.trySend(
            if (success) OperationStatus.Successful
            else OperationStatus.Failed
        )

        success
    }
}