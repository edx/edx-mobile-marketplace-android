package org.openedx.course.presentation.unit.video

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.window.layout.WindowMetricsCalculator
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.presentation.dialog.appreview.AppReviewManager
import org.openedx.core.presentation.dialog.selectorbottomsheet.SelectBottomDialogFragment
import org.openedx.core.presentation.global.viewBinding
import org.openedx.core.ui.ConnectionErrorView
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.utils.LocaleUtils
import org.openedx.course.R
import org.openedx.course.databinding.FragmentVideoUnitBinding
import org.openedx.course.presentation.ui.VideoSubtitles
import org.openedx.course.presentation.ui.VideoTitle
import org.openedx.course.presentation.ui.enableLongPressDoubleSpeed
import org.openedx.foundation.extension.computeWindowSizeClasses
import org.openedx.foundation.extension.dpToPixel
import org.openedx.foundation.extension.objectToString
import org.openedx.foundation.extension.stringToObject
import org.openedx.foundation.presentation.WindowSize
import kotlin.math.roundToInt

class VideoUnitFragment : Fragment(R.layout.fragment_video_unit) {

    private val binding by viewBinding(FragmentVideoUnitBinding::bind)
    private val viewModel by viewModel<EncodedVideoUnitViewModel> {
        parametersOf(
            requireArguments().getString(ARG_COURSE_ID, ""),
            requireArguments().getString(ARG_VIDEO_URL, ""),
            requireArguments().getString(ARG_BLOCK_ID, ""),
            requireArguments().getString(ARG_TITLE, ""),
        )
    }
    private val appReviewManager by inject<AppReviewManager> { parametersOf(requireActivity()) }

    private var windowSize: WindowSize? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        windowSize = computeWindowSizeClasses()
        lifecycle.addObserver(viewModel)
        requireArguments().apply {
            viewModel.transcripts = stringToObject<Map<String, String>>(
                getString(ARG_TRANSCRIPT_URL, "")
            ) ?: emptyMap()
            viewModel.isDownloaded = getBoolean(ARG_DOWNLOADED)
        }
        viewModel.downloadSubtitles()
    }

    @OptIn(UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.cvVideoTitle?.setContent {
            OpenEdXTheme {
                VideoTitle(text = viewModel.title)
            }
        }

        binding.connectionError.setContent {
            OpenEdXTheme {
                ConnectionErrorView {
                    binding.connectionError.isVisible =
                        !viewModel.hasInternetConnection && !viewModel.isDownloaded
                }
            }
        }

        binding.subtitles.setContent {
            OpenEdXTheme {
                val state = rememberLazyListState()
                val currentIndex by viewModel.currentIndex.collectAsState(0)
                val transcriptObject by viewModel.transcriptObject.observeAsState()
                VideoSubtitles(
                    listState = state,
                    timedTextObject = transcriptObject,
                    subtitleLanguage = LocaleUtils.getDisplayLanguage(viewModel.transcriptLanguage),
                    showSubtitleLanguage = viewModel.transcripts.size > 1,
                    currentIndex = currentIndex,
                    onTranscriptClick = {
                        binding.playerView.player?.apply {
                            seekTo(it.start.mseconds.toLong())
                            play()
                        }
                    },
                    onSettingsClick = {
                        binding.playerView.player?.pause()
                        val dialog = SelectBottomDialogFragment.newInstance(
                            LocaleUtils.getLanguages(viewModel.transcripts.keys.toList())
                        )
                        dialog.show(
                            requireActivity().supportFragmentManager,
                            SelectBottomDialogFragment::class.simpleName
                        )
                    }
                )
            }
        }

        binding.connectionError.isVisible =
            !viewModel.hasInternetConnection && !viewModel.isDownloaded

        setupPlayerHeight()

        viewModel.isUpdated.observe(viewLifecycleOwner) { isUpdated ->
            if (isUpdated) {
                initPlayer()
            }
        }

        viewModel.isVideoEnded.observe(viewLifecycleOwner) { isVideoEnded ->
            if (isVideoEnded && !appReviewManager.isDialogShowed) {
                appReviewManager.tryToOpenRateDialog()
            }
        }
    }

    private fun setupPlayerHeight() {
        val orientation = resources.configuration.orientation
        val windowMetrics = WindowMetricsCalculator.getOrCreate()
            .computeCurrentWindowMetrics(requireActivity())
        val currentBounds = windowMetrics.bounds
        val layoutParams = binding.playerView.layoutParams as FrameLayout.LayoutParams

        if (orientation == Configuration.ORIENTATION_PORTRAIT || windowSize?.isTablet == true) {
            val padding = requireContext().dpToPixel(PLAYER_VIEW_PADDING_DP)
            val width = currentBounds.width() - padding
            val minHeight = requireContext().dpToPixel(MIN_PLAYER_HEIGHT_DP).roundToInt()
            val aspectRatio = VIDEO_ASPECT_RATIO_WIDTH / VIDEO_ASPECT_RATIO_HEIGHT
            val calculatedHeight = (width / aspectRatio).roundToInt()

            layoutParams.height = when {
                windowSize?.isTablet == true -> {
                    requireContext().dpToPixel(TABLET_PLAYER_HEIGHT_DP).roundToInt()
                }

                calculatedHeight < minHeight -> {
                    minHeight
                }

                else -> {
                    calculatedHeight
                }
            }
        }

        binding.playerView.layoutParams = layoutParams
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun initPlayer() {
        with(binding) {
            playerView.player = viewModel.getActivePlayer()
            playerView.setShowNextButton(false)
            playerView.setShowPreviousButton(false)
            showVideoControllerIndefinitely(false)

            val movieMetadata = MediaMetadata.Builder()
                .setMediaType(MediaMetadata.MEDIA_TYPE_MOVIE)
                .build()
            val mediaItem = MediaItem.Builder().setMediaMetadata(movieMetadata)
                .setUri(viewModel.videoUrl)
                .setMimeType("video/*")
                .build()

            if (!viewModel.isPlayerSetUp) {
                setPlayerMedia(mediaItem)
                viewModel.getActivePlayer()?.prepare()
                viewModel.getActivePlayer()?.playWhenReady = viewModel.isPlaying && isResumed
                viewModel.isPlayerSetUp = true
            }
            viewModel.getActivePlayer()?.seekTo(viewModel.getCurrentVideoTime())
        viewModel.state.onEach {
            when {
                it.activePlayerType == PlayerType.EXO_REGULAR -> {
                    updatePlayerType(viewModel.exoPlayer)
                    showVideoControllerIndefinitely(false)
                }

                it.activePlayerType == PlayerType.CHROME_CAST -> {
                    updatePlayerType(viewModel.getCastPlayer())
                    showVideoControllerIndefinitely(true)
                }
            viewModel.castPlayer?.setSessionAvailabilityListener(
                object : SessionAvailabilityListener {
                    override fun onCastSessionAvailable() {
                        viewModel.logCastConnection(CourseAnalyticsEvent.CAST_CONNECTED)
                        viewModel.isCastActive = true
                        viewModel.exoPlayer?.pause()
                        playerView.player = viewModel.castPlayer
                        viewModel.castPlayer?.setMediaItem(
                            mediaItem,
                            viewModel.exoPlayer?.currentPosition ?: 0L
                        )
                        viewModel.castPlayer?.playWhenReady = true
                        showVideoControllerIndefinitely(true)
                    }

                it.isVideoEnded && !appReviewManager.isDialogShowed -> {
                    appReviewManager.tryToOpenRateDialog()
                }
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)

        enableLongPressDoubleSpeed()
    }
            playerView.setFullscreenButtonClickListener {
                if (viewModel.isCastActive) {
                    return@setFullscreenButtonClickListener
                }

    @OptIn(UnstableApi::class)
    private fun updatePlayerType(player: Player?) {
        with(binding.playerView) {
            this.player = null
            this.player = player
            this.setShowNextButton(false)
            this.setShowPreviousButton(false)
            this.setFullscreenButtonClickListener {
                if (viewModel.enterFullscreen()) {
                    VideoFullScreenFragment.newInstance()
                        .show(childFragmentManager, VideoFullScreenFragment.TAG)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onPause()
    }

    @UnstableApi
    override fun onDestroy() {
        if (!requireActivity().isChangingConfigurations) {
            viewModel.releasePlayers()
        }
        super.onDestroy()
    }

    @UnstableApi
    private fun showVideoControllerIndefinitely(show: Boolean) {
        if (show) {
            binding.playerView.controllerAutoShow = false
            binding.playerView.controllerShowTimeoutMs = 0
        } else {
            binding.playerView.controllerAutoShow = true
            binding.playerView.controllerShowTimeoutMs = CONTROLLER_SHOW_TIMEOUT
        }
        binding.playerView.showController()
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun setPlayerMedia(mediaItem: MediaItem) {
        if (viewModel.videoUrl.endsWith(".m3u8")) {
            val factory = DefaultDataSource.Factory(requireContext())
            val mediaSource: HlsMediaSource =
                HlsMediaSource.Factory(factory).createMediaSource(mediaItem)
            viewModel.exoPlayer?.setMediaSource(mediaSource, viewModel.getCurrentVideoTime())
        } else {
            viewModel.getActivePlayer()?.setMediaItem(
                mediaItem,
                viewModel.getCurrentVideoTime()
            )
        }
    private fun enableLongPressDoubleSpeed() {
        binding.playerView.enableLongPressDoubleSpeed(
            player = viewModel.exoPlayer!!,
            scope = viewLifecycleOwner.lifecycleScope,
            onBadgeVisibilityChange = { binding.doubleSpeedBadge.isVisible = it },
        )
    }

    companion object {
        private const val ARG_BLOCK_ID = "blockId"
        private const val ARG_VIDEO_URL = "videoUrl"
        private const val ARG_TRANSCRIPT_URL = "transcriptUrl"
        private const val ARG_COURSE_ID = "courseId"
        private const val ARG_TITLE = "title"
        private const val ARG_DOWNLOADED = "isDownloaded"

        private const val PLAYER_VIEW_PADDING_DP = 32
        private const val MIN_PLAYER_HEIGHT_DP = 194
        private const val TABLET_PLAYER_HEIGHT_DP = 320
        private const val VIDEO_ASPECT_RATIO_WIDTH = 16f
        private const val VIDEO_ASPECT_RATIO_HEIGHT = 9f
        private const val CONTROLLER_SHOW_TIMEOUT = 2000

        fun newInstance(
            blockId: String,
            courseId: String,
            videoUrl: String,
            transcriptsUrl: Map<String, String>,
            title: String,
            isDownloaded: Boolean,
        ): VideoUnitFragment {
            val fragment = VideoUnitFragment()
            fragment.arguments = bundleOf(
                ARG_BLOCK_ID to blockId,
                ARG_COURSE_ID to courseId,
                ARG_VIDEO_URL to videoUrl,
                ARG_TRANSCRIPT_URL to objectToString(transcriptsUrl),
                ARG_TITLE to title,
                ARG_DOWNLOADED to isDownloaded
            )
            return fragment
        }
    }
}
