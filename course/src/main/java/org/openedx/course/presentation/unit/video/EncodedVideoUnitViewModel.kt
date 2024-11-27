package org.openedx.course.presentation.unit.video

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.media3.cast.CastPlayer
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.Clock
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.extractor.DefaultExtractorsFactory
import com.google.android.gms.cast.framework.CastContext
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.VideoPlaybackSpeed
import org.openedx.core.domain.model.VideoQuality
import org.openedx.core.module.TranscriptManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseNotifier
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
    private val _isVideoEnded = MutableLiveData(false)
    val isVideoEnded: LiveData<Boolean>
        get() = _isVideoEnded

    var exoPlayer: ExoPlayer? = null
        private set

    @SuppressLint("UnsafeOptInUsageError")
    var castPlayer: CastPlayer? = null
        private set

    var isCastActive = false

    var isPlayerSetUp = false

    private val exoPlayerListener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            super.onPlayWhenReadyChanged(playWhenReady, reason)
            isPlaying = playWhenReady
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            if (playbackState == Player.STATE_ENDED) {
                _isVideoEnded.value = true
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
    }

    @androidx.media3.common.util.UnstableApi
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        if (exoPlayer != null) {
            return
        }
        initPlayer()

        val executor = Executors.newSingleThreadExecutor()
        CastContext.getSharedInstance(context, executor).addOnSuccessListener { castContext ->
            castPlayer = CastPlayer(castContext)
        }.addOnFailureListener {
            logger.e(it, true)
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        exoPlayer?.addListener(exoPlayerListener)
        getActivePlayer()?.playWhenReady = isPlaying
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        if (isCastActive) {
            getActivePlayer()?.release()
        } else {
            exoPlayer?.removeListener(exoPlayerListener)
            exoPlayer?.pause()
        }
    }

    fun getActivePlayer(): Player? {
        return if (isCastActive) {
            castPlayer
        } else {
            exoPlayer
        }
    }

    @androidx.media3.common.util.UnstableApi
    fun releasePlayers() {
        exoPlayer?.release()
        castPlayer?.release()
        exoPlayer = null
        castPlayer = null
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun initPlayer() {
        val videoQuality = getVideoQuality()
        val params = DefaultTrackSelector.Parameters.Builder(context)
            .apply {
                if (videoQuality != VideoQuality.AUTO) {
                    setMaxVideoSize(videoQuality.width, videoQuality.height)
                    setViewportSize(videoQuality.width, videoQuality.height, false)
                }
            }
            .build()

        val factory = AdaptiveTrackSelection.Factory()
        val selector = DefaultTrackSelector(context, factory)
        selector.parameters = params

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

    private fun getVideoQuality() = preferencesManager.videoSettings.videoStreamingQuality

    private companion object {
        private const val TAG = "EncodedVideoUnitViewModel"
    }
}
