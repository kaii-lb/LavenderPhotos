package com.kaii.photos.helpers.video

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.RetainedEffect
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.Effect
import androidx.media3.common.Format
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.di.appModule
import com.kaii.photos.helpers.VideoPlayerConstants
import com.kaii.photos.helpers.appSecureFolderDir
import com.kaii.photos.helpers.appSecureVideoCacheDir
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.ceil

@OptIn(ExperimentalCoroutinesApi::class)
class VideoPlayerState(
    context: Context,
    autoPlayFlow: Flow<Boolean>,
    private val coroutineScope: CoroutineScope,
    private val muteOnStartFlow: Flow<Boolean>,
    private val loopFlow: Flow<Int>,
    private val isOpenWithView: Boolean,
    private val onControlsTimeout: () -> Unit,
    onPlaybackStateChanged: (state: Int) -> Unit
) {
    companion object {
        private val TAG = VideoPlayerState::class.qualifiedName
    }

    private var playingJob: Job? = null
    private var timeoutJob: Job? = null
    private var currentSource = ""
    private var autoPlay = false
    private var loop = true // default to true to avoid autoplaying when we don't mean it (motion-photo)
    private var hideTimeout = 0L

    /** In Seconds */
    var currentPosition by mutableFloatStateOf(0f)
        private set
    var duration by mutableFloatStateOf(0f)
        private set

    var isMuted by mutableStateOf(true)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var isRepeatModeOn by mutableStateOf(false)
        private set

    var controlsVisible by mutableStateOf(true)
    var shouldPlay = { true }

    var videoTitle by mutableStateOf("")
        private set

    var audioTracks = mutableStateListOf<LavenderExoPlayer.AudioTrack>()
        private set
    var selectedAudioTrack by mutableStateOf<LavenderExoPlayer.AudioTrack?>(null)
        private set

    private val player: LavenderExoPlayer = LavenderExoPlayer(
        context = context,
        onDurationChanged = { new ->
            duration = new

            coroutineScope.launch {
                isRepeatModeOn =
                    when (loopFlow.last()) {
                        0 -> false
                        1 -> new <= 30f
                        else -> true
                    }

                player.setRepeatMode(isRepeatModeOn)
            }
        },
        onCurrentPositionChanged = { new ->
            currentPosition = new
        },
        onPlaybackStateChanged = onPlaybackStateChanged,
        onAudioTracksChanged = { tracks ->
            audioTracks.clear()
            audioTracks.addAll(tracks)

            val current = player.getAudioTrack() ?: tracks.firstOrNull()?.language
            current?.let { setAudioTrack(it) }
        }
    )

    val playbackSpeed: Float
        get() = player.playbackSpeed

    val videoSize: VideoSize
        get() = player.videoSize

    val videoFormat: Format?
        get() = player.videoFormat

    val audioFormat: Format?
        get() = player.audioFormat

    init {
        coroutineScope.launch {
            muteOnStartFlow.collectLatest {
                isMuted = it && !isOpenWithView
                player.setMute(isMuted)
            }
        }

        coroutineScope.launch {
            autoPlayFlow.collectLatest {
                autoPlay = it || isOpenWithView
                if (shouldPlay() && autoPlay && !loop) {
                    play()
                }
            }
        }

        coroutineScope.launch {
            loopFlow.collectLatest {
                isRepeatModeOn =
                    when (it) {
                        0 -> false
                        1 -> player.duration <= 30f
                        else -> true
                    }

                player.setRepeatMode(isRepeatModeOn)
            }
        }
    }

    fun toggleMute() {
        isMuted = !isMuted
        player.setMute(isMuted)
    }

    fun resetMute() = coroutineScope.launch {
        isMuted = muteOnStartFlow.last() && !isOpenWithView
    }

    fun toggleRepeatMode() {
        isRepeatModeOn = !isRepeatModeOn
        player.setRepeatMode(isRepeatModeOn)
    }

    fun release(context: Context) {
        player.release()

        context.appModule.scope.launch(Dispatchers.IO) {
            val dir = File(context.appSecureVideoCacheDir)

            dir.listFiles()?.forEach { if (it.exists()) it.delete() }
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    suspend fun setSource(
        context: Context,
        item: MediaStoreData,
        accessToken: String,
        endpoint: String,
        loop: Boolean = false,
        shouldPlay: () -> Boolean,
        decryptProgress: (progress: Float) -> Unit = {}
    ) {
        this.loop = loop
        this.shouldPlay = shouldPlay
        this.audioTracks.clear()

        val immichUrl = item.immichVideoUrl?.let { endpoint + it }
        val uri = immichUrl ?: item.uri
        if (currentSource == uri) return

        videoTitle = item.displayName

        val input = when {
            // secure item, needs decoding
            item.absolutePath.startsWith(context.appSecureFolderDir) -> {
                withContext(Dispatchers.IO) {
                    val iv = MediaDatabase.getInstance(context).securedItemEntityDao()
                        .getIvFromSecuredPath(item.absolutePath)

                    if (iv == null) {
                        Log.e(TAG, "IV for ${item.displayName} was null, aborting")
                        return@withContext null
                    }

                    LavenderExoPlayer.Input.Secure(
                        absolutePath = item.absolutePath,
                        iv = iv
                    )
                }
            }

            item.immichUrl != null -> {
                LavenderExoPlayer.Input.Networked(
                    uri = uri.toUri(),
                    accessToken = accessToken
                )
            }

            else -> {
                LavenderExoPlayer.Input.Local(
                    uri = uri.toUri()
                )
            }
        }

        if (input == null) return

        player.setSource(
            context = context,
            input = input,
            loop = loop,
            decryptProgress = decryptProgress
        )

        player.setPlayWhenReady(autoPlay && shouldPlay() && !loop)
        if (shouldPlay() && autoPlay && !loop) {
            play()
        }

        isRepeatModeOn =
            when (loopFlow.last()) {
                0 -> false
                1 -> player.duration <= 30f
                else -> true
            }

        player.setRepeatMode(isRepeatModeOn)
    }

    private suspend fun startTimeout() {
        while (hideTimeout > 0) {
            delay(1000)
            hideTimeout -= 1000
        }

        if (!isPlaying) return

        controlsVisible = false
        onControlsTimeout()
    }

    fun delayHide() {
        timeoutJob?.cancel()
        timeoutJob = coroutineScope.launch {
            hideTimeout = VideoPlayerConstants.CONTROLS_HIDE_TIMEOUT
            startTimeout()
        }
    }

    fun play() {
        if (!shouldPlay()) return

        isPlaying = true

        player.setScrubbingModeEnabled(false)
        player.play()

        playingJob?.cancel()
        playingJob = coroutineScope.launch {
            delayHide()

            while (isPlaying && shouldPlay()) {
                currentPosition = player.currentPosition

                delay(250)

                if (ceil(currentPosition) >= ceil(duration) &&
                    duration != 0f &&
                    isPlaying &&
                    !loop &&
                    !isRepeatModeOn
                ) launch {
                    controlsVisible = true
                    isPlaying = false
                    delay(2000)
                    delayHide()
                    player.pause()
                    player.seekTo(0)
                    currentPosition = 0f
                }
            }
        }
    }

    fun pause() {
        isPlaying = false
        controlsVisible = true
        playingJob?.cancel()
        playingJob = null

        delayHide()
        player.pause()

        if (currentPosition > 0f) player.setScrubbingModeEnabled(true)
    }

    fun seekBack() {
        val prev = isPlaying
        player.seekBack()
        isPlaying = prev
    }

    fun seekForward() {
        val prev = isPlaying
        player.seekForward()
        isPlaying = prev
    }

    /** @param position in millisecond */
    fun seekTo(position: Long) {
        val prev = isPlaying
        player.seekTo(position)
        currentPosition = player.currentPosition
        isPlaying = prev
    }

    fun setPlaybackSpeed(speed: Float) {
        if (speed !in 0.5f..4f) return

        player.setPlaybackSpeed(speed)
    }

    fun setVolume(volume: Float) = player.setVolume(volume)

    @androidx.annotation.OptIn(UnstableApi::class)
    fun applyEffects(uri: Uri, effectList: List<Effect>) {
        player.applyEffects(uri, effectList)
    }

    fun linkPlayerView(playerView: PlayerView) = player.linkPlayerView(playerView)

    fun setAudioTrack(language: String) {
        if (player.setAudioTrack(language)) {
            selectedAudioTrack = audioTracks.find { it.language == language }
        }
    }

    fun getFrameRate() = player.getFrameRate()
}

@Composable
fun retainVideoPlayerState(
    isOpenWithView: Boolean,
    onControlsTimeout: () -> Unit,
    onPlaybackStateChanged: (state: Int) -> Unit
): VideoPlayerState {
    val context = LocalContext.current
    val settings = context.applicationContext.appModule.settings
    val coroutineScope = rememberCoroutineScope()

    val state = retain {
        VideoPlayerState(
            context = context,
            coroutineScope = coroutineScope,
            muteOnStartFlow = settings.video.getMuteOnStart(),
            autoPlayFlow = settings.video.getShouldAutoPlay(),
            loopFlow = settings.behaviour.getLoopVideos(),
            isOpenWithView = isOpenWithView,
            onControlsTimeout = onControlsTimeout,
            onPlaybackStateChanged = onPlaybackStateChanged
        )
    }

    // pause video when activity goes into background or another activity displays on top of it
    LifecycleEventEffect(event = Lifecycle.Event.ON_PAUSE) {
        state.pause()
    }

    RetainedEffect(state) {
        onRetire {
            state.release(context)
        }
    }

    return state
}