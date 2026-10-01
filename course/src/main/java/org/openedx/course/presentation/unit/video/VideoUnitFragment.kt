package org.openedx.course.presentation.unit.video

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.marginTop
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.ui.AspectRatioFrameLayout
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.extension.computeWindowSizeClasses
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
import org.openedx.course.data.repository.PipBroadcastReceiverManager
import org.openedx.course.data.repository.player.ExoPlayerController
import org.openedx.course.databinding.FragmentVideoUnitBinding
import org.openedx.course.domain.model.PipPlayerType
import org.openedx.course.presentation.ui.VideoSubtitles
import org.openedx.course.presentation.ui.VideoTitle
import org.openedx.course.presentation.ui.enableLongPressDoubleSpeed

class VideoUnitFragment : Fragment(R.layout.fragment_video_unit) {
    private var pictureInPictureParamsBuilder: PictureInPictureParams.Builder? = null
    private var mediaSession: MediaSession? = null
    private var cvVideoTitle: ComposeView? = null
    private val pipViewModel: PipViewModel by viewModel(ownerProducer = { requireActivity() })
    private val pipReceiverManager: PipBroadcastReceiverManager by inject()
    val binding by viewBinding(FragmentVideoUnitBinding::bind)
    private val viewModel by viewModel<EncodedVideoUnitViewModel> {
        parametersOf(
            requireArguments().getString(ARG_COURSE_ID, ""),
            requireArguments().getString(ARG_BLOCK_ID, ""),
            requireArguments().getString(ARG_TITLE, ""),
        )
    }
    private val appReviewManager by inject<AppReviewManager> { parametersOf(requireActivity()) }
    private var windowSize: WindowSize? = null
    private val constraintContainer: ConstraintLayout
        get() = binding.rootLayout as ConstraintLayout
    private var lastVideoAspectRatio: Rational? = null

