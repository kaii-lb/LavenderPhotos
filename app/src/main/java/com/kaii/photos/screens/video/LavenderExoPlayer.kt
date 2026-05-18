package com.kaii.photos.screens.video

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Effect
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.kaii.photos.di.appModule
import com.kaii.photos.helpers.EncryptionManager
import com.kaii.photos.helpers.editing.applyEffects
import io.github.kaii_lb.lavender.immichintegration.Auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

@OptIn(UnstableApi::class)
class LavenderExoPlayer(
    context: Context,
    onDurationChanged: (duration: Float) -> Unit,
    onCurrentPositionChanged: (position: Float) -> Unit,
    onPlaybackStateChanged: (state: Int) -> Unit,
    onAudioTracksChanged: (audioTrack: List<AudioTrack>) -> Unit
) {
    data class AudioTrack(
        val language: String,
        val label: String
    )

    sealed interface Input {
        data class Local(
            val uri: Uri
        ) : Input

        data class Secure(
            val absolutePath: String,
            val iv: ByteArray
        ) : Input {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Secure

                if (absolutePath != other.absolutePath) return false
                if (!iv.contentEquals(other.iv)) return false

                return true
            }

            override fun hashCode(): Int {
                var result = absolutePath.hashCode()
                result = 31 * result + iv.contentHashCode()
                return result
            }
        }

        data class Networked(
            val uri: Uri,
            val auth: Auth
        ) : Input
    }

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).apply {
        setLoadControl(
            DefaultLoadControl.Builder().apply {
                setBufferDurationsMs(
                    1000,
                    5000,
                    1000,
                    1000
                )

                setBackBuffer(
                    1000,
                    false
                )

                setPrioritizeTimeOverSizeThresholds(false)
            }.build()
        )
        setSeekBackIncrementMs(5000)
        setSeekForwardIncrementMs(5000)

        setPauseAtEndOfMediaItems(true)

        setAudioAttributes(
            AudioAttributes.Builder().apply {
                setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                AudioAttributes.DEFAULT
                setAllowedCapturePolicy(C.ALLOW_CAPTURE_BY_ALL)
            }.build(),
            false
        )

        setHandleAudioBecomingNoisy(true)
    }.build().apply {
        videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
        repeatMode = ExoPlayer.REPEAT_MODE_ONE

        trackSelector?.parameters?.let {
            trackSelector?.parameters = it.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
        }

        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)

                onPlaybackStateChanged(playbackState)

                if (playbackState == ExoPlayer.STATE_READY) {
                    onDurationChanged(duration / 1000f)
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                super.onPositionDiscontinuity(oldPosition, newPosition, reason)
                onCurrentPositionChanged(newPosition.positionMs / 1000f)
            }

            override fun onTracksChanged(tracks: Tracks) {
                onAudioTracksChanged(getAudioTracks(tracks))
            }
        }

        addListener(listener)
    }

    /** in seconds */
    val currentPosition: Float
        get() = exoPlayer.currentPosition / 1000f

    /** in seconds */
    val duration: Float
        get() = exoPlayer.duration / 1000f

    val playbackSpeed: Float
        get() = exoPlayer.playbackParameters.speed

    val videoSize: VideoSize
        get() = exoPlayer.videoSize

    val videoFormat: Format?
        get() = exoPlayer.videoFormat

    val audioFormat: Format?
        get() = exoPlayer.audioFormat

    private fun getAudioTracks(tracks: Tracks): List<AudioTrack> {
        val list = mutableListOf<AudioTrack>()

        tracks.groups.forEach { group ->
            for (i in 0..<group.length) {
                val track = group.getTrackFormat(i)

                if (track.sampleMimeType == null || track.language == null || track.label == null) continue

                track.label

                if (track.sampleMimeType!!.contains("audio")) {
                    list.add(
                        AudioTrack(
                            language = track.language!!,
                            label = Locale.forLanguageTag(track.language!!).displayName
                        )
                    )
                }
            }
        }

        return list
    }

    fun pause() = exoPlayer.pause()
    fun play() = exoPlayer.play()
    fun setPlayWhenReady(value: Boolean) {
        exoPlayer.playWhenReady = value
    }

    fun seekTo(position: Long) = exoPlayer.seekTo(position)
    fun seekBack() = exoPlayer.seekBack()
    fun seekForward() = exoPlayer.seekForward()

    fun setMute(value: Boolean) {
        exoPlayer.volume = if (value) 0f else 1f

        exoPlayer.setAudioAttributes(
            AudioAttributes.Builder().apply {
                setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                AudioAttributes.DEFAULT
                setAllowedCapturePolicy(C.ALLOW_CAPTURE_BY_ALL)
            }.build(),
            !value
        )
    }

    fun setScrubbingModeEnabled(enabled: Boolean) {
        exoPlayer.isScrubbingModeEnabled = enabled
    }

    fun setRepeatMode(value: Boolean) {
        if (value) {
            exoPlayer.pauseAtEndOfMediaItems = false
            exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
        } else {
            exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
            exoPlayer.pauseAtEndOfMediaItems = true
        }
    }

    suspend fun setSource(
        input: Input,
        context: Context,
        loop: Boolean,
        decryptProgress: (progress: Float) -> Unit
    ) {
        if (exoPlayer.isReleased) return

        if (exoPlayer.isCommandAvailable(Player.COMMAND_STOP)) exoPlayer.stop()
        if (exoPlayer.isCommandAvailable(Player.COMMAND_CHANGE_MEDIA_ITEMS)) exoPlayer.clearMediaItems()

        setRepeatMode(loop)

        val source = when (input) {
            is Input.Secure -> {
                val output = withContext(Dispatchers.IO) {
                    EncryptionManager.decryptVideo(
                        absolutePath = input.absolutePath,
                        iv = input.iv,
                        context = context,
                        progress = decryptProgress
                    )
                }

                val factory = DefaultDataSource.Factory(context)
                ProgressiveMediaSource.Factory(factory)
                    .createMediaSource(MediaItem.fromUri(output.toUri()))
            }

            is Input.Local -> {
                val factory = DefaultDataSource.Factory(context)
                ProgressiveMediaSource.Factory(factory)
                    .createMediaSource(MediaItem.fromUri(input.uri))
            }

            is Input.Networked -> {
                val cacheSink = CacheDataSink.Factory()
                    .setCache(context.appModule.cache)

                val downstream = FileDataSource.Factory()
                val upstream = DefaultHttpDataSource.Factory()
                    .setAllowCrossProtocolRedirects(true)
                    .setConnectTimeoutMs(10.seconds.inWholeMilliseconds.toInt())
                    .setDefaultRequestProperties(input.auth.headers)

                val factory = CacheDataSource.Factory()
                    .setCache(context.appModule.cache)
                    .setCacheWriteDataSinkFactory(cacheSink)
                    .setCacheReadDataSourceFactory(downstream)
                    .setUpstreamDataSourceFactory(upstream)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

                ProgressiveMediaSource.Factory(factory)
                    .createMediaSource(MediaItem.fromUri(input.uri))
            }
        }

        exoPlayer.setMediaSource(source)
        if (exoPlayer.isCommandAvailable(Player.COMMAND_PREPARE)) exoPlayer.prepare()
    }

    fun release() {
        exoPlayer.stop()
        exoPlayer.release()
    }

    fun setPlaybackSpeed(speed: Float) = exoPlayer.setPlaybackSpeed(speed)

    fun setVolume(volume: Float) {
        exoPlayer.volume = volume
    }

    fun applyEffects(uri: String, effectList: List<Effect>) = exoPlayer.applyEffects(uri, effectList)

    fun linkPlayerView(playerView: PlayerView) {
        playerView.player = exoPlayer
    }

    fun setAudioTrack(language: String): Boolean {
        val trackSelector = exoPlayer.trackSelector ?: return false

        if (!trackSelector.isSetParametersSupported) return false

        trackSelector.parameters = trackSelector.parameters
            .buildUpon()
            .setPreferredAudioLanguage(language)
            .build()

        return true
    }

    fun getAudioTrack(): String? {
        val trackSelector = exoPlayer.trackSelector ?: return null
        return trackSelector.parameters.preferredAudioLanguages.firstOrNull()
    }

    fun getFrameRate(): Float? {
        if (videoFormat?.frameRate != null &&
            videoFormat?.frameRate!!.toInt() != Format.NO_VALUE
        ) {
            return videoFormat!!.frameRate
        }

        val groups = exoPlayer.currentTracks.groups

        groups.forEach { group ->
            for (i in 0..<group.length) {
                val fps = group.getTrackFormat(i).frameRate

                if (fps.toInt() != Format.NO_VALUE) {
                    return fps
                }
            }
        }

        return null
    }
}