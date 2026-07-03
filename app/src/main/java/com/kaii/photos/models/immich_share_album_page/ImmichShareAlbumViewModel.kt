package com.kaii.photos.models.immich_share_album_page

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.PhotosApplication
import com.kaii.photos.datastore.Settings
import com.kaii.photos.screens.ImmichShareLinkState
import io.github.kaii_lb.lavender.immichintegration.Auth
import io.github.kaii_lb.lavender.immichintegration.clients.ApiClient
import io.github.kaii_lb.lavender.immichintegration.clients.SharedLinkClient
import io.github.kaii_lb.lavender.immichintegration.serialization.shared_links.SharedLinkResponseDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface CreateLinkState {
    object Idle : CreateLinkState
    object Creating : CreateLinkState
    object Failed : CreateLinkState
    data class Success(val url: String) : CreateLinkState
}

@OptIn(ExperimentalUuidApi::class)
class ImmichShareAlbumViewModel(
    private val albumImmichId: String,
    private val settings: Settings = PhotosApplication.appModule.settings,
    apiClient: ApiClient = PhotosApplication.appModule.apiClient
) : ViewModel() {
    private val sharedLinkClient = SharedLinkClient(
        client = apiClient,
        endpoint = "",
        auth = Auth.None
    )

    private val _state = MutableStateFlow<CreateLinkState>(CreateLinkState.Idle)
    val state = _state.asStateFlow()

    private val _links = MutableStateFlow(emptyList<SharedLinkResponseDto>())
    val links = _links.asStateFlow()

    val shareLinkState = ImmichShareLinkState()

    private var endpoint = ""

    init {
        viewModelScope.launch {
            settings.immich.getImmichBasicInfo().collect { info ->
                sharedLinkClient.setEndpoint(info.endpoint)
                sharedLinkClient.setAuth(info.auth)
                endpoint = info.endpoint

                if (endpoint.isNotBlank()) launch {
                    while (true) {
                        if (endpoint.isBlank()) break

                        refreshLinks()

                        delay(5.seconds)
                    }
                }
            }
        }
    }

    fun createLink() {
        viewModelScope.launch {
            _state.value = CreateLinkState.Creating

            val url = getLink()

            _state.value = if (url != null) {
                CreateLinkState.Success(url)
            } else {
                CreateLinkState.Failed
            }
        }
    }

    fun showLink(slug: String?, id: String) {
        _state.value = CreateLinkState.Success(
            url = buildLink(slug, id)
        )
    }

    fun dismiss() {
        _state.value = CreateLinkState.Idle
    }

    fun removeLink(id: String) {
        viewModelScope.launch {
            sharedLinkClient.deleteLink(id = id)
            refreshLinks()
        }
    }

    /** returns the URL of the shared album */
    @OptIn(ExperimentalUuidApi::class)
    private suspend fun getLink(): String? {
        val link = sharedLinkClient.postLink(
            link = shareLinkState.createRequest(albumImmichId)
        ) ?: return null

        return buildLink(link.slug, link.id.toString())
    }

    private fun buildLink(slug: String?, id: String) =
        buildString {
            append("$endpoint/s/")

            if (slug != null) append(slug)
            else append(id)
        }

    private suspend fun refreshLinks() {
        _links.value = sharedLinkClient.getAllLinks(
            albumId = Uuid.parse(albumImmichId),
            linkId = null
        ) ?: emptyList()
    }
}