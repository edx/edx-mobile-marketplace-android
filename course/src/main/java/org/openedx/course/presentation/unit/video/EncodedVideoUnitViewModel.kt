package org.openedx.course.presentation.unit.video

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.media3.cast.CastPlayer
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.Clock
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.extractor.DefaultExtractorsFactory
import com.google.android.gms.cast.framework.CastState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.VideoPlaybackSpeed
import org.openedx.core.domain.model.VideoQuality
import org.openedx.core.extension.isTrue
import org.openedx.core.module.TranscriptManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.utils.LocaleUtils
import org.openedx.course.data.repository.CourseRepository
import org.openedx.course.extension.matches
import org.openedx.course.module.CastManager
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.CourseAnalyticsEvent

@SuppressLint("StaticFieldLeak")
@androidx.annotation.OptIn(UnstableApi::class)
class EncodedVideoUnitViewModel(
    courseId: String,
    blockId: String,
    val title: String,
    private val context: Context,
    private val preferencesManager: CorePreferences,
    private val castManager: CastManager,
    courseRepository: CourseRepository,
    notifier: CourseNotifier,
    networkConnection: NetworkConnection,
    transcriptManager: TranscriptManager,
    courseAnalytics: CourseAnalytics,
) : VideoUnitViewModel(
    courseId,
    blockId,
    courseRepository,
    notifier,
    networkConnection,
    transcriptManager,
    courseAnalytics
) {

    var exoPlayer: ExoPlayer? = null
        private set
    private var playWhenReadyState: Boolean = true
    private val _state = MutableStateFlow(PlayerState())
    internal val state: StateFlow<PlayerState>
        get() = _state

    private var videoTimeJob: Job? = null
    private var isPlayerPrepared = false
    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition = 0L

    init {
        transcriptObject.asFlow().distinctUntilChanged().mapNotNull {
            if (!state.value.isSubtitlesReady) {
                exoPlayer?.currentMediaItem?.buildUpon()
                    ?.setSubtitleConfigurations(subtitleConfigurations)?.build()
                    ?.let { mediaItem ->
                        exoPlayer?.setMediaItem(mediaItem, getCurrentVideoTime())
                        exoPlayer?.playWhenReady = true
                        _state.update { it.copy(isSubtitlesReady = true) }
                    }
            }
        }.launchIn(viewModelScope)
    }

    private val subtitleConfigurations: List<MediaItem.SubtitleConfiguration>
        get() = transcripts
            .toSortedMap(
                compareBy { LocaleUtils.getLanguageByLanguageCode(it) }
            )
            .map { (language, uri) ->
                val selectionFlags =
                    if (language == state.value.selectedLanguage) C.SELECTION_FLAG_DEFAULT else 0

                MediaItem.SubtitleConfiguration.Builder(uri.toUri())
                    .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                    .setSelectionFlags(selectionFlags)
                    .setLanguage(language)
                    .build()
            }

    private val movieMetadata = MediaMetadata.Builder()
        .setMediaType(MediaMetadata.MEDIA_TYPE_MOVIE)
        .setTitle(title)
        .build()

    private val exoPlayerListener = object : Player.Listener {

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)

            if (playbackState == Player.STATE_READY && exoPlayer?.playWhenReady == true) {
                exoPlayer?.play()
            }

            if (playbackState == Player.STATE_ENDED) {
                _state.update { it.copy(isVideoEnded = true) }
                markBlockCompleted(blockId)
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            this@EncodedVideoUnitViewModel.isPlaying = isPlaying
            logPlayPauseEvent(
                videoUrl,
                isPlaying,
                getCurrentVideoTime(),
                getActivePlayer()?.duration ?: 0L
            )
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            super.onPlaybackParametersChanged(playbackParameters)
            val currentSettings = preferencesManager.videoSettings
            val oldSpeed = currentSettings.videoPlaybackSpeed.speedValue
            preferencesManager.videoSettings =
                currentSettings.copy(
                    videoPlaybackSpeed = VideoPlaybackSpeed.getVideoPlaybackSpeed(playbackParameters.speed)
                )
            logVideoSpeedEvent(
                videoUrl,
                oldSpeed,
                playbackParameters.speed,
                getCurrentVideoTime(),
                getActivePlayer()?.duration ?: 0L
            )
        }

        override fun onTracksChanged(tracks: Tracks) {
            super.onTracksChanged(tracks)
            val selectedLanguage = tracks.groups
                .firstOrNull { it.isSelected && it.type == C.TRACK_TYPE_TEXT }
                ?.getTrackFormat(0)
                ?.language ?: ""
            _state.update { it.copy(selectedLanguage = selectedLanguage) }
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        if (exoPlayer != null) {
            return
        }
        initPlayer()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        exoPlayer?.playWhenReady = playWhenReady
        castManager.attachCastPlayer { state ->
            when (state) {
                CastState.CONNECTED -> {
                    logCastConnection(CourseAnalyticsEvent.CAST_CONNECTED)
                    exoPlayer?.pause()
                    castManager.setMediaItem(getMediaItem(), getCurrentVideoTime())
                    changeCastState(true)
                }

                CastState.NOT_CONNECTED -> {
                    logCastConnection(CourseAnalyticsEvent.CAST_DISCONNECTED)
                    exoPlayer?.seekTo(castManager.getCurrentPosition())
                    castManager.stopPlayer()
                    exoPlayer?.play()
                    changeCastState(false)
                }
            }
        }
        exoPlayer?.addListener(exoPlayerListener)

        if ((_state.value.activePlayerType == PlayerType.EXO_REGULAR || _state.value.activePlayerType == PlayerType.EXO_FULL_SCREEN)
            && !isPlayerPrepared
        ) {
            setPlayerMedia(getMediaItem())
            exoPlayer?.prepare()
            exoPlayer?.seekTo(currentWindow, playbackPosition)
            exoPlayer?.playWhenReady = playWhenReady
            isPlayerPrepared = true


        }
        startUpdatingVideoTime()
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        castManager.detachCastPlayer()
        if (state.value.activePlayerType != PlayerType.CHROME_CAST) {
            exoPlayer?.removeListener(exoPlayerListener)
        }
//        exoPlayer?.pause()
        stopUpdatingVideoTime()
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        playWhenReady = exoPlayer?.playWhenReady.isTrue()
        exoPlayer?.pause()
    }

    private fun initPlayer() {
        val selector = applyTrackSelector(isSubtitlesDisabled = true)
        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true) // Use software if hardware fails
        exoPlayer = ExoPlayer.Builder(
            context,
            renderersFactory,
            DefaultMediaSourceFactory(context, DefaultExtractorsFactory()),
            selector,
            DefaultLoadControl(),
            DefaultBandwidthMeter.getSingletonInstance(context),
            DefaultAnalyticsCollector(Clock.DEFAULT),
        ).build().apply {
            setPlaybackSpeed(preferencesManager.videoSettings.videoPlaybackSpeed.speedValue)

            // Build and set the media source once
            val mediaSource = buildMediaSource(videoUrl)
            setMediaSource(mediaSource)

            // Restore playback position and playWhenReady from saved state
            seekTo(playbackPosition)
            playWhenReady = playWhenReadyState

            prepare()
        }
        _state.update { it.copy(activePlayerType = PlayerType.EXO_REGULAR) }
        logVideoLoadedEvent(videoUrl)
    }
    private fun buildMediaSource(videoUrl: String): MediaSource {
        val uri = videoUrl.toUri()
        return ProgressiveMediaSource.Factory(DefaultDataSource.Factory(context))
            .createMediaSource(MediaItem.fromUri(uri))
    }

    private fun getActivePlayer(): Player? {
        return if (state.value.activePlayerType == PlayerType.CHROME_CAST) {
            castManager.castPlayer
        } else {
            exoPlayer
        }
    }

    private fun startUpdatingVideoTime() {
        videoTimeJob = viewModelScope.launch {
            while (isActive) {
                getActivePlayer()?.let {
                    if (it.isPlaying && it.currentMediaItem?.matches(getMediaItem()).isTrue()) {
                        setCurrentVideoTime(it.currentPosition)
                    } else if (it.playbackState == Player.STATE_IDLE || it.playbackState == Player.STATE_ENDED) {
                        setCurrentVideoTime(0)
                    }
                    val completePercentage = it.currentPosition.toDouble() / it.duration.toDouble()
                    if (completePercentage >= 0.8f) {
                        markBlockCompleted(blockId)
                    }
                }
                delay(200L)
            }
        }
    }

    private fun stopUpdatingVideoTime() {
        videoTimeJob?.cancel()
        videoTimeJob = null
    }

    private fun applyTrackSelector(isSubtitlesDisabled: Boolean): DefaultTrackSelector {
        val videoQuality = getVideoQuality()
        val params = DefaultTrackSelector.Parameters.Builder(context)
            .apply {
                if (videoQuality != VideoQuality.AUTO) {
                    setMaxVideoSize(videoQuality.width, videoQuality.height)
                    setViewportSize(videoQuality.width, videoQuality.height, false)
                }
            }
            .setPreferredTextLanguage(_state.value.selectedLanguage)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, isSubtitlesDisabled)
            .build()

        val factory = AdaptiveTrackSelection.Factory()
        val selector = DefaultTrackSelector(context, factory)
        selector.parameters = params
        exoPlayer?.trackSelectionParameters = params
        return selector
    }

    fun enterFullscreen(): Boolean {
        if (state.value.activePlayerType == PlayerType.CHROME_CAST) return false
        applyTrackSelector(isSubtitlesDisabled = false)
        _state.update { it.copy(activePlayerType = PlayerType.EXO_FULL_SCREEN) }
        return true
    }

    fun leaveFullscreen() {
        applyTrackSelector(isSubtitlesDisabled = true)
        _state.update { it.copy(activePlayerType = PlayerType.EXO_REGULAR) }
    }

    private fun changeCastState(isCastActive: Boolean) {
        _state.update {
            it.copy(
                activePlayerType = if (isCastActive) PlayerType.CHROME_CAST else PlayerType.EXO_REGULAR
            )
        }
    }

    private fun setPlayerMedia(mediaItem: MediaItem) {
        val currentItem = exoPlayer?.currentMediaItem

        if (currentItem != null && mediaItem.matches(currentItem)) {
            exoPlayer?.seekTo(getCurrentVideoTime())
            exoPlayer?.playWhenReady = true
            return
        }
        if (videoUrl.endsWith(HLS_EXT)) {
            val factory = DefaultDataSource.Factory(context)
            val mediaSource: HlsMediaSource =
                HlsMediaSource.Factory(factory).createMediaSource(mediaItem)
            exoPlayer?.setMediaSource(mediaSource, getCurrentVideoTime())
        } else {
            exoPlayer?.setMediaItem(
                mediaItem,
                getCurrentVideoTime()
            )
        }
    }

    fun releasePlayers() {
        _state.update { it.copy(activePlayerType = PlayerType.NONE) }
        exoPlayer?.release()
        exoPlayer = null
        castManager.stopPlayer()
    }

    private fun getMediaItem() = MediaItem.Builder().setMediaMetadata(movieMetadata)
        .setUri(videoUrl)
        .setMimeType(if (videoUrl.endsWith(HLS_EXT)) MimeTypes.APPLICATION_M3U8 else VIDEO_MIME_TYPE)
        .build()

    fun getCastPlayer(): CastPlayer? = castManager.castPlayer

    private fun getVideoQuality() = preferencesManager.videoSettings.videoStreamingQuality

    private companion object {
        private const val HLS_EXT = ".m3u8"
        private const val VIDEO_MIME_TYPE = "video/*"
    }
}
