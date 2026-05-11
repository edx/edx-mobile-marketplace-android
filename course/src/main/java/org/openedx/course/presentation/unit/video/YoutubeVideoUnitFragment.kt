@file:SuppressLint("UnsafeOptInUsageError")
@file:androidx.media3.common.util.UnstableApi

package org.openedx.course.presentation.unit.video

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.app.PendingIntent
import android.app.RemoteAction
import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.media3.common.util.UnstableApi
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerTracker
import com.pierfrancescosoffritti.androidyoutubeplayer.core.customui.DefaultPlayerUiController
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.extension.computeWindowSizeClasses
import org.openedx.core.extension.objectToString
import org.openedx.core.extension.stringToObject
import org.openedx.core.presentation.dialog.appreview.AppReviewManager
import org.openedx.core.presentation.dialog.selectorbottomsheet.SelectBottomDialogFragment
import org.openedx.core.ui.ConnectionErrorView
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.utils.LocaleUtils
import org.openedx.course.R
import org.openedx.course.databinding.FragmentYoutubeVideoUnitBinding
import org.openedx.course.presentation.CourseRouter
import org.openedx.course.presentation.ui.VideoSubtitles
import org.openedx.course.presentation.ui.VideoTitle
import org.openedx.course.data.repository.PipBroadcastReceiverManager
import org.openedx.course.data.repository.PipBroadcastReceiverManager.Companion.ACTION_FORWARD
import org.openedx.course.data.repository.PipBroadcastReceiverManager.Companion.ACTION_PAUSE
import org.openedx.course.data.repository.PipBroadcastReceiverManager.Companion.ACTION_PLAY
import org.openedx.course.data.repository.PipBroadcastReceiverManager.Companion.ACTION_REWIND
import org.openedx.course.data.repository.PipBroadcastReceiverManager.Companion.REQUEST_FORWARD
import org.openedx.course.data.repository.PipBroadcastReceiverManager.Companion.REQUEST_PAUSE
import org.openedx.course.data.repository.PipBroadcastReceiverManager.Companion.REQUEST_PLAY
import org.openedx.course.data.repository.PipBroadcastReceiverManager.Companion.REQUEST_REWIND
import org.openedx.course.data.repository.player.YouTubePlayerController
import org.openedx.course.domain.interactor.model.PipPlayerType
import org.openedx.course.presentation.videos.SharedViewModel
import kotlin.getValue
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.core.view.isGone


@UnstableApi
class YoutubeVideoUnitFragment : Fragment(R.layout.fragment_youtube_video_unit) {

    private val sharedViewModel: SharedViewModel by activityViewModels()

    private val viewModel by viewModel<VideoUnitViewModel> {
        parametersOf(
            requireArguments().getString(ARG_COURSE_ID, ""),
            requireArguments().getString(ARG_BLOCK_ID, "")
        )
    }
    private val router by inject<CourseRouter>()
    private val appReviewManager by inject<AppReviewManager> { parametersOf(requireActivity()) }
    private val pipViewModel by viewModel<PipViewModel>()
    private val pipReceiverManager by inject<PipBroadcastReceiverManager>()

    private var _binding: FragmentYoutubeVideoUnitBinding? = null
    private val binding get() = _binding!!

    private var windowSize: WindowSize? = null
    private var _youTubePlayer: YouTubePlayer? = null

    private var blockId = ""

    private var isPlayerInitialized = false
    private var isPipPlayerRegistered = false
    private var isEnteringPip = false

    private var originalCardMargins: Rect? = null
    private var originalCardWidth: Int? = null
    private var originalCardHeight: Int? = null
    private var originalCardTopToTop: Int? = null
    private var originalCardTopToBottom: Int? = null
    private var originalCardBottomToBottom: Int? = null
    private var originalCardBottomToTop: Int? = null
    private var originalCardStartToStart: Int? = null
    private var originalCardEndToEnd: Int? = null
    private var originalCardCornerRadius: Float? = null

    private val youtubeTrackerListener = YouTubePlayerTracker()

    private var _playerUiController: DefaultPlayerUiController? = null




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        windowSize = computeWindowSizeClasses()
        lifecycle.addObserver(viewModel)
        requireArguments().apply {
            viewModel.videoUrl = getString(ARG_VIDEO_URL, "")
            viewModel.transcripts = stringToObject<Map<String, String>>(
                getString(ARG_TRANSCRIPT_URL, "")
            ) ?: emptyMap()
            blockId = getString(ARG_BLOCK_ID, "")
        }
        viewModel.downloadSubtitles()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentYoutubeVideoUnitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (isPipUiActive()) {
            updateUiForPipMode(true)
            return
        }
        val isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        applyOrientationLayout(isLandscape)
    }

    /**
     * Since the Activity uses android:configChanges="orientation|screenSize|...",
     * it never recreates on rotation — so layout-land qualifiers are NEVER
     * auto-applied by Android. We must manually re-apply the correct constraints
     * on every orientation change using ConstraintSet.
     *
     * ConstraintSet.clone(context, R.layout.fragment_youtube_video_unit) will
     * automatically pick the layout-land variant when called from a landscape
     * configuration, because at the time onConfigurationChanged fires, the
     * context/resources already reflect the NEW orientation.
     */
    private fun applyOrientationLayout(isLandscape: Boolean) {
        val rootLayout = view?.findViewById<ConstraintLayout>(R.id.rootLayout) ?: return

        // Load the constraint set from the correct qualifier variant
        // (layout vs layout-land) based on the CURRENT (already-updated) config
        val constraintSet = ConstraintSet()
        constraintSet.clone(requireContext(), R.layout.fragment_youtube_video_unit)
        constraintSet.applyTo(rootLayout)

        if (isLandscape) {
            // cv_video_title is absent from layout-land; hide it explicitly
            binding.cvVideoTitle?.visibility = View.GONE
            // PIP is not available in landscape
            binding.pipBtn?.visibility = View.GONE
        } else {
            binding.cvVideoTitle?.visibility = View.VISIBLE
            // PIP is available in portrait; show it (actual enable check happens elsewhere)
            binding.pipBtn?.visibility = View.VISIBLE
        }
    }

    private fun isPipUiActive(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            (isEnteringPip || requireActivity().isInPictureInPictureMode)
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.isPlaying) {
            _youTubePlayer?.play()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !requireActivity().isInPictureInPictureMode) {
            setContainerChromeVisible(true)
        }

        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR


    }

    @UnstableApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

            parentFragmentManager.setFragmentResultListener(
                "FULLSCREEN_EXIT",
                viewLifecycleOwner
            ) { _, bundle ->

                val resumedTime = bundle.getFloat("time", 0f)

                binding.youtubePlayerView.post {
                    _youTubePlayer?.apply {
                        seekTo(resumedTime)
                        play()
                    }
                }

                viewModel.setCurrentVideoTime((resumedTime * 1000).toLong())
            }
        binding.cvVideoTitle?.setContent {
            OpenEdXTheme {
                VideoTitle(text = requireArguments().getString(ARG_TITLE) ?: "")
            }
        }

        binding.connectionError.setContent {
            OpenEdXTheme {
                ConnectionErrorView {
                    binding.connectionError.isVisible = !viewModel.hasInternetConnection
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
                        _youTubePlayer?.apply {
                            seekTo(it.start.mseconds / 1000f)
                            play()
                        }
                    },
                    onSettingsClick = {
                        val dialog =
                            SelectBottomDialogFragment.newInstance(
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

        binding.connectionError.isVisible = !viewModel.hasInternetConnection

        binding.pipBtn?.setOnClickListener {
            enablePipMode()
        }

        // Apply the correct layout constraints for the current orientation
        // (portrait vs landscape). This covers initial creation in any orientation.
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        applyOrientationLayout(isLandscape)

        val options = IFramePlayerOptions.Builder(requireActivity())
            .controls(0)
            .rel(0)
            .ivLoadPolicy(0)
            .modestBranding(1)
            .build()

        lifecycle.addObserver(binding.youtubePlayerView)



        val listener = object : AbstractYouTubePlayerListener() {
            var isMarkBlockCompletedCalled = false

            override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                super.onVideoDuration(youTubePlayer, duration)
                viewModel.videoDuration = (duration * 1000f).toLong()
            }

            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                super.onCurrentSecond(youTubePlayer, second)
                viewModel.setCurrentVideoTime((second * 1000f).toLong())
                val completePercentage = second / youtubeTrackerListener.videoDuration
                if (completePercentage >= 0.8f && !isMarkBlockCompletedCalled) {
                    viewModel.markBlockCompleted(blockId)
                    isMarkBlockCompletedCalled = true
                }
                if (completePercentage >= 0.99f && !appReviewManager.isDialogShowed) {
                    appReviewManager.tryToOpenRateDialog()
                }
            }

            override fun onStateChange(
                youTubePlayer: YouTubePlayer,
                state: PlayerConstants.PlayerState
            ) {
                // Ignore when fullscreen fragment is open
                if (requireActivity()
                        .supportFragmentManager
                        .findFragmentByTag("FullscreenYoutube") != null
                ) return

                when (state) {
                    PlayerConstants.PlayerState.PLAYING -> {
                        pipViewModel.updatePlaybackState(isPlaying = true, isEnded = false)
                        viewModel.isPlaying = true
                        updatePictureInPictureActions(true)
                    }

                    PlayerConstants.PlayerState.PAUSED -> {
                        pipViewModel.updatePlaybackState(isPlaying = false)
                        viewModel.isPlaying = false
                        updatePictureInPictureActions(false)
                    }

                    PlayerConstants.PlayerState.ENDED -> {
                        pipViewModel.updatePlaybackState(isPlaying = false, isEnded = true)
                        viewModel.isPlaying = false
                        updatePictureInPictureActions(false)

                    }

                    else -> return
                }

            }

            override fun onReady(youTubePlayer: YouTubePlayer) {
                super.onReady(youTubePlayer)

                _youTubePlayer = youTubePlayer

                if (!isPipPlayerRegistered) {
                    pipViewModel.registerPlayer(
                        controller = YouTubePlayerController(youTubePlayer, youtubeTrackerListener),
                        playerType = PipPlayerType.YOUTUBE,
                    )
                    isPipPlayerRegistered = true
                }


                if (_playerUiController == null) {
                    _playerUiController = DefaultPlayerUiController(binding.youtubePlayerView, youTubePlayer)
                }

                //  Attach custom UI
                val controller = _playerUiController ?: return
                controller.rootView.visibility = View.VISIBLE
                binding.youtubePlayerView.setCustomPlayerUi(controller.rootView)

                _playerUiController?.setFullscreenButtonClickListener {

                    val currentTime = viewModel.getCurrentVideoTime() / 1000f

                    val videoId = viewModel.videoUrl.substringAfter("watch?v=")

                    // Pause main player exactly once
                    FullscreenYoutubeFragment.newInstance(
                        videoId = videoId,
                        startTime = currentTime
                    ).show(
                        parentFragmentManager,
                        "FullscreenYoutube"
                    )
                }


                viewModel.videoUrl.split("watch?v=").getOrNull(1)?.let { videoId ->
                    if (viewModel.isPlaying && isResumed) {
                        youTubePlayer.loadVideo(
                            videoId, viewModel.getCurrentVideoTime().toFloat() / 1000
                        )
                    } else {
                        youTubePlayer.cueVideo(
                            videoId, viewModel.getCurrentVideoTime().toFloat() / 1000
                        )
                    }
                }
                youTubePlayer.addListener(youtubeTrackerListener)
                viewModel.logVideoLoadedEvent(viewModel.videoUrl)
            }


            override fun onError(
                youTubePlayer: YouTubePlayer,
                error: PlayerConstants.PlayerError
            ) {
                super.onError(youTubePlayer, error)

                //  HIDE ALL YouTube fallback UI when internet drops
                _playerUiController?.rootView?.visibility = View.GONE
                binding.youtubePlayerView.visibility = View.INVISIBLE

                //  Show your offline UI
                binding.connectionError.isVisible = true

            }

        }


        if (!isPlayerInitialized) {
            binding.youtubePlayerView.initialize(listener, options)
            isPlayerInitialized = true
        }

    }

    override fun onPause() {
        super.onPause()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && (isEnteringPip || requireActivity().isInPictureInPictureMode) && viewModel.isPlaying) {
            binding.youtubePlayerView.post {
                _youTubePlayer?.play()
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        isEnteringPip = false
        updateUiForPipMode(isInPictureInPictureMode)
        if (!isInPictureInPictureMode) {
            val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            applyOrientationLayout(isLandscape)
            setContainerChromeVisible(true)
            pipViewModel.exitPipMode()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && requireActivity().isInPictureInPictureMode) {
            return
        }
        pipReceiverManager.unregister()
    }

    override fun onStart() {
        super.onStart()
        pipReceiverManager.register()
    }

    override fun onDestroyView() {
        isPlayerInitialized = false
        isEnteringPip = false
        setContainerChromeVisible(true)
        _youTubePlayer = null
        pipViewModel.unregisterPlayer()
        isPipPlayerRegistered = false
        super.onDestroyView()
        _binding = null
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    companion object {

        private const val ARG_VIDEO_URL = "videoUrl"
        private const val ARG_TRANSCRIPT_URL = "transcriptUrl"
        private const val ARG_BLOCK_ID = "blockId"
        private const val ARG_COURSE_ID = "courseId"
        private const val ARG_TITLE = "blockTitle"

        fun newInstance(
            blockId: String,
            courseId: String,
            videoUrl: String,
            transcriptsUrl: Map<String, String>,
            blockTitle: String,
        ): YoutubeVideoUnitFragment {
            val fragment = YoutubeVideoUnitFragment()
            fragment.arguments = bundleOf(
                ARG_VIDEO_URL to videoUrl,
                ARG_TRANSCRIPT_URL to objectToString(transcriptsUrl),
                ARG_BLOCK_ID to blockId,
                ARG_COURSE_ID to courseId,
                ARG_TITLE to blockTitle
            )
            return fragment
        }
    }

    @UnstableApi
    private fun enablePipMode() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        binding.cardView.isVisible = true
        binding.youtubePlayerView.isVisible = true
        binding.connectionError.isVisible = false
        _playerUiController?.rootView?.isGone = true
        _youTubePlayer?.play()
        pipViewModel.updatePlaybackState(isPlaying = true, isEnded = false)

        setContainerChromeVisible(false)
        pipViewModel.enterPipMode()
        updateUiForPipMode(true)
        isEnteringPip = true

        // Wait one frame so Android captures the player-focused UI in PiP.
        binding.cardView.post {
            val sourceRectHint = getPipSourceRect(binding.youtubePlayerView)
            val params = PictureInPictureParams.Builder().apply {
                setAspectRatio(Rational(16, 9))
                sourceRectHint?.let { setSourceRectHint(it) }
                setActions(buildPipActions(viewModel.isPlaying))
            }.build()
            requireActivity().setPictureInPictureParams(params)
            val entered = requireActivity().enterPictureInPictureMode(params)
            if (!entered) {
                isEnteringPip = false
                setContainerChromeVisible(true)
                updateUiForPipMode(false)
                pipViewModel.exitPipMode()
                return@post
            }

            // Some devices pause YouTube playback during PiP transition.
            _youTubePlayer?.play()
            pipViewModel.updatePlaybackState(isPlaying = true, isEnded = false)
        }
    }

    private fun updateUiForPipMode(isInPip: Boolean) {
        updatePlayerContainerForPipMode(isInPip)
        binding.cardView.isVisible = true
        binding.youtubePlayerView.isVisible = true
        _playerUiController?.rootView?.isVisible = !isInPip
        binding.cvVideoTitle?.isGone = isInPip
        binding.subtitles.isGone = isInPip
        binding.pipBtn.isGone = isInPip
        binding.connectionError.isVisible = !isInPip && !viewModel.hasInternetConnection
    }


    private fun getPipSourceRect(anchor: View): Rect? {
        val rect = Rect()
        return rect.takeIf { anchor.getGlobalVisibleRect(it) }
    }

    private fun updatePlayerContainerForPipMode(isInPip: Boolean) {
        val layoutParams = binding.cardView.layoutParams as? ConstraintLayout.LayoutParams ?: return
        if (originalCardMargins == null) {
            originalCardMargins = Rect(
                layoutParams.leftMargin,
                layoutParams.topMargin,
                layoutParams.rightMargin,
                layoutParams.bottomMargin,
            )
        }
        if (originalCardCornerRadius == null) {
            originalCardCornerRadius = binding.cardView.radius
        }
        if (originalCardWidth == null) {
            originalCardWidth = layoutParams.width
            originalCardHeight = layoutParams.height
            originalCardTopToTop = layoutParams.topToTop
            originalCardTopToBottom = layoutParams.topToBottom
            originalCardBottomToBottom = layoutParams.bottomToBottom
            originalCardBottomToTop = layoutParams.bottomToTop
            originalCardStartToStart = layoutParams.startToStart
            originalCardEndToEnd = layoutParams.endToEnd
        }

        val originalMargins = originalCardMargins ?: return
        layoutParams.leftMargin = if (isInPip) 0 else originalMargins.left
        layoutParams.topMargin = if (isInPip) 0 else originalMargins.top
        layoutParams.rightMargin = if (isInPip) 0 else originalMargins.right
        layoutParams.bottomMargin = if (isInPip) 0 else originalMargins.bottom
        layoutParams.width = if (isInPip) ViewGroup.LayoutParams.MATCH_PARENT else (originalCardWidth ?: layoutParams.width)
        layoutParams.height = if (isInPip) ViewGroup.LayoutParams.MATCH_PARENT else (originalCardHeight ?: layoutParams.height)
        layoutParams.topToTop = if (isInPip) ConstraintLayout.LayoutParams.PARENT_ID else (originalCardTopToTop ?: ConstraintLayout.LayoutParams.UNSET)
        layoutParams.topToBottom = if (isInPip) ConstraintLayout.LayoutParams.UNSET else (originalCardTopToBottom ?: ConstraintLayout.LayoutParams.UNSET)
        layoutParams.bottomToBottom = if (isInPip) ConstraintLayout.LayoutParams.PARENT_ID else (originalCardBottomToBottom ?: ConstraintLayout.LayoutParams.UNSET)
        layoutParams.bottomToTop = if (isInPip) ConstraintLayout.LayoutParams.UNSET else (originalCardBottomToTop ?: ConstraintLayout.LayoutParams.UNSET)
        layoutParams.startToStart = if (isInPip) ConstraintLayout.LayoutParams.PARENT_ID else (originalCardStartToStart ?: ConstraintLayout.LayoutParams.UNSET)
        layoutParams.endToEnd = if (isInPip) ConstraintLayout.LayoutParams.PARENT_ID else (originalCardEndToEnd ?: ConstraintLayout.LayoutParams.UNSET)
        binding.cardView.layoutParams = layoutParams
        binding.cardView.radius = if (isInPip) 0f else (originalCardCornerRadius ?: 0f)
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.O)
    private fun buildPipActions(isPlaying: Boolean): List<RemoteAction> {
        return listOf(
            createPipAction(
                action = ACTION_REWIND,
                requestCode = REQUEST_REWIND,
                titleRes = R.string.course_pip_rewind,
                iconRes = android.R.drawable.ic_media_rew,
            ),
            createPipAction(
                action = if (isPlaying) ACTION_PAUSE else ACTION_PLAY,
                requestCode = if (isPlaying) REQUEST_PAUSE else REQUEST_PLAY,
                titleRes = if (isPlaying) R.string.course_pip_pause else R.string.course_pip_play,
                iconRes = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
            ),
            createPipAction(
                action = ACTION_FORWARD,
                requestCode = REQUEST_FORWARD,
                titleRes = R.string.course_pip_forward,
                iconRes = android.R.drawable.ic_media_ff,
            ),
        )
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.O)
    private fun createPipAction(
        action: String,
        requestCode: Int,
        titleRes: Int,
        iconRes: Int,
    ): RemoteAction {
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            requestCode,
            Intent(action).setPackage(requireContext().packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return RemoteAction(
            Icon.createWithResource(requireContext(), iconRes),
            getString(titleRes),
            getString(titleRes),
            pendingIntent,
        )
    }

    private fun updatePictureInPictureActions(isPlaying: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !requireActivity().isInPictureInPictureMode) return
        val sourceRectHint = getPipSourceRect(binding.youtubePlayerView)
        val params = PictureInPictureParams.Builder().apply {
            setAspectRatio(Rational(16, 9))
            sourceRectHint?.let { setSourceRectHint(it) }
            setActions(buildPipActions(isPlaying))
        }.build()
        requireActivity().setPictureInPictureParams(params)
    }

    private fun setContainerChromeVisible(isVisible: Boolean) {
        sharedViewModel.buttonVisibility.value = isVisible
    }








}











