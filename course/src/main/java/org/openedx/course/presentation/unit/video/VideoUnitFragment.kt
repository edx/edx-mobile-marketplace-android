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
import org.openedx.core.extension.computeWindowSizeClasses
import org.openedx.core.extension.dpToPixel
import org.openedx.core.extension.objectToString
import org.openedx.core.extension.stringToObject
import org.openedx.core.presentation.dialog.appreview.AppReviewManager
import org.openedx.core.presentation.dialog.selectorbottomsheet.SelectBottomDialogFragment
import org.openedx.core.presentation.global.viewBinding
import org.openedx.core.ui.ConnectionErrorView
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.utils.LocaleUtils
import org.openedx.course.R
import org.openedx.course.databinding.FragmentVideoUnitBinding
import org.openedx.course.presentation.ui.VideoSubtitles
import org.openedx.course.presentation.ui.VideoTitle
import org.openedx.course.presentation.ui.enableLongPressDoubleSpeed
import kotlin.math.roundToInt

class VideoUnitFragment : Fragment(R.layout.fragment_video_unit) {

    private val binding by viewBinding(FragmentVideoUnitBinding::bind)
    private val viewModel by viewModel<EncodedVideoUnitViewModel> {
        parametersOf(
            requireArguments().getString(ARG_COURSE_ID, ""),
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
            viewModel.videoUrl = getString(ARG_VIDEO_URL, "")
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

        val orientation = resources.configuration.orientation
        val windowMetrics =
            WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(requireActivity())
        val currentBounds = windowMetrics.bounds
        val layoutParams = binding.playerView.layoutParams as FrameLayout.LayoutParams
        if (orientation == Configuration.ORIENTATION_PORTRAIT || windowSize?.isTablet == true) {
            val width = currentBounds.width() - requireContext().dpToPixel(32)
            val minHeight = requireContext().dpToPixel(194).roundToInt()
            val height = (width / 16f * 9f).roundToInt()
            layoutParams.height = if (windowSize?.isTablet == true) {
                requireContext().dpToPixel(320).roundToInt()
            } else if (height < minHeight) {
                minHeight
            } else {
                height
            }
        }

        binding.playerView.layoutParams = layoutParams

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

                it.isVideoEnded && !appReviewManager.isDialogShowed -> {
                    appReviewManager.tryToOpenRateDialog()
                }
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)

        enableLongPressDoubleSpeed()
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
            binding.playerView.controllerShowTimeoutMs = 2000
        }
        binding.playerView.showController()
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
