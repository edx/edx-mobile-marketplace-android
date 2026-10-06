package org.openedx.course.presentation.unit.video

import android.app.AppOpsManager
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
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
import org.openedx.course.data.repository.PipBroadcastReceiverManager
import org.openedx.course.data.repository.player.YouTubePlayerController
import org.openedx.course.databinding.FragmentYoutubeVideoUnitBinding
import org.openedx.course.domain.model.PipPlayerType
import org.openedx.course.presentation.ui.VideoSubtitles
import org.openedx.course.presentation.ui.VideoTitle
import kotlin.getValue
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class YoutubeVideoUnitFragment : Fragment(R.layout.fragment_youtube_video_unit) {

    private val viewModel by viewModel<VideoUnitViewModel> {
        parametersOf(
            requireArguments().getString(ARG_COURSE_ID, ""),
            requireArguments().getString(ARG_BLOCK_ID, "")
        )
    }
    private val appReviewManager by inject<AppReviewManager> { parametersOf(requireActivity()) }

    private val pipViewModel: PipViewModel by viewModel(ownerProducer = { requireActivity() })

    private val pipReceiverManager: PipBroadcastReceiverManager by inject()

    private var ytController: YouTubePlayerController? = null

    private var _binding: FragmentYoutubeVideoUnitBinding? = null
    private val binding get() = _binding!!

    private var windowSize: WindowSize? = null
    private var _youTubePlayer: YouTubePlayer? = null

    private var blockId = ""

    private var isPlayerInitialized = false

    private val youtubeTrackerListener = YouTubePlayerTracker()

    private var _playerUiController: DefaultPlayerUiController? = null
    private var ignoringNextOrientation = false

    // PIP mode state variables
    private var savedCardViewParams: ConstraintLayout.LayoutParams? = null
    private var savedSubtitleParams: ConstraintLayout.LayoutParams? = null
    private var savedInnerLayoutParams: ConstraintLayout.LayoutParams? = null
    private var savedPlayerViewParams: ViewGroup.LayoutParams? = null
    private var innerYoutubeConstraintLayout: ConstraintLayout? = null
    private var savedConstraintSet: ConstraintSet? = null
    private var savedOrientation: Int = Configuration.ORIENTATION_PORTRAIT




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

    override fun onResume() {
        super.onResume()
        if (viewModel.isPlaying) {
            _youTubePlayer?.play()
        }

        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val isTablet = windowSize?.isTablet == true
        if(isLandscape&&!isTablet) {
            binding.pipBtn.isVisible = false
        }
        else{
            binding.pipBtn.isVisible = true
        }


        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pipViewModel.pipState
            .onEach {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    requireActivity().isInPictureInPictureMode
                ) {
                    // Always update PIP actions when state changes
                    binding.rootLayout.post {
                        updatePipActions()
                    }
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

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


        updateLayoutForOrientation()

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
                        _youTubePlayer?.pause()
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

        binding.pipBtn.isVisible = true

        binding.pipBtn.setOnClickListener {
            enablePipMode()
        }

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
                ytController?.updateTime(second, youtubeTrackerListener.videoDuration)
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
                if (requireActivity()
                        .supportFragmentManager
                        .findFragmentByTag("FullscreenYoutube") != null
                ) return

                when (state) {
                    PlayerConstants.PlayerState.PLAYING -> {
                        ytController?.updateState(isPlaying = true, isEnded = false)
                        pipViewModel.updatePlaybackState(isPlaying = true, isEnded = false)
                        viewModel.isPlaying = true
                    }

                    PlayerConstants.PlayerState.PAUSED -> {
                        ytController?.updateState(isPlaying = false, isEnded = false)
                        pipViewModel.updatePlaybackState(isPlaying = false)
                        viewModel.isPlaying = false
                    }

                    PlayerConstants.PlayerState.ENDED -> {
                        ytController?.updateState(isPlaying = false, isEnded = true)
                        pipViewModel.updatePlaybackState(isPlaying = false, isEnded = true)
                        viewModel.isPlaying = false
                        updatePipActions()
                    }

                    else -> return
                }

                updatePipActions()
            }

            override fun onReady(youTubePlayer: YouTubePlayer) {
                super.onReady(youTubePlayer)

                _youTubePlayer = youTubePlayer

                ytController = YouTubePlayerController(youTubePlayer)
                pipViewModel.registerPlayer(ytController!!, PipPlayerType.YOUTUBE)


                if (_playerUiController == null) {
                    _playerUiController = DefaultPlayerUiController(binding.youtubePlayerView, youTubePlayer)
                }

                val controller = _playerUiController ?: return
                controller.rootView.visibility = View.VISIBLE
                binding.youtubePlayerView.setCustomPlayerUi(controller.rootView)

                _playerUiController?.setFullscreenButtonClickListener {

                    val currentTime = viewModel.getCurrentVideoTime() / 1000f

                    val videoId = viewModel.videoUrl.substringAfter("watch?v=")

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

                _playerUiController?.rootView?.visibility = View.GONE
                binding.youtubePlayerView.visibility = View.VISIBLE

            }

        }


        if (!isPlayerInitialized) {
            binding.youtubePlayerView.initialize(listener, options)
            isPlayerInitialized = true
        }
    }

    override fun onStart() {
        super.onStart()
        // Register receiver early to ensure it's ready for PIP actions
        pipReceiverManager.register()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        if (!isInPipMode()) {
            pipReceiverManager.unregister()
        }
    }

    override fun onDestroyView() {
        pipReceiverManager.unregister()
        isPlayerInitialized = false
        _youTubePlayer = null
        ytController = null
        pipViewModel.unregisterPlayer()
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

    @OptIn(UnstableApi::class)
    private fun enablePipMode() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (!requireContext().isPipPermissionGranted()) {
            showPipDisabledMessage()
            return
        }

        // Save the exact current state BEFORE making any changes
        saveConstraintState()

        try {
            // Ensure receiver is registered before entering PIP mode
            pipReceiverManager.register()

            // Register the player with the ViewModel so actions work
            ytController?.let { controller ->
                pipViewModel.registerPlayer(controller, PipPlayerType.YOUTUBE)
            }

            // Build PIP params WITH actions BEFORE entering PIP mode
            val pipState = pipViewModel.pipState.value
            val showPlay = !pipState.isPlaying

            val iconRes = if (showPlay) {
                R.drawable.ic_play
            } else {
                R.drawable.ic_pause
            }

            val title = getString(
                if (showPlay)
                    androidx.media3.ui.R.string.exo_controls_play_description
                else
                    androidx.media3.ui.R.string.exo_controls_pause_description
            )

            val intent = if (showPlay) {
                Intent(PipBroadcastReceiverManager.ACTION_PLAY)
            } else {
                Intent(PipBroadcastReceiverManager.ACTION_PAUSE)
            }

            // Use unique request IDs from PipBroadcastReceiverManager
            val requestId = if (showPlay) {
                PipBroadcastReceiverManager.REQUEST_PLAY
            } else {
                PipBroadcastReceiverManager.REQUEST_PAUSE
            }

            val pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                requestId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val action = RemoteAction(
                Icon.createWithResource(requireContext(), iconRes),
                title,
                title,
                pendingIntent
            )

            // Create params WITH actions
            val pipParams = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .setActions(listOf(action))
                .build()

            requireActivity().enterPictureInPictureMode(pipParams)
        } catch (e: Exception) {
            android.util.Log.e("YoutubeVideoUnit", "Error entering PIP mode", e)
        }
    }

    private fun saveConstraintState() {
        try {
            // Save current orientation
            savedOrientation = resources.configuration.orientation

            // Find and save innerConstraintLayout if it exists (for w600dp-h480dp layout)
            val rootView = binding.root
            if (rootView is ViewGroup && rootView.childCount > 0) {
                val firstChild = rootView.getChildAt(0)
                if (firstChild is ConstraintLayout && firstChild.id != binding.cardView.id) {
                    innerYoutubeConstraintLayout = firstChild
                    val innerParams = firstChild.layoutParams as? ConstraintLayout.LayoutParams
                    if (innerParams != null) {
                        savedInnerLayoutParams = ConstraintLayout.LayoutParams(innerParams)
                    }
                }
            }

            // Save FULL constraint set from target layout
            val targetLayout = innerYoutubeConstraintLayout ?: (binding.rootLayout as? ConstraintLayout)
            if (targetLayout is ConstraintLayout) {
                savedConstraintSet = ConstraintSet()
                savedConstraintSet!!.clone(targetLayout)
            }

            // Save cardView layout params with all properties
            val cardParams = binding.cardView.layoutParams as? ConstraintLayout.LayoutParams
            if (cardParams != null) {
                savedCardViewParams = ConstraintLayout.LayoutParams(cardParams)
            }

            // Save subtitles layout params
            val subParams = binding.subtitles.layoutParams as? ConstraintLayout.LayoutParams
            if (subParams != null) {
                savedSubtitleParams = ConstraintLayout.LayoutParams(subParams)
            }

            // Save playerView layout params safely only if they're valid
            binding.youtubePlayerView.layoutParams?.let { params ->
                if (params.width > 0 && params.height > 0) {
                    savedPlayerViewParams = ViewGroup.LayoutParams(params.width, params.height)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("YoutubeVideoUnit", "Error saving constraint state", e)
        }
    }

    @OptIn(UnstableApi::class)
    private fun restoreSavedConstraintState() {
        try {
            // Restore inner constraint layout parameters if they were saved
            innerYoutubeConstraintLayout?.let {
                savedInnerLayoutParams?.let { params ->
                    val newParams = ConstraintLayout.LayoutParams(params)
                    it.layoutParams = newParams
                }
            }

            // Restore cardView parameters
            savedCardViewParams?.let {
                val newParams = ConstraintLayout.LayoutParams(it)
                binding.cardView.layoutParams = newParams
            }

            // Restore subtitles parameters
            savedSubtitleParams?.let {
                val newParams = ConstraintLayout.LayoutParams(it)
                binding.subtitles.layoutParams = newParams
            }

            // Restore playerView layout params safely
            savedPlayerViewParams?.let { params ->
                if (params.width > 0 && params.height > 0) {
                    binding.youtubePlayerView.layoutParams = ViewGroup.LayoutParams(params.width, params.height)
                }
            }

            // Restore CardView padding to default
            binding.cardView.setPadding(0, 0, 0, 0)
            binding.youtubePlayerView.setPadding(0, 0, 0, 0)

            // Restore UI visibility and properties - CRITICAL
            binding.subtitles.visibility = View.VISIBLE
            binding.pipBtn.visibility = View.VISIBLE
            binding.cvVideoTitle.visibility = View.VISIBLE
            binding.cardView.radius = resources.getDimension(R.dimen.card_corner_radius)

            // Show player UI controller - CRITICAL for play/pause buttons
            _playerUiController?.let { controller ->
                controller.rootView.visibility = View.VISIBLE
                binding.youtubePlayerView.setCustomPlayerUi(controller.rootView)
            }

            // Explicitly ensure player view is visible
            binding.youtubePlayerView.visibility = View.VISIBLE

            // Update button visibility
            pipViewModel.updateButtonVisibility(true)

            // Request layout refresh but don't override with orientation constraints
            binding.rootLayout.post {
                binding.rootLayout.requestLayout()
            }
        } catch (e: Exception) {
            android.util.Log.e("YoutubeVideoUnit", "Error restoring constraint state", e)
        }
    }


    @OptIn(UnstableApi::class)
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        if (isInPictureInPictureMode) {
            pipViewModel.enterPipMode()
            // Keep controller visible so play/pause buttons show
            // Do NOT hide the controller rootView
            binding.subtitles.visibility = View.GONE
            binding.pipBtn.visibility = View.GONE
            pipViewModel.updateButtonVisibility(false)
            binding.cvVideoTitle.visibility = View.GONE
            binding.cardView.radius = 0f

            // Ensure ALL layouts have no padding
            binding.rootLayout.setPadding(0, 0, 0, 0)
            if (innerYoutubeConstraintLayout != null) {
                innerYoutubeConstraintLayout!!.setPadding(0, 0, 0, 0)
            }

            // If innerConstraintLayout exists (for tablet layouts), expand it to fill the screen
            innerYoutubeConstraintLayout?.let { layout ->
                (layout.layoutParams as ConstraintLayout.LayoutParams).apply {
                    width = ConstraintLayout.LayoutParams.MATCH_PARENT
                    height = ConstraintLayout.LayoutParams.MATCH_PARENT
                    marginStart = 0
                    marginEnd = 0
                    topMargin = 0
                    bottomMargin = 0
                    leftMargin = 0
                    rightMargin = 0
                    layout.layoutParams = this
                }
            }

            // CRITICAL: Remove ALL padding and margins from CardView
            binding.cardView.setPadding(0, 0, 0, 0)

            // Update CardView layout params - remove all margins
            (binding.cardView.layoutParams as ConstraintLayout.LayoutParams).apply {
                width = ConstraintLayout.LayoutParams.MATCH_PARENT
                height = ConstraintLayout.LayoutParams.MATCH_PARENT
                marginStart = 0
                marginEnd = 0
                topMargin = 0
                bottomMargin = 0
                leftMargin = 0
                rightMargin = 0
                binding.cardView.layoutParams = this
            }

            // CRITICAL: Remove padding and margins from playerView to fill cardView completely
            binding.youtubePlayerView.setPadding(0, 0, 0, 0)
            binding.youtubePlayerView.layoutParams = binding.youtubePlayerView.layoutParams?.apply {
                width = ViewGroup.LayoutParams.MATCH_PARENT
                height = ViewGroup.LayoutParams.MATCH_PARENT
            }

            // Apply PIP constraints to fill entire screen
            resetConstraintsForPip()

            // Post delayed layout refresh to ensure PIP window is properly sized
            binding.rootLayout.postDelayed({
                binding.rootLayout.requestLayout()
                innerYoutubeConstraintLayout?.requestLayout()
                binding.cardView.requestLayout()
                binding.youtubePlayerView.requestLayout()

                // Update PIP actions after layout is complete
                updatePipActions()
            }, 150)

        } else {
            pipViewModel.exitPipMode()
            try {
                // Show the controller again - CRITICAL for play/pause buttons
                _playerUiController?.let { controller ->
                    controller.rootView.visibility = View.VISIBLE
                    binding.youtubePlayerView.setCustomPlayerUi(controller.rootView)
                }

                // Ensure player view is visible
                binding.youtubePlayerView.visibility = View.VISIBLE

                // Restore to exact previous state BEFORE any visibility changes
                restoreSavedConstraintState()

                // IMPORTANT: Restore constraint set AND reapply orientation-specific layout
                // This ensures video returns to proper size with correct orientation
                binding.rootLayout.postDelayed({
                    try {
                        val targetLayout = innerYoutubeConstraintLayout ?: (binding.rootLayout as? ConstraintLayout)
                        if (targetLayout is ConstraintLayout && savedConstraintSet != null) {
                            // First restore the saved constraint set
                            savedConstraintSet!!.applyTo(targetLayout)
                            android.util.Log.d("YoutubeVideoUnit", "Constraint set restored")
                        }

                        // Then immediately reapply orientation layout to ensure proper sizing
                        binding.rootLayout.post {
                            updateLayoutForOrientation()
                        }

                        // Force layout refresh
                        binding.rootLayout.requestLayout()
                        binding.cardView.requestLayout()
                        binding.youtubePlayerView.requestLayout()
                        targetLayout?.requestLayout()

                        android.util.Log.d("YoutubeVideoUnit", "Layout refresh and orientation update completed")
                    } catch (e: Exception) {
                        android.util.Log.e("YoutubeVideoUnit", "Error restoring constraints", e)
                    }
                }, 250)
            } catch (e: Exception) {
                android.util.Log.e("YoutubeVideoUnit", "Error exiting PIP mode", e)
            }
        }
    }

    private fun isInPipMode(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requireActivity().isInPictureInPictureMode
        } else {
            false
        }
    }

    @OptIn(UnstableApi::class)
    private fun updatePipActions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            requireActivity().isInPictureInPictureMode
        ) {
            try {
                val pipState = pipViewModel.pipState.value
                val isPlaying = pipState.isPlaying
                val showPlay = !isPlaying

                val iconRes = if (showPlay) {
                    R.drawable.ic_play
                } else {
                    R.drawable.ic_pause
                }

                val title = getString(
                    if (showPlay)
                        androidx.media3.ui.R.string.exo_controls_play_description
                    else
                        androidx.media3.ui.R.string.exo_controls_pause_description
                )

                val intent = if (showPlay) {
                    Intent(PipBroadcastReceiverManager.ACTION_PLAY)
                } else {
                    Intent(PipBroadcastReceiverManager.ACTION_PAUSE)
                }

                // Use unique request IDs from PipBroadcastReceiverManager
                val requestId = if (showPlay) {
                    PipBroadcastReceiverManager.REQUEST_PLAY
                } else {
                    PipBroadcastReceiverManager.REQUEST_PAUSE
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    requireContext(),
                    requestId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val action = RemoteAction(
                    Icon.createWithResource(requireContext(), iconRes),
                    title,
                    title,
                    pendingIntent
                )

                val pipParams = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .setActions(listOf(action))
                    .build()

                requireActivity().setPictureInPictureParams(pipParams)
                android.util.Log.d("YoutubeVideoUnit", "PIP actions updated: showPlay=$showPlay, requestId=$requestId")
            } catch (e: Exception) {
                android.util.Log.e("YoutubeVideoUnit", "Error updating PIP actions", e)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)


        if (ignoringNextOrientation) {
            ignoringNextOrientation = false
            return
        }

        if (_binding == null) return
        binding.rootLayout.post {
            updateLayoutForOrientation()
        }
    }

    private fun updateLayoutForOrientation() {
        if (_binding == null) return
        if (isInPipMode()) return

        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val isTablet = windowSize?.isTablet == true

        binding.cvVideoTitle?.visibility = if (isLandscape) View.GONE else View.VISIBLE

        // Use inner ConstraintLayout if it exists (for w600dp-h480dp), otherwise use rootLayout
        val targetLayout: ConstraintLayout? = if (innerYoutubeConstraintLayout != null) {
            innerYoutubeConstraintLayout
        } else {
            binding.rootLayout
        }

        if (targetLayout == null) return

        val constraintSet = ConstraintSet()
        constraintSet.clone(targetLayout)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            requireActivity().isInPictureInPictureMode
        ) return

        val playerHeight = if (isTablet) {
            250 // dp for tablet
        } else {
            resources.getDimensionPixelSize(R.dimen.player_height)
        }

        val playerWidth = if (isTablet) {
            560 // dp for tablet
        } else {
            0 // Use constraint width for mobile
        }

        val playerMarginH = resources.getDimensionPixelSize(R.dimen.video_margin_horizontal)
        val subtitleMarginH = resources.getDimensionPixelSize(R.dimen.subtitle_margin_horizontal)
        val subtitleMarginBottom = resources.getDimensionPixelSize(R.dimen.subtitle_margin_bottom)
        val subtitleMarginTop = resources.getDimensionPixelSize(R.dimen.subtitle_margin_top)
        val titleMarginTop = resources.getDimensionPixelSize(R.dimen.video_title_margin_top)
        val titleMarginH = resources.getDimensionPixelSize(R.dimen.video_title_margin_horizontal)
        val titleToVideoMargin = resources.getDimensionPixelSize(R.dimen.video_title_to_video_margin)

        constraintSet.clear(binding.cardView.id)
        constraintSet.clear(binding.subtitles.id)

        if (isLandscape) {
            // MOBILE ONLY: Side-by-side layout in landscape
            if (!isTablet) {
                constraintSet.connect(binding.cardView.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 8)
                constraintSet.connect(binding.cardView.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0)
                constraintSet.connect(binding.cardView.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0)

                constraintSet.constrainWidth(binding.cardView.id, 0)
                constraintSet.constrainPercentWidth(binding.cardView.id, 0.65f)
                constraintSet.constrainHeight(binding.cardView.id, playerHeight)
                constraintSet.setDimensionRatio(binding.cardView.id, "20:9")

                constraintSet.connect(binding.subtitles.id, ConstraintSet.START, binding.cardView.id, ConstraintSet.END, 70)
                constraintSet.connect(binding.subtitles.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, subtitleMarginH)
                constraintSet.connect(binding.subtitles.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, subtitleMarginH)
                constraintSet.connect(binding.subtitles.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 80)

                constraintSet.constrainWidth(binding.subtitles.id, 0)
                constraintSet.constrainPercentWidth(binding.subtitles.id, 0.35f)
                binding.pipBtn.visibility = View.GONE
            } else {
                // TABLET LANDSCAPE: Keep portrait-style stacked layout
                binding.cvVideoTitle?.let { titleView ->
                    constraintSet.clear(titleView.id)
                    constraintSet.connect(titleView.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, titleMarginTop)
                    constraintSet.connect(binding.cvVideoTitle.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, titleMarginH)
                    constraintSet.connect(binding.cvVideoTitle.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, titleMarginH)
                    constraintSet.constrainWidth(titleView.id, resources.getDimensionPixelSize(R.dimen.video_title_width_tablet_landscape))
                    constraintSet.constrainHeight(titleView.id, ConstraintSet.WRAP_CONTENT)
                }

                constraintSet.connect(binding.cardView.id, ConstraintSet.TOP, binding.cvVideoTitle.id, ConstraintSet.BOTTOM, titleToVideoMargin)
                constraintSet.connect(binding.cardView.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, playerMarginH)
                constraintSet.connect(binding.cardView.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, playerMarginH)

                constraintSet.constrainWidth(binding.cardView.id, resources.getDimensionPixelSize(R.dimen.tablet_video_player_width))
                constraintSet.constrainHeight(binding.cardView.id, resources.getDimensionPixelSize(R.dimen.tablet_video_player_height))
                constraintSet.setDimensionRatio(binding.cardView.id, null) // Remove ratio constraint for fixed dimensions

                constraintSet.connect(binding.subtitles.id, ConstraintSet.TOP, binding.cardView.id, ConstraintSet.BOTTOM, subtitleMarginTop)
                constraintSet.connect(binding.subtitles.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, subtitleMarginH)
                constraintSet.connect(binding.subtitles.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, subtitleMarginH)
                constraintSet.connect(binding.subtitles.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, subtitleMarginBottom)


                constraintSet.constrainWidth(binding.subtitles.id, 0)
                constraintSet.constrainHeight(binding.subtitles.id, 0)
                binding.pipBtn.visibility = View.INVISIBLE
            }

        } else {
            // PORTRAIT MODE: Same for both mobile and tablet
            binding.cvVideoTitle?.let { titleView ->
                constraintSet.clear(titleView.id)
                constraintSet.connect(titleView.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, titleMarginTop)
                constraintSet.connect(titleView.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, titleMarginH)
                constraintSet.connect(titleView.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, titleMarginH)
                constraintSet.constrainWidth(titleView.id, 0)
                constraintSet.constrainHeight(titleView.id, ConstraintSet.WRAP_CONTENT)
            }

            constraintSet.connect(binding.cardView.id, ConstraintSet.TOP, binding.cvVideoTitle.id, ConstraintSet.BOTTOM, titleToVideoMargin)
            constraintSet.connect(binding.cardView.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, playerMarginH)
            constraintSet.connect(binding.cardView.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, playerMarginH)

            if (isTablet) {
                // For tablet: use fixed dimensions (560dp width, 250dp height)
                constraintSet.constrainWidth(binding.cardView.id, (560 * resources.displayMetrics.density).toInt())
                constraintSet.constrainHeight(binding.cardView.id, (300 * resources.displayMetrics.density).toInt())
                constraintSet.setDimensionRatio(binding.cardView.id, null) // Remove ratio constraint for fixed dimensions

                constraintSet.connect(binding.cvVideoTitle.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, resources.getDimensionPixelSize(R.dimen.tablet_video_title_start_margin))
                constraintSet.connect(binding.cvVideoTitle.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, resources.getDimensionPixelSize(R.dimen.tablet_video_title_end_margin))
            } else {
                // For mobile: keep original behavior (constraint width and height based on playerHeight)
                constraintSet.constrainWidth(binding.cardView.id, 0)
                constraintSet.constrainHeight(binding.cardView.id, playerHeight)
                constraintSet.setDimensionRatio(binding.cardView.id, "16:9")
            }

            constraintSet.connect(binding.subtitles.id, ConstraintSet.TOP, binding.cardView.id, ConstraintSet.BOTTOM, subtitleMarginTop)
            constraintSet.connect(binding.subtitles.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, subtitleMarginH)
            constraintSet.connect(binding.subtitles.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, subtitleMarginH)
            constraintSet.connect(binding.subtitles.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, subtitleMarginBottom)

            constraintSet.constrainWidth(binding.subtitles.id, 0)
            constraintSet.constrainHeight(binding.subtitles.id, 0)
            binding.pipBtn.visibility = View.VISIBLE

        }

        constraintSet.applyTo(targetLayout)

        binding.rootLayout.post { binding.rootLayout.requestLayout() }
    }

    private fun resetConstraintsForPip() {
        // Use inner ConstraintLayout if it exists (for w600dp-h480dp), otherwise use rootLayout
        val targetLayout: ConstraintLayout? = if (innerYoutubeConstraintLayout != null) {
            innerYoutubeConstraintLayout
        } else {
            binding.rootLayout
        }

        if (targetLayout == null) return

        val set = ConstraintSet()
        set.clone(targetLayout)

        set.clear(binding.cardView.id)

        // Connect to all edges with ZERO spacing - CRITICAL for no white bars
        set.connect(
            binding.cardView.id, ConstraintSet.TOP,
            ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0
        )
        set.connect(
            binding.cardView.id, ConstraintSet.START,
            ConstraintSet.PARENT_ID, ConstraintSet.START, 0
        )
        set.connect(
            binding.cardView.id, ConstraintSet.END,
            ConstraintSet.PARENT_ID, ConstraintSet.END, 0
        )
        set.connect(
            binding.cardView.id, ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0
        )

        // Fill entire screen without any margins
        set.constrainWidth(binding.cardView.id, ConstraintSet.MATCH_CONSTRAINT)
        set.constrainHeight(binding.cardView.id, ConstraintSet.MATCH_CONSTRAINT)

        // Remove dimension ratio to allow full screen filling without white bars
        set.setDimensionRatio(binding.cardView.id, null)

        set.applyTo(targetLayout)

        // Force immediate layout pass
        binding.cardView.requestLayout()
        binding.youtubePlayerView.requestLayout()
        targetLayout.requestLayout()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun Context.isPipPermissionGranted(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun showPipDisabledMessage() {
        Toast.makeText(
            requireContext(),
            "Enable Picture-in-Picture in app settings to use PiP",
            Toast.LENGTH_LONG
        ).show()
    }

}