    private var savedConstraintState: Bundle? = null
    private var savedCardViewParams: ConstraintLayout.LayoutParams? = null
    private var savedSubtitleParams: ConstraintLayout.LayoutParams? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        windowSize = computeWindowSizeClasses()
        lifecycle.addObserver(viewModel)
        requireArguments().apply {
            viewModel.videoUrl = getString(ARG_VIDEO_URL, "")
            viewModel.transcripts = stringToObject<Map<String, String>>(
                getString(ARG_TRANSCRIPT_URL, "")
            ) ?: emptyMap()
        }
        viewModel.downloadSubtitles()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pictureInPictureParamsBuilder = PictureInPictureParams.Builder()
        }

    }

    @RequiresApi(Build.VERSION_CODES.S)
    @OptIn(UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            updateAutoPipForOrientation()
        }

        pipViewModel.pipActions.observe(viewLifecycleOwner) { actions ->
            if (actions.isNotEmpty()) {
                pictureInPictureParamsBuilder?.setActions(actions)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    requireActivity().isInPictureInPictureMode
                ) {
                    requireActivity().setPictureInPictureParams(
                        pictureInPictureParamsBuilder!!.build()
                    )
                }
            }
        }
        pipViewModel.pipState
            .onEach {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    requireActivity().isInPictureInPictureMode
                ) {
                    updatePipActions()
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
        binding.pipBtn.isVisible = true
        cvVideoTitle = ComposeView(requireContext()).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            }
            setContent {
                OpenEdXTheme {
                    VideoTitle(text = viewModel.title)
                }
            }
        }
        constraintContainer.addView(cvVideoTitle)
        updateLayoutForOrientation()
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
                        binding.playerView?.player?.apply {
                            seekTo(it.start.mseconds.toLong())
                            play()
                        }
                    },
                    onSettingsClick = {
                        binding.playerView?.player?.pause()
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
        setupMediaSession()
        binding.connectionError.isVisible =
            !viewModel.hasInternetConnection && !viewModel.isDownloaded
        binding.pipBtn.setOnClickListener {
            enablePipMode()

        }
        binding.playerView?.resizeMode =
            AspectRatioFrameLayout.RESIZE_MODE_FILL

        viewModel.exoPlayer?.addListener(object : Player.Listener {

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    requireActivity().isInPictureInPictureMode
                ) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            viewModel.exoPlayer?.let { player ->
                                if (!player.isPlaying) {
                                    player.play()
                                }
                            }
                            updatePipActions()
                        }
                        Player.STATE_ENDED -> {
                            pipViewModel.updatePlaybackState(isPlaying = false, isEnded = true)
                            showReplayAction()
                        }
                    }
                }
            }

            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val aspect =
                        if (videoSize.height > 0) Rational(videoSize.width, videoSize.height)
                        else Rational(16, 9)
                    lastVideoAspectRatio = aspect
                    
                    // Only set aspect ratio if NOT in PIP mode
                    // In PIP mode, we want to fill the entire available space
                    if (!requireActivity().isInPictureInPictureMode) {
                        pictureInPictureParamsBuilder?.setAspectRatio(aspect)
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                pipViewModel.updatePlaybackState(isPlaying)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    requireActivity().isInPictureInPictureMode
                ) {
                    updatePipActions()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    requireActivity().isInPictureInPictureMode
                ) {
                    retryPlayback()
                }
            }


        })
        lifecycleScope.launchWhenStarted {
            pipViewModel.pipEvent.collect { event ->
                when (event) {
                    is PipUiEvent.PipModeRequested -> {
                        isPipModeRequested = true
                        if (isAdded) {
                            enablePipMode()
                        }
                    }
                    else -> {}
                }
            }
        }
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
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect {
                if (viewModel.hasInternetConnection &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    requireActivity().isInPictureInPictureMode
                ) {
                    retryPlayback()
                }
            }
        }
        enableLongPressDoubleSpeed()
    }

    @OptIn(UnstableApi::class)
    private fun updatePlayerType(player: Player?) {
        with(binding.playerView) {
            this?.player = null
            this?.player = player
            this?.setShowNextButton(false)
            this?.setShowPreviousButton(false)
            this?.controllerHideOnTouch = false
            this?.setFullscreenButtonClickListener {
                if (viewModel.enterFullscreen()) {
                    VideoFullScreenFragment.newInstance()
                        .show(childFragmentManager, VideoFullScreenFragment.TAG)                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        pipReceiverManager.register()
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            requireActivity().isInPictureInPictureMode
        ) {
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            return
        }
        registerExoplayerController()
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    }

    override fun onPause() {
        super.onPause()
        registerExoplayerController()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (requireActivity().isInPictureInPictureMode) {
                requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                return
            }
            if (!pipViewModel.pipState.value.isPipMode && !isPipModeRequested) {
                binding.playerView?.player?.let { player ->
                    if (player.isPlaying) player.pause()
                }
            }
        }
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onStop() {
        super.onStop()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (requireActivity().isInPictureInPictureMode) {
                pipReceiverManager.register()
                viewModel.exoPlayer?.let { player ->
                    if (player.isPlaying) {
                        android.util.Log.d("PipMode", "Playback continuing in background during device lock")
                    }
                }
                return
            }
            pipReceiverManager.unregister()
        } else {
            pipReceiverManager.unregister()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            requireActivity().isInPictureInPictureMode
        ) {
            return
        }

        binding.playerView?.player?.let { player ->
            if (player.isPlaying) player.pause()
        }
    }

    @UnstableApi
    override fun onDestroy() {
        if (!requireActivity().isChangingConfigurations) {
            viewModel.releasePlayers()
            pipViewModel.unregisterPlayer()
        }
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    @UnstableApi
    private fun showVideoControllerIndefinitely(show: Boolean) {
        if (show) {
            binding.playerView.controllerAutoShow = false
            binding.playerView?.controllerShowTimeoutMs = 0
            binding.playerView?.controllerHideOnTouch = true
        } else {
            binding.playerView?.controllerAutoShow = true
            binding.playerView?.controllerShowTimeoutMs = 1000
            binding.playerView?.controllerHideOnTouch = false
        }
        binding.playerView?.showController()
    }

    private fun enableLongPressDoubleSpeed() {
        binding.playerView?.enableLongPressDoubleSpeed(
            player = viewModel.exoPlayer!!,
            scope = viewLifecycleOwner.lifecycleScope,
            onBadgeVisibilityChange = { binding.doubleSpeedBadge?.isVisible = it },
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

    private fun isTablet(): Boolean {
        return windowSize?.isTablet == true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(UnstableApi::class)
    private fun enablePipMode() {
        if (isLandscape()) {
            return
        }

        if (!pipViewModel.isPipPermissionGranted(requireContext())) {
            showPipDisabledMessage()
            return
        }

        // Save the exact current state BEFORE making any changes
        saveConstraintState()

        viewModel.exoPlayer?.let { player ->
            val controller = ExoPlayerController(player)
            pipViewModel.registerPlayer(controller, PipPlayerType.EXOPLAYER)
        }
        binding.subtitles.isVisible = false
        cvVideoTitle?.isVisible = false
        binding.pipBtn.isVisible = false
        pipViewModel.updateButtonVisibility(false)
        binding.playerView?.useController = false

        lastVideoAspectRatio?.let { pictureInPictureParamsBuilder?.setAspectRatio(it) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pictureInPictureParamsBuilder?.setSeamlessResizeEnabled(true)
        }
        updatePipActions()
        pictureInPictureParamsBuilder?.build()?.let {
            requireActivity().enterPictureInPictureMode(it)
        }
    }

    fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun saveConstraintState() {
        // Find and save innerConstraintLayout if it exists (for w600dp-h480dp layout)
        val rootView = binding.rootLayout
        if (rootView.childCount > 0) {
            val firstChild = rootView.getChildAt(0)
            if (firstChild is ConstraintLayout && firstChild.id != binding.cardView.id) {
                innerConstraintLayout = firstChild
                val innerParams = firstChild.layoutParams as? ConstraintLayout.LayoutParams
                if (innerParams != null) {
                    savedInnerLayoutParams = ConstraintLayout.LayoutParams(innerParams)
                }
            }
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

        // Save player layout params
        savedPlayerLayoutParams = (binding.playerView?.layoutParams as? FrameLayout.LayoutParams)?.let {
            FrameLayout.LayoutParams(it)
        }
    }

    private var savedPlayerLayoutParams: FrameLayout.LayoutParams? = null
    private var savedInnerLayoutParams: ConstraintLayout.LayoutParams? = null
    private var innerConstraintLayout: ConstraintLayout? = null


    @OptIn(UnstableApi::class)
    private fun restoreSavedConstraintState() {
        // Restore inner constraint layout parameters if they were saved
        innerConstraintLayout?.let {
            savedInnerLayoutParams?.let { params ->
                val newParams = ConstraintLayout.LayoutParams(params)
                it.layoutParams = newParams
            }
        }

        // Restore cardView parameters first
        savedCardViewParams?.let {
            val newParams = ConstraintLayout.LayoutParams(it)
            binding.cardView.layoutParams = newParams
        }

        // Restore subtitles parameters
        savedSubtitleParams?.let {
            val newParams = ConstraintLayout.LayoutParams(it)
            binding.subtitles.layoutParams = newParams
        }

        // Restore player layout params
        savedPlayerLayoutParams?.let {
            val newParams = FrameLayout.LayoutParams(it)
            binding.playerView?.layoutParams = newParams
        }

        // Restore CardView padding to default
        binding.cardView.setPadding(0, 0, 0, 0)

         // Restore UI visibility and properties
         binding.subtitles.isVisible = true
         binding.pipBtn.isVisible = true
         binding.playerView?.useController = true
         binding.playerView?.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
         binding.playerView?.showController()
         cvVideoTitle?.visibility = View.VISIBLE
         pipViewModel.updateButtonVisibility(true)
         binding.cardView.radius = resources.getDimension(R.dimen.video_corner_radius)

        // Request layout refresh
        binding.rootLayout?.post {
            binding.rootLayout?.requestLayout()
        }
    }

    @OptIn(UnstableApi::class)
    private fun restoreNormalUI() {
        // This is now handled by restoreSavedConstraintState
    }


    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(UnstableApi::class)
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        if (isInPictureInPictureMode) {
            pipViewModel.enterPipMode()
            binding.subtitles.isVisible = false
            binding.pipBtn.isVisible = false
            binding.playerView?.useController = false
            pipViewModel.updateButtonVisibility(false)
            cvVideoTitle?.visibility = View.GONE
            binding.cardView.radius = 0f

            // Ensure rootLayout has no padding
            binding.rootLayout?.setPadding(0, 0, 0, 0)

            // If innerConstraintLayout exists (for tablet layouts), expand it to fill the screen
            innerConstraintLayout?.let { layout ->
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

            // Set player to fill CardView completely
            binding.playerView?.layoutParams = (binding.playerView?.layoutParams as FrameLayout.LayoutParams).apply {
                width = FrameLayout.LayoutParams.MATCH_PARENT
                height = FrameLayout.LayoutParams.MATCH_PARENT
                marginStart = 0
                marginEnd = 0
                topMargin = 0
                bottomMargin = 0
                leftMargin = 0
                rightMargin = 0
            }
            // Use RESIZE_MODE_FILL to stretch video to fill entire PIP view without white bars
            binding.playerView?.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL

            updatePipActions()

            // Apply PIP constraints to fill entire screen
            resetConstraintsForPip()

            // Reset PIP params builder to clear any previously set aspect ratio
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                pictureInPictureParamsBuilder = PictureInPictureParams.Builder()
                // Build params WITHOUT aspect ratio to allow full screen filling
                requireActivity().setPictureInPictureParams(
                    pictureInPictureParamsBuilder!!.build()
                )
            }
            
            // Post delayed layout refresh to ensure PIP window is properly sized
            binding.rootLayout?.postDelayed({
                binding.rootLayout?.requestLayout()
                innerConstraintLayout?.requestLayout()
                binding.cardView.requestLayout()
                binding.playerView?.requestLayout()
            }, 100)

        } else {
            pipViewModel.exitPipMode()
            binding.playerView?.player?.let { player ->
                if (player.isPlaying) player.pause()
            }
            // Restore to exact previous state
            restoreSavedConstraintState()
        }
    }

    private fun resetConstraintsForPip() {
        val set = ConstraintSet()
        set.clone(constraintContainer)

        // Clear all constraints on cardView
        set.clear(binding.cardView.id)

        // Connect to all edges with ZERO spacing
        set.connect(binding.cardView.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0)
        set.connect(binding.cardView.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0)
        set.connect(binding.cardView.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0)
        set.connect(binding.cardView.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0)

        // Fill entire screen without any margins
        set.constrainWidth(binding.cardView.id, ConstraintSet.MATCH_CONSTRAINT)
        set.constrainHeight(binding.cardView.id, ConstraintSet.MATCH_CONSTRAINT)

        // IMPORTANT: Remove dimension ratio to allow proper filling without aspect ratio constraints
        set.setDimensionRatio(binding.cardView.id, null)

        set.applyTo(constraintContainer)

        // Ensure root layout has no padding
        binding.rootLayout?.setPadding(0, 0, 0, 0)
        
        // Force multiple layout passes to ensure proper sizing
        binding.cardView.requestLayout()
        binding.playerView?.requestLayout()
        binding.rootLayout?.requestLayout()
        
        // Post a delayed layout refresh to ensure constraints are fully applied
        binding.rootLayout?.post {
            binding.cardView.requestLayout()
            binding.playerView?.requestLayout()
            binding.rootLayout?.requestLayout()
        }
    }


    private fun clearAllMarginsAndConstraints() {
        if (!isTablet()) {
            val cardParams = binding.cardView.layoutParams as ConstraintLayout.LayoutParams
            cardParams.marginStart = 0
            cardParams.marginEnd = 0
            cardParams.topMargin = 0
            cardParams.bottomMargin = 0
            binding.cardView.layoutParams = cardParams
            val subtitleParams = binding.subtitles.layoutParams as ConstraintLayout.LayoutParams
            subtitleParams.marginStart = 0
            subtitleParams.marginEnd = 0
            subtitleParams.topMargin = 0
            binding.subtitles.layoutParams = subtitleParams
            binding.cardView.requestLayout()
            binding.subtitles.requestLayout()
            binding.rootLayout?.requestLayout()
        }
        else{
            val cardParams = binding.cardView.layoutParams as ConstraintLayout.LayoutParams
            cardParams.marginStart = 0
            cardParams.marginEnd = 0
            cardParams.topMargin = 40.dpToPx()
            cardParams.bottomMargin = 0
            binding.cardView.layoutParams = cardParams
            val subtitleParams = binding.subtitles.layoutParams as ConstraintLayout.LayoutParams
            subtitleParams.marginStart = 0
            subtitleParams.marginEnd = 0
            subtitleParams.topMargin = 24.dpToPx()
            binding.subtitles.layoutParams = subtitleParams
            binding.cardView.requestLayout()
            binding.subtitles.requestLayout()
            binding.rootLayout?.requestLayout()
        }
    }

    @OptIn(UnstableApi::class)
    private fun updateLayoutForOrientation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            requireActivity().isInPictureInPictureMode
        ) {
            return
        }
        val titleView = cvVideoTitle ?: return
        clearAllMarginsAndConstraints()
        val isLandscape =
            resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val constraintSet = androidx.constraintlayout.widget.ConstraintSet()
        constraintSet.clone(constraintContainer)
        val playerHeight = resources.getDimensionPixelSize(R.dimen.player_height)
        val playerMarginH = resources.getDimensionPixelSize(R.dimen.video_margin_horizontal)
        val subtitleMarginH = resources.getDimensionPixelSize(R.dimen.subtitle_margin_horizontal)
        val subtitleMarginBottom = resources.getDimensionPixelSize(R.dimen.subtitle_margin_bottom)
        val subtitleMarginTop = resources.getDimensionPixelSize(R.dimen.subtitle_margin_top)
        constraintSet.clear(binding.cardView.id)
        constraintSet.clear(binding.subtitles.id)

        if (isLandscape) {
            if (isTablet()){
                pipViewModel.updateButtonVisibility(isTablet())
                constraintSet.setVisibility(titleView.id, ConstraintSet.VISIBLE)
                constraintSet.connect(
                    titleView.id,
                    ConstraintSet.TOP,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.TOP,
                    0
                )

                constraintSet.connect(
                    titleView.id,
                    ConstraintSet.START,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.START,
                    250
                )

                constraintSet.connect(
                    titleView.id,
                    ConstraintSet.END,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.END,
                    225
                )

                constraintSet.constrainWidth(
                    titleView.id,
                    ConstraintSet.WRAP_CONTENT
                )

                constraintSet.constrainHeight(
                    titleView.id,
                    ConstraintSet.WRAP_CONTENT
                )

            }
            else {
                constraintSet.setVisibility(titleView.id, ConstraintSet.GONE)
                constraintSet.connect(
                    titleView.id, ConstraintSet.TOP,
                    ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0
                )
                constraintSet.connect(
                    titleView.id, ConstraintSet.START,
                    ConstraintSet.PARENT_ID, ConstraintSet.START, 0
                )
                constraintSet.connect(
                    titleView.id, ConstraintSet.END,
                    ConstraintSet.PARENT_ID, ConstraintSet.END, 0
                )
                constraintSet.constrainWidth(titleView.id, 0)
                constraintSet.constrainHeight(titleView.id, ConstraintSet.WRAP_CONTENT)

                constraintSet.connect(
                    binding.cardView.id,
                    ConstraintSet.START,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.START,
                    8
                )
                constraintSet.connect(
                    binding.cardView.id,
                    ConstraintSet.TOP,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.TOP,
                    0
                )
                constraintSet.connect(
                    binding.cardView.id,
                    ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM,
                    0
                )

                constraintSet.constrainWidth(binding.cardView.id, 0)
                constraintSet.constrainPercentWidth(binding.cardView.id, 0.60f)
                constraintSet.constrainHeight(binding.cardView.id, playerHeight)
                constraintSet.setDimensionRatio(binding.cardView.id, "20:9")
                binding.playerView?.resizeMode =
                    AspectRatioFrameLayout.RESIZE_MODE_FILL
                constraintSet.connect(
                    binding.subtitles.id,
                    ConstraintSet.START,
                    binding.cardView.id,
                    ConstraintSet.END,
                    70
                )
                constraintSet.connect(
                    binding.subtitles.id,
                    ConstraintSet.END,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.END,
                    subtitleMarginH
                )
                constraintSet.connect(
                    binding.subtitles.id,
                    ConstraintSet.TOP,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.TOP,
                    subtitleMarginH
                )
                constraintSet.connect(
                    binding.subtitles.id,
                    ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM,
                    80
                )

                constraintSet.constrainWidth(binding.subtitles.id, 0)
                constraintSet.constrainPercentWidth(binding.subtitles.id, 0.35f)

            }
            binding.pipBtn.visibility = View.GONE

        } else {

            if (isTablet()) {
                pipViewModel.updateButtonVisibility(isTablet())
                // TABLET PORTRAIT: Title centered with 16dp top margin
                constraintSet.setVisibility(titleView.id, ConstraintSet.VISIBLE)
                constraintSet.connect(
                    titleView.id, ConstraintSet.TOP,
                    ConstraintSet.PARENT_ID, ConstraintSet.TOP, 16
                )
                constraintSet.connect(
                    titleView.id, ConstraintSet.START,
                    ConstraintSet.PARENT_ID, ConstraintSet.START, 190
                )
                constraintSet.connect(
                    titleView.id, ConstraintSet.END,
                    ConstraintSet.PARENT_ID, ConstraintSet.END, 185
                )
                constraintSet.connect(
                    binding.cardView.id, ConstraintSet.TOP,
                    ConstraintSet.PARENT_ID, ConstraintSet.TOP, 24
                )
                constraintSet.connect(
                    titleView.id, ConstraintSet.TOP,
                    binding.cardView.id, ConstraintSet.BOTTOM, 24
                )
                constraintSet.connect(
                    binding.cardView.id, ConstraintSet.TOP,
                    titleView.id, ConstraintSet.BOTTOM, 24
                )
            } else {
                constraintSet.setVisibility(titleView.id, ConstraintSet.VISIBLE)

                constraintSet.connect(
                    titleView.id, ConstraintSet.TOP,
                    ConstraintSet.PARENT_ID, ConstraintSet.TOP, 16
                )
                constraintSet.connect(
                    titleView.id, ConstraintSet.START,
                    ConstraintSet.PARENT_ID, ConstraintSet.START, playerMarginH
                )
                constraintSet.connect(
                    titleView.id, ConstraintSet.END,
                    ConstraintSet.PARENT_ID, ConstraintSet.END, playerMarginH
                )
                constraintSet.constrainWidth(titleView.id, 0)
                constraintSet.constrainHeight(titleView.id, ConstraintSet.WRAP_CONTENT)

                constraintSet.connect(
                    binding.cardView.id, ConstraintSet.TOP,
                    titleView.id, ConstraintSet.BOTTOM, 16
                )
                constraintSet.connect(
                    binding.cardView.id, ConstraintSet.START,
                    ConstraintSet.PARENT_ID, ConstraintSet.START, playerMarginH
                )
                constraintSet.connect(
                    binding.cardView.id, ConstraintSet.END,
                    ConstraintSet.PARENT_ID, ConstraintSet.END, playerMarginH
                )
                constraintSet.constrainWidth(binding.cardView.id, 0)
                constraintSet.constrainHeight(binding.cardView.id, playerHeight)
                constraintSet.setDimensionRatio(binding.cardView.id, null)
                binding.playerView?.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM

                constraintSet.connect(
                    binding.subtitles.id,
                    ConstraintSet.TOP,
                    binding.cardView.id,
                    ConstraintSet.BOTTOM,
                    subtitleMarginTop
                )
                constraintSet.connect(
                    binding.subtitles.id,
                    ConstraintSet.START,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.START,
                    subtitleMarginH
                )
                constraintSet.connect(
                    binding.subtitles.id,
                    ConstraintSet.END,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.END,
                    subtitleMarginH
                )
                constraintSet.connect(
                    binding.subtitles.id,
                    ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM,
                    subtitleMarginBottom
                )

                constraintSet.constrainWidth(binding.subtitles.id, 0)
                constraintSet.constrainHeight(binding.subtitles.id, 0)

                constraintSet.constrainWidth(binding.subtitles.id, 0)
                constraintSet.constrainHeight(binding.subtitles.id, 0)

            }
            binding.pipBtn.visibility = View.VISIBLE
        }
        constraintSet.applyTo(constraintContainer)

        binding.rootLayout.post {
            binding.rootLayout.requestLayout()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        binding.rootLayout.postDelayed({
            updateLayoutForOrientation()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !requireActivity().isInPictureInPictureMode
            ) {
                updateAutoPipForOrientation()
            }
        }, 100)

    }

    @OptIn(UnstableApi::class)
    private fun setupMediaSession() {
        viewModel.exoPlayer?.let { player ->
            mediaSession = MediaSession.Builder(requireContext(), player)
                .setId("video_session_${System.currentTimeMillis()}")
                .build()
        }
    }
    private fun retryPlayback() {
        pipViewModel.retryPlayback()
    }

    private fun seekBy(millis: Long) {
        viewModel.exoPlayer?.let {
            val position = it.currentPosition + millis
            it.seekTo(position.coerceAtLeast(0))
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(UnstableApi::class)
    private fun updatePipActions() {
        val player = viewModel.exoPlayer ?: return
        if (player.playbackState == Player.STATE_ENDED) {
            showReplayAction()
            return
        }
        val isPlaying = pipViewModel.pipState.value.isPlaying
        pipViewModel.loadPipActions(requireContext(), isPlaying)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            requireActivity().isInPictureInPictureMode
        ) {
            requireActivity().setPictureInPictureParams(
                pictureInPictureParamsBuilder!!.build()
            )
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun showReplayAction() {
        if (!requireActivity().isInPictureInPictureMode) return
        if (pictureInPictureParamsBuilder == null) return

        val replayIntent = PendingIntent.getBroadcast(
            requireContext(),
            105,
            android.content.Intent(PipBroadcastReceiverManager.ACTION_PLAY),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val replayAction = RemoteAction(
            Icon.createWithResource(requireContext(), R.drawable.ic_play),
            "Replay",
            "Replay Video",
            replayIntent
        )

        pictureInPictureParamsBuilder?.setActions(listOf(replayAction))

        requireActivity().setPictureInPictureParams(
            pictureInPictureParamsBuilder!!.build()
        )
    }

    private fun showPipDisabledMessage() {
        Toast.makeText(
            requireContext(),
            "Enable Picture-in-Picture in app settings to use PiP",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun registerExoplayerController() {
        viewModel.exoPlayer?.let { player ->
            val controller = ExoPlayerController(player)
            pipViewModel.registerPlayer(controller, PipPlayerType.EXOPLAYER)
        }
    }

    private fun isLandscape(): Boolean {
        return resources.configuration.orientation ==
                Configuration.ORIENTATION_LANDSCAPE
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun updateAutoPipForOrientation() {
        val params = PictureInPictureParams.Builder()
            .setAutoEnterEnabled(!isLandscape())
            .setSeamlessResizeEnabled(true)
            .build()

        requireActivity().setPictureInPictureParams(params)
    }


}








