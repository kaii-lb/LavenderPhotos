package com.kaii.photos.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.data.immich.ImmichSessionManager
import com.kaii.photos.screens.ImmichShareLinkState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.kaii_lb.lavender.immichintegration.serialization.shared_links.SharedLinkResponseDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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

@HiltViewModel(assistedFactory = ImmichShareAlbumViewModel.Factory::class)
class ImmichShareAlbumViewModel @AssistedInject constructor(
    @Assisted private val albumImmichId: String,
    sessionManager: ImmichSessionManager
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(albumImmichId: String): ImmichShareAlbumViewModel
    }

    private val _state = MutableStateFlow<CreateLinkState>(CreateLinkState.Idle)
    val state = _state.asStateFlow()

    private val _links = MutableStateFlow(emptyList<SharedLinkResponseDto>())
    val links = _links.asStateFlow()

    val shareLinkState = ImmichShareLinkState()

    private val sharedLinkClient = sessionManager.sharedLinkClient

    private val _endpoint = MutableStateFlow("")
    val endpoint = _endpoint.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.infoUpdates.collectLatest { info ->
                _endpoint.value = info.endpoint

                if (info.endpoint.isNotBlank()) {
                    while (true) {
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

    fun showLink(slug: String?, key: String) {
        _state.value = CreateLinkState.Success(
            url = buildLink(slug, key)
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

        return buildLink(link.slug, link.key)
    }

    private fun buildLink(slug: String?, key: String) =
        buildString {
            val slug = slug?.takeIf { it.isNotBlank() }
            val path = if (slug != null) "s" else "share"

            append("${endpoint.value}/$path/")

            if (slug != null) append(slug)
            else append(key)
        }

    private suspend fun refreshLinks() {
        _links.value = sharedLinkClient.getAllLinks(
            albumId = Uuid.parse(albumImmichId),
            linkId = null
        ) ?: emptyList()
    }
}