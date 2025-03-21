package org.openedx.course.presentation.unit.video

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
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
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.extractor.DefaultExtractorsFactory
import com.google.android.gms.cast.framework.CastContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.VideoPlaybackSpeed
import org.openedx.core.domain.model.VideoQuality
import org.openedx.core.extension.isTrue
import org.openedx.core.module.TranscriptManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.utils.LocaleUtils
import org.openedx.core.utils.Logger
import org.openedx.course.data.repository.CourseRepository
import org.openedx.course.presentation.CourseAnalytics
import java.util.concurrent.Executors

@SuppressLint("StaticFieldLeak")
class EncodedVideoUnitViewModel(
    courseId: String,
    blockId: String,
    private val context: Context,
    private val preferencesManager: CorePreferences,
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
    private val logger = Logger(TAG)

    var exoPlayer: ExoPlayer? = null
        private set

    @SuppressLint("UnsafeOptInUsageError")
    var castPlayer: CastPlayer? = null
        private set

    private val _state = MutableStateFlow(PlayerState())
    internal val state: StateFlow<PlayerState> = _state

    init {
        transcriptObject.asFlow().distinctUntilChanged().mapNotNull {
            if (!state.value.isSubtitlesReady) {
                exoPlayer?.currentMediaItem?.buildUpon()
                    ?.setSubtitleConfigurations(subtitleConfigurations)?.build()
                    ?.let { mediaItem ->
                        exoPlayer?.clearMediaItems()
                        exoPlayer?.addMediaItem(mediaItem)
                        exoPlayer?.seekTo(getCurrentVideoTime())
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

                MediaItem.SubtitleConfiguration.Builder(Uri.parse(uri))
                    .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                    .setSelectionFlags(selectionFlags)
                    .setLanguage(language)
                    .build()
            }

    private val movieMetadata = MediaMetadata.Builder()
        .setMediaType(MediaMetadata.MEDIA_TYPE_MOVIE)
        .build()

    private val exoPlayerListener = object : Player.Listener {

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            if (playbackState == Player.STATE_ENDED) {
                _state.update { it.copy(isVideoEnded = true) }
                markBlockCompleted(blockId)
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
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

    @UnstableApi
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        if (exoPlayer != null) {
            return
        }
        initPlayer()

        val executor = Executors.newSingleThreadExecutor()
        CastContext.getSharedInstance(context, executor).addOnSuccessListener { castContext ->
            castPlayer = CastPlayer(castContext)
            _isUpdated.value = true
        }.addOnFailureListener {
            logger.e(it, true)
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        exoPlayer?.addListener(exoPlayerListener)
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        if (state.value.isCastActive) {
            getActivePlayer()?.release()
        } else {
            exoPlayer?.removeListener(exoPlayerListener)
            exoPlayer?.pause()
        }
    }

    fun getActivePlayer(): Player? {
        return if (state.value.isCastActive) {
            castPlayer
        } else {
            exoPlayer
        }
    }

    @UnstableApi
    fun releasePlayers() {
        _state.update { it.copy(isPlayerSetUp = false) }
        exoPlayer?.release()
        castPlayer?.release()
        exoPlayer = null
        castPlayer = null
    }

    @UnstableApi
    fun initPlayer() {
        val selector = applyTrackSelector(isSubtitlesDisabled = true)
        exoPlayer = ExoPlayer.Builder(
            context,
            DefaultRenderersFactory(context),
            DefaultMediaSourceFactory(context, DefaultExtractorsFactory()),
            selector,
            DefaultLoadControl(),
            DefaultBandwidthMeter.getSingletonInstance(context),
            DefaultAnalyticsCollector(Clock.DEFAULT),
        ).build().apply {
            setPlaybackSpeed(preferencesManager.videoSettings.videoPlaybackSpeed.speedValue)
        }
        logVideoLoadedEvent(videoUrl)
    }

    @UnstableApi
    private fun applyTrackSelector(isSubtitlesDisabled: Boolean): DefaultTrackSelector {
        val videoQuality = getVideoQuality()
        val params = DefaultTrackSelector.Parameters.Builder(context)
            .apply {
                if (videoQuality != VideoQuality.AUTO) {
                    setMaxVideoSize(videoQuality.width, videoQuality.height)
                    setViewportSize(videoQuality.width, videoQuality.height, false)
                }
            }
            .setRendererDisabled(C.TRACK_TYPE_TEXT, isSubtitlesDisabled)
            .build()

        val factory = AdaptiveTrackSelection.Factory()
        val selector = DefaultTrackSelector(context, factory)
        selector.parameters = params
        exoPlayer?.trackSelectionParameters = params
        return selector
    }

    @UnstableApi
    fun applyPlayerMedia() {
        if (!state.value.isPlayerSetUp) {
            setPlayerMedia(getMediaItem())
            getActivePlayer()?.prepare()
            _state.update { it.copy(isPlayerSetUp = true) }
        }
    }

    fun getMediaItem() = MediaItem.Builder().setMediaMetadata(movieMetadata)
        .setUri(videoUrl)
        .setMimeType(if (videoUrl.endsWith(HLS_EXT)) MimeTypes.APPLICATION_M3U8 else VIDEO_MIME_TYPE)
        .build()

    @UnstableApi
    fun enterFullscreen(): Boolean {
        if (state.value.isCastActive) return false
        isPlaying = getActivePlayer()?.isPlaying.isTrue()
        applyTrackSelector(isSubtitlesDisabled = false)
        return true
    }

    @UnstableApi
    fun leaveFullscreen() {
        applyTrackSelector(isSubtitlesDisabled = true)
        _isUpdated.value = true
    }

    fun changeCastState(isActive: Boolean) {
        _state.update { it.copy(isCastActive = isActive) }
    }

    @UnstableApi
    private fun setPlayerMedia(mediaItem: MediaItem) {
        if (videoUrl.endsWith(HLS_EXT)) {
            val factory = DefaultDataSource.Factory(context)
            val mediaSource: HlsMediaSource =
                HlsMediaSource.Factory(factory).createMediaSource(mediaItem)
            exoPlayer?.setMediaSource(mediaSource, getCurrentVideoTime())
        } else {
            getActivePlayer()?.setMediaItem(
                mediaItem,
                getCurrentVideoTime()
            )
        }
    }

    private fun getVideoQuality() = preferencesManager.videoSettings.videoStreamingQuality

    private companion object {
        private const val TAG = "EncodedVideoUnitViewModel"
        private const val HLS_EXT = ".m3u8"
        private const val VIDEO_MIME_TYPE = "video/*"
    }
}
