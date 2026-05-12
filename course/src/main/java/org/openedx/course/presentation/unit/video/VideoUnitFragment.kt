package org.openedx.course.presentation.unit.video

import android.app.AppOpsManager
import android.app.PictureInPictureParams
import android.app.PendingIntent
import android.app.RemoteAction
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.util.Rational
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
import org.openedx.course.databinding.FragmentVideoUnitBinding
import org.openedx.course.presentation.ui.VideoSubtitles
import org.openedx.course.presentation.ui.VideoTitle
import org.openedx.course.presentation.ui.enableLongPressDoubleSpeed
import org.openedx.course.presentation.videos.SharedViewModel
import org.openedx.course.data.repository.PipBroadcastReceiverManager
import org.openedx.course.data.repository.player.ExoPlayerController
import org.openedx.course.domain.interactor.model.PipPlayerType
import kotlin.math.max
import kotlin.math.min

@UnstableApi
class VideoUnitFragment : Fragment(R.layout.fragment_video_unit) {
    private var pictureInPictureParamsBuilder: PictureInPictureParams.Builder? = null
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var mediaSession: MediaSession? = null

    val binding by viewBinding(FragmentVideoUnitBinding::bind)
    private val viewModel by viewModel<EncodedVideoUnitViewModel> {
        parametersOf(
            requireArguments().getString(ARG_COURSE_ID, ""),
            requireArguments().getString(ARG_BLOCK_ID, ""),
            requireArguments().getString(ARG_TITLE, ""),
        )
    }
    private val appReviewManager by inject<AppReviewManager> { parametersOf(requireActivity()) }
    private val pipViewModel by viewModel<PipViewModel>()
    private val pipReceiverManager by inject<PipBroadcastReceiverManager>()

    private var windowSize: WindowSize? = null

    private var lastPlayState: Boolean? = null

    private var isPipPlayerRegistered = false
    private var isEnteringPip = false

    private var originalCardMargins: Rect? = null
    private var originalCardCornerRadius: Float? = null
    private var originalResizeMode: Int? = null
    private var originalCardWidth: Int? = null
    private var originalCardHeight: Int? = null
    private var originalCardTopToTop: Int? = null
    private var originalCardTopToBottom: Int? = null
    private var originalCardBottomToBottom: Int? = null
    private var originalCardBottomToTop: Int? = null
    private var originalCardStartToStart: Int? = null
    private var originalCardEndToEnd: Int? = null


    private var lastVideoAspectRatio: Rational? = null
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
    @UnstableApi
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

        setupMediaSession()
        viewModel.exoPlayer?.let { initializePipSystem(it) }

        binding.connectionError.isVisible =
            !viewModel.hasInternetConnection && !viewModel.isDownloaded
        binding.pipBtn.setOnClickListener {
            enablePipMode()
        }

        binding.playerView.resizeMode =
            AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH

