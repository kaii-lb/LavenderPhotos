package com.kaii.photos.helpers.video

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
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.google.common.net.HttpHeaders
import com.kaii.photos.helpers.editing.applyEffects
import kotlin.time.Duration.Companion.seconds

@OptIn(UnstableApi::class)
class LavenderExoPlayer(
    context: Context,
    onDurationChanged: (duration: Float) -> Unit,
    onCurrentPositionChanged: (position: Float) -> Unit,
    onPlaybackStateChanged: (state: Int) -> Unit
) {
    private var cache = SimpleCache(
        context.externalCacheDir ?: context.cacheDir,
        NoOpCacheEvictor(),
        StandaloneDatabaseProvider(context)
    )

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
        }

        addListener(listener)
    }

    /** in seconds */
    val currentPosition: Float
        get() = exoPlayer.currentPosition / 1000f

    val playbackSpeed: Float
        get() = exoPlayer.playbackParameters.speed

    val videoSize: VideoSize
        get() = exoPlayer.videoSize

    val videoFormat: Format?
        get() = exoPlayer.videoFormat

    val audioFormat: Format?
        get() = exoPlayer.audioFormat

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

    /** @param uri either the content uri of a local media item or the link to the **original** cloud media
     * @param isNetworked whether this is a cloud media item or not
     * @param accessToken self-explanatory, should not be null if [isNetworked] is true*/
    fun setSource(
        uri: String,
        context: Context,
        isNetworked: Boolean,
        accessToken: String?,
        loop: Boolean
    ) {
        if (exoPlayer.isReleased) return

        if (exoPlayer.isCommandAvailable(Player.COMMAND_STOP)) exoPlayer.stop()
        if (exoPlayer.isCommandAvailable(Player.COMMAND_CHANGE_MEDIA_ITEMS)) exoPlayer.clearMediaItems()

        if (loop) {
            exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
            exoPlayer.pauseAtEndOfMediaItems = false
        } else {
            exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
            exoPlayer.pauseAtEndOfMediaItems = true
        }

        val source = if (!isNetworked) {
            val factory = DefaultDataSource.Factory(context)
            ProgressiveMediaSource.Factory(factory)
                .createMediaSource(MediaItem.fromUri(uri.toUri()))
        } else {
            val cacheSink = CacheDataSink.Factory()
                .setCache(cache)

            val downstream = FileDataSource.Factory()
            val upstream = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(10.seconds.inWholeMilliseconds.toInt())
                .setDefaultRequestProperties(
                    mapOf(
                        HttpHeaders.AUTHORIZATION to "bearer ${accessToken!!}"
                    )
                )

            val factory = CacheDataSource.Factory()
                .setCache(cache)
                .setCacheWriteDataSinkFactory(cacheSink)
                .setCacheReadDataSourceFactory(downstream)
                .setUpstreamDataSourceFactory(upstream)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

            ProgressiveMediaSource.Factory(factory)
                .createMediaSource(
                    MediaItem.fromUri(
                        uri.replace("original", "video/playback")
                    )
                )
        }

        exoPlayer.setMediaSource(source)
        if (exoPlayer.isCommandAvailable(Player.COMMAND_PREPARE)) exoPlayer.prepare()
    }

    fun release() {
        cache.release()
        exoPlayer.stop()
        exoPlayer.release()
    }

    fun setPlaybackSpeed(speed: Float) = exoPlayer.setPlaybackSpeed(speed)

    fun setVolume(volume: Float) {
        exoPlayer.volume = volume
    }

    fun applyEffects(uri: Uri, effectList: List<Effect>) = exoPlayer.applyEffects(uri, effectList)

    fun linkPlayerView(playerView: PlayerView) {
        playerView.player = exoPlayer
    }
}