        viewModel.exoPlayer?.addListener(object : Player.Listener {

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    requireActivity().isInPictureInPictureMode
                ) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            if (!viewModel.exoPlayer!!.isPlaying) {
                                viewModel.exoPlayer?.play()
                            }
                        }
                    }
                }
            }

            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    lastVideoAspectRatio = Rational(videoSize.width, videoSize.height)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    requireActivity().isInPictureInPictureMode
                ) {
                    if (lastPlayState == null || lastPlayState != isPlaying) {
                        lastPlayState = isPlaying
                    }
                    updatePictureInPictureActions(isPlaying)
                }
            }


            override fun onPlayerError(error: PlaybackException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    requireActivity().isInPictureInPictureMode
                ) {
                    // Retry playback when internet is back
                    retryPlayback()
                }
            }


        })


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

        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        applyOrientationLayout(isLandscape)

        enableLongPressDoubleSpeed()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (isPipUiActive()) {
            updateUiForPipMode(true)
            return
        }
        val isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        view?.post { applyOrientationLayout(isLandscape) }
    }

    private fun applyOrientationLayout(isLandscape: Boolean) {
        val rootLayout = view?.findViewById<ConstraintLayout>(R.id.rootLayout) ?: return
        val constraintSet = ConstraintSet()
        constraintSet.clone(requireContext(), R.layout.fragment_video_unit)
        constraintSet.applyTo(rootLayout)

        val cardLayoutParams = binding.cardView.layoutParams as? ConstraintLayout.LayoutParams
        if (cardLayoutParams != null) {
            if (isLandscape) {
                // Keep intended split layout in landscape.
                cardLayoutParams.width = 0
                cardLayoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                cardLayoutParams.leftMargin = 0
                cardLayoutParams.topMargin = 0
                cardLayoutParams.rightMargin = 0
                cardLayoutParams.bottomMargin = 0
                cardLayoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                cardLayoutParams.topToBottom = ConstraintLayout.LayoutParams.UNSET
                cardLayoutParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                cardLayoutParams.bottomToTop = ConstraintLayout.LayoutParams.UNSET
                cardLayoutParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                cardLayoutParams.endToEnd = ConstraintLayout.LayoutParams.UNSET
                cardLayoutParams.matchConstraintDefaultWidth = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT_PERCENT
                cardLayoutParams.matchConstraintPercentWidth = 0.6f
            } else {
                // Hard reset to portrait full-width constrained behavior.
                cardLayoutParams.width = 0
                cardLayoutParams.height = 0
                cardLayoutParams.leftMargin = dpToPx(24)
                cardLayoutParams.topMargin = dpToPx(16)
                cardLayoutParams.rightMargin = dpToPx(24)
                cardLayoutParams.bottomMargin = 0
                cardLayoutParams.topToTop = ConstraintLayout.LayoutParams.UNSET
                cardLayoutParams.topToBottom = R.id.cv_video_title
                cardLayoutParams.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                cardLayoutParams.bottomToTop = ConstraintLayout.LayoutParams.UNSET
                cardLayoutParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                cardLayoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                cardLayoutParams.matchConstraintDefaultWidth = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT_SPREAD
                cardLayoutParams.matchConstraintPercentWidth = 1f
            }
            binding.cardView.layoutParams = cardLayoutParams
        }

        val subtitlesLayoutParams = binding.subtitles.layoutParams as? ConstraintLayout.LayoutParams
        if (subtitlesLayoutParams != null) {
            if (isLandscape) {
                subtitlesLayoutParams.width = 0
                subtitlesLayoutParams.height = 0
                subtitlesLayoutParams.leftMargin = dpToPx(20)
                subtitlesLayoutParams.topMargin = 0
                subtitlesLayoutParams.rightMargin = dpToPx(20)
                subtitlesLayoutParams.bottomMargin = 0
                subtitlesLayoutParams.startToStart = ConstraintLayout.LayoutParams.UNSET
                subtitlesLayoutParams.startToEnd = R.id.cardView
                subtitlesLayoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                subtitlesLayoutParams.topToTop = R.id.cardView
                subtitlesLayoutParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            } else {
                subtitlesLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                subtitlesLayoutParams.height = 0
                subtitlesLayoutParams.leftMargin = dpToPx(24)
                subtitlesLayoutParams.topMargin = dpToPx(28)
                subtitlesLayoutParams.rightMargin = dpToPx(24)
                subtitlesLayoutParams.bottomMargin = dpToPx(4)
                subtitlesLayoutParams.startToEnd = ConstraintLayout.LayoutParams.UNSET
                subtitlesLayoutParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                subtitlesLayoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                subtitlesLayoutParams.topToTop = ConstraintLayout.LayoutParams.UNSET
                subtitlesLayoutParams.topToBottom = R.id.cardView
                subtitlesLayoutParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            }
            binding.subtitles.layoutParams = subtitlesLayoutParams
        }

        if (isLandscape) {
            binding.cvVideoTitle?.visibility = View.GONE
            binding.pipBtn.visibility = View.GONE
        } else {
            binding.cvVideoTitle?.visibility = View.VISIBLE
            binding.pipBtn.visibility = View.VISIBLE
        }
        updatePipButtonState(isLandscape)
    }

    private fun isPipUiActive(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            (isEnteringPip || requireActivity().isInPictureInPictureMode)
    }

    private fun updatePipButtonState(isLandscape: Boolean) {
        if (isLandscape) {
            binding.pipBtn.visibility = View.GONE
            return
        }

        binding.pipBtn.visibility = if (isPipPermissionAllowed()) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }
    }

    private fun isPipPermissionAllowed(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val hostActivity = activity ?: return false
        if (!hostActivity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            return false
        }

        val appOps = hostActivity.getSystemService(AppOpsManager::class.java) ?: return true
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
            Process.myUid(),
            hostActivity.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED || mode == AppOpsManager.MODE_DEFAULT
    }

    private fun clearSavedCardState() {
        originalCardMargins = null
        originalCardCornerRadius = null
        originalResizeMode = null
        originalCardWidth = null
        originalCardHeight = null
        originalCardTopToTop = null
        originalCardTopToBottom = null
        originalCardBottomToBottom = null
        originalCardBottomToTop = null
        originalCardStartToStart = null
        originalCardEndToEnd = null
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics,
        ).toInt()
    }

    @UnstableApi
    private fun updatePlayerType(player: Player?) {
        with(binding.playerView) {
            this.player = null
            this.player = player
            this.setShowNextButton(false)
            this.setShowPreviousButton(false)
            this.controllerHideOnTouch = false
            this.setFullscreenButtonClickListener {
                if (viewModel.enterFullscreen()) {
                    VideoFullScreenFragment.newInstance()
                        .show(childFragmentManager, VideoFullScreenFragment.TAG)                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !requireActivity().isInPictureInPictureMode) {
            setContainerChromeVisible(true)
        }
        updatePipButtonState(
            isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE,
        )
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        super.onPause()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isEnteringPip) {
            viewModel.exoPlayer?.playWhenReady = true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!requireActivity().isInPictureInPictureMode) {
                requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        } else {
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    @UnstableApi
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        isEnteringPip = false
        updateUiForPipMode(isInPictureInPictureMode)
        if (!isInPictureInPictureMode) {
            view?.post {
                clearSavedCardState()
                val isLandscape =
                    resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                applyOrientationLayout(isLandscape)
            }
            setContainerChromeVisible(true)
            pipViewModel.exitPipMode()
        }
    }

    @UnstableApi
    override fun onDestroy() {
        setContainerChromeVisible(true)
        if (isAdded && !requireActivity().isChangingConfigurations) {
            viewModel.releasePlayers()
        }
        pipViewModel.unregisterPlayer()
        isPipPlayerRegistered = false
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()

    }

    @UnstableApi
    private fun showVideoControllerIndefinitely(show: Boolean) {
        if (show) {
            binding.playerView.controllerAutoShow = false
            binding.playerView.controllerShowTimeoutMs = 0
            binding.playerView.controllerHideOnTouch = true
        } else {
            binding.playerView.controllerAutoShow = true
            binding.playerView.controllerShowTimeoutMs = 1000
            binding.playerView.controllerHideOnTouch = false
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
        private const val ACTION_FORWARD = "pip_forward"
        private const val ACTION_REWIND = "pip_rewind"

        private const val ACTION_PLAY = "pip_play"
        private const val ACTION_PAUSE = "pip_pause"
        private const val REQUEST_PLAY = 101

        private const val REQUEST_PAUSE = 102


        private const val REQUEST_FORWARD = 103
        private const val REQUEST_REWIND = 104


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

    @UnstableApi
    private fun setupMediaSession() {
        mediaSession = MediaSession.Builder(requireContext(), viewModel.exoPlayer!!).build()

    }
    private fun retryPlayback() {
        val player = viewModel.exoPlayer ?: return

        player.apply {
            prepare()   // re-buffer stream
            playWhenReady = true
        }
    }

    private fun initializePipSystem(player: androidx.media3.exoplayer.ExoPlayer) {
        if (isPipPlayerRegistered) return
        pipViewModel.registerPlayer(
            controller = ExoPlayerController(player),
            playerType = PipPlayerType.EXOPLAYER,
        )
        isPipPlayerRegistered = true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildPipParams(isPlaying: Boolean): PictureInPictureParams {
        val aspectRatio = getPipAspectRatioForVideo()
        val sourceRectHint = getPipSourceRect(binding.playerView)
        return PictureInPictureParams.Builder().apply {
            setAspectRatio(aspectRatio)
            sourceRectHint?.let(::setSourceRectHint)
            setActions(buildPipActions(isPlaying))
        }.build()
    }

    @UnstableApi
    private fun enablePipMode() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (!isPipPermissionAllowed()) return

        viewModel.exoPlayer?.let { player ->
            if (!player.isPlaying) {
                if (player.playbackState == Player.STATE_IDLE) {
                    player.prepare()
                }
                player.playWhenReady = true
                player.play()
            }
            pipViewModel.updatePlaybackState(isPlaying = true, isEnded = false)
        }

        setContainerChromeVisible(false)
        pipViewModel.enterPipMode()
        updateUiForPipMode(true)
        binding.playerView.hideController()
        isEnteringPip = true

        // Wait one frame so Android captures the player-focused UI in PiP.
        binding.cardView.post {
            val params = buildPipParams(viewModel.exoPlayer?.isPlaying == true)
            requireActivity().setPictureInPictureParams(params)
            val entered = requireActivity().enterPictureInPictureMode(params)
            if (!entered) {
                isEnteringPip = false
                setContainerChromeVisible(true)
                updateUiForPipMode(false)
                pipViewModel.exitPipMode()
                return@post
            }

            // Some devices pause/idle right after entering PiP; force resume.
            viewModel.exoPlayer?.apply {
                if (!isPlaying) {
                    if (playbackState == Player.STATE_IDLE) prepare()
                    playWhenReady = true
                    play()
                }
            }
        }
    }

    @UnstableApi
    private fun updateUiForPipMode(isInPip: Boolean) {
        updatePlayerContainerForPipMode(isInPip)
        binding.cardView.isVisible = true
        binding.playerView.isVisible = true
        binding.cvVideoTitle?.isGone = isInPip
        binding.subtitles.isGone = isInPip
        binding.pipBtn.isGone = isInPip
        binding.connectionError.isVisible = !isInPip && !viewModel.hasInternetConnection && !viewModel.isDownloaded
        binding.doubleSpeedBadge.isVisible = false
        binding.playerView.useController = !isInPip
        binding.playerView.controllerAutoShow = !isInPip
        if (isInPip) {
            binding.playerView.hideController()
        } else {
            binding.playerView.showController()
        }
    }

    private fun getPipSourceRect(anchor: View): Rect? {
        val rect = Rect()
        return rect.takeIf { anchor.getGlobalVisibleRect(it) }
    }

    private fun getPipAspectRatioForVideo(): Rational {
        val ratio = lastVideoAspectRatio ?: Rational(16, 9)
        val numerator = ratio.numerator
        val denominator = ratio.denominator
        if (numerator <= 0 || denominator <= 0) return Rational(16, 9)

        // Keep PiP landscape-oriented to avoid tiny portrait-like PiP windows.
        val width = max(numerator, denominator)
        val height = min(numerator, denominator)
        return Rational(width, height)
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
        if (originalResizeMode == null) {
            originalResizeMode = binding.playerView.resizeMode
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
        binding.playerView.resizeMode = if (isInPip) {
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        } else {
            originalResizeMode ?: AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
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

    @RequiresApi(Build.VERSION_CODES.O)
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
        requireActivity().setPictureInPictureParams(buildPipParams(isPlaying))
    }

    private fun setContainerChromeVisible(isVisible: Boolean) {
        sharedViewModel.buttonVisibility.value = isVisible
    }
}
