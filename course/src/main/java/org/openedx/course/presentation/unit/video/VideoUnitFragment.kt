package org.openedx.course.presentation.unit.video

import android.app.AppOpsManager
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.os.bundleOf
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
import org.openedx.course.data.repository.PipBroadcastReceiverManager
import org.openedx.course.data.repository.player.ExoPlayerController
import org.openedx.course.databinding.FragmentVideoUnitBinding
import org.openedx.course.domain.model.PipPlayerType
import org.openedx.course.presentation.ui.VideoSubtitles
import org.openedx.course.presentation.ui.VideoTitle
import org.openedx.course.presentation.ui.enableLongPressDoubleSpeed
import org.openedx.course.presentation.videos.SharedViewModel

class VideoUnitFragment : Fragment(R.layout.fragment_video_unit) {
    private var pictureInPictureParamsBuilder: PictureInPictureParams.Builder? = null
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var mediaSession: MediaSession? = null

    // NEW: PiP ViewModel (Activity-scoped, shared across video fragments)
    private val pipViewModel: PipViewModel by viewModel(ownerProducer = { requireActivity() })

    // NEW: PiP BroadcastReceiver manager (injected via Koin)
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
        //init PictureInPictureParams, requires Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pictureInPictureParamsBuilder = PictureInPictureParams.Builder()
        }

    }

    @RequiresApi(Build.VERSION_CODES.S)
    @OptIn(UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe PiP state changes to refresh PiP remote actions (play/pause/replay)
        pipViewModel.pipState
            .onEach {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    requireActivity().isInPictureInPictureMode
                ) {
                    updatePipActions()
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        binding.pipBtn?.isVisible =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    requireContext().isPipPermissionGranted()

        updateLayoutForOrientation()
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

        binding.connectionError.isVisible =
            !viewModel.hasInternetConnection && !viewModel.isDownloaded
        binding.pipBtn?.setOnClickListener {
            enablePipMode()

        }

        binding.playerView.resizeMode =
            AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH

        // Register ExoPlayer with PiP system
        viewModel.exoPlayer?.let { player ->
            val controller = ExoPlayerController(player)
            pipViewModel.registerPlayer(controller, PipPlayerType.EXOPLAYER)
        }

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
                    pictureInPictureParamsBuilder?.setAspectRatio(aspect)

                    // If you are already in PiP, push the updated params
                    if (requireActivity().isInPictureInPictureMode) {
                        requireActivity().setPictureInPictureParams(
                            pictureInPictureParamsBuilder!!.build()
                        )

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

        enableLongPressDoubleSpeed()
    }

    @OptIn(UnstableApi::class)
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
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding.pipBtn?.isVisible =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    requireContext().isPipPermissionGranted()
    }

    override fun onPause() {
        super.onPause()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!requireActivity().isInPictureInPictureMode) {
                requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        } else {
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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

    @RequiresApi(Build.VERSION_CODES.S)
    @OptIn(UnstableApi::class)
    private fun enablePipMode() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (!requireContext().isPipPermissionGranted()) {
            showPipDisabledMessage()
            return
        }

        binding.subtitles.isVisible = false
        binding.cvVideoTitle?.isVisible = false
        binding.pipBtn?.isVisible = false
        sharedViewModel.buttonVisibility.value = false
        binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        binding.playerView.useController = false

        val cs = ConstraintSet()
        cs.clone(constraintContainer)
        // Clear any existing ratio on the card
        cs.setDimensionRatio(binding.cardView.id, null) // If your ConstraintSet version doesn’t accept null, set "0:0"
        // Make the card follow content
        cs.constrainWidth(binding.cardView.id, ConstraintSet.MATCH_CONSTRAINT)
        cs.constrainHeight(binding.cardView.id, ConstraintSet.WRAP_CONTENT)
        cs.applyTo(constraintContainer)

        resetConstraintsForPip()

        // Prefer the actual video aspect if known
        lastVideoAspectRatio?.let { pictureInPictureParamsBuilder?.setAspectRatio(it) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pictureInPictureParamsBuilder?.setSeamlessResizeEnabled(true)
        }

        updatePipActions()
        pictureInPictureParamsBuilder?.build()?.let {
            requireActivity().enterPictureInPictureMode(it)
        }

    }



    override fun onStop() {
        super.onStop()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !requireActivity().isInPictureInPictureMode
        ) {
            pipReceiverManager.unregister()
        }

        // Do NOT stop when in PiP
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            requireActivity().isInPictureInPictureMode
        ) {
            return
        }

        // Only stop if truly backgrounded without PiP
        binding.playerView.player?.let { player ->
            if (player.isPlaying) player.pause()
        }
    }

    override fun onStart() {
        super.onStart()
        pipReceiverManager.register()
    }


    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(UnstableApi::class)
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        if (isInPictureInPictureMode) {
            pipViewModel.enterPipMode()
            binding.subtitles.isVisible = false
            binding.pipBtn?.isVisible = false
            binding.playerView.useController = false
            sharedViewModel.buttonVisibility.value = false
            binding.cvVideoTitle?.visibility = View.GONE
            // Clear all margins for PiP
            clearAllMarginsAndConstraints()

            binding.cardView.radius = 0f
            updatePipActions()

            (binding.playerView.layoutParams as FrameLayout.LayoutParams).apply {
                width = FrameLayout.LayoutParams.MATCH_PARENT
                height = FrameLayout.LayoutParams.WRAP_CONTENT
            }

            binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT

            resetConstraintsForPip()

            lastVideoAspectRatio?.let { ar ->
                pictureInPictureParamsBuilder?.setAspectRatio(ar)
                requireActivity().setPictureInPictureParams(
                    pictureInPictureParamsBuilder!!.build()
                )
            }

        } else {
            pipViewModel.exitPipMode()
            restoreNormalUI()
        }
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


    @OptIn(UnstableApi::class)
    private fun restoreNormalUI() {
        binding.subtitles.isVisible = true
        binding.pipBtn?.isVisible = true
        binding.playerView.useController = true
        binding.cvVideoTitle?.visibility = View.VISIBLE
        sharedViewModel.buttonVisibility.value = true

        binding.playerView.resizeMode =
            AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH

        binding.cardView.radius =
            resources.getDimension(R.dimen.subtitle_margin_top)

        clearAllMarginsAndConstraints()

        binding.rootLayout?.post {
            updateLayoutForOrientation()
        }
    }
    private fun clearAllMarginsAndConstraints() {
        // Clear layout params margins
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
        subtitleParams.bottomMargin = 0
        binding.subtitles.layoutParams = subtitleParams

        // Force layout update
        binding.cardView.requestLayout()
        binding.subtitles.requestLayout()
        binding.rootLayout?.requestLayout()
    }

    private fun updateLayoutForOrientation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            requireActivity().isInPictureInPictureMode
        ) {
            return
        }

        // Clear everything first
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

        // Completely reset constraints
        constraintSet.clear(binding.cardView.id)
        constraintSet.clear(binding.subtitles.id)

        if (isLandscape) {
            // VIDEO LEFT - NO TOP MARGIN IN LANDSCAPE
            constraintSet.connect(
                binding.cardView.id,
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START,
                playerMarginH  // Only horizontal margin
            )
            constraintSet.connect(
                binding.cardView.id,
                ConstraintSet.TOP,
                ConstraintSet.PARENT_ID,
                ConstraintSet.TOP,
                0  // NO TOP MARGIN
            )
            constraintSet.connect(
                binding.cardView.id,
                ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID,
                ConstraintSet.BOTTOM,
                0
            )

            constraintSet.constrainWidth(binding.cardView.id, 0)
            constraintSet.constrainPercentWidth(binding.cardView.id, 0.65f)
            constraintSet.constrainHeight(binding.cardView.id, playerHeight)
            constraintSet.setDimensionRatio(binding.cardView.id, "20:9")

            // SUBTITLES RIGHT
            constraintSet.connect(
                binding.subtitles.id,
                ConstraintSet.START,
                binding.cardView.id,
                ConstraintSet.END,
                0
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
                subtitleMarginH
            )

            constraintSet.constrainWidth(binding.subtitles.id, 0)
            constraintSet.constrainPercentWidth(binding.subtitles.id, 0.35f)

            constraintSet.setVisibility(binding.cvVideoTitle!!.id, ConstraintSet.GONE)
            binding.pipBtn?.visibility = View.GONE

        } else {
            // PORTRAIT
            binding.cvVideoTitle?.visibility = View.VISIBLE
            val playerMarginTop = resources.getDimensionPixelSize(R.dimen.portrait_video_margin_top)

            // VIDEO CARD
            constraintSet.connect(
                binding.cardView.id,
                ConstraintSet.TOP,
                ConstraintSet.PARENT_ID,
                ConstraintSet.TOP,
                playerMarginTop
            )
            constraintSet.connect(
                binding.cardView.id,
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START,
                playerMarginH
            )
            constraintSet.connect(
                binding.cardView.id,
                ConstraintSet.END,
                ConstraintSet.PARENT_ID,
                ConstraintSet.END,
                playerMarginH
            )

            constraintSet.constrainHeight(binding.cardView.id, playerHeight)
            constraintSet.constrainWidth(binding.cardView.id, 0)
            constraintSet.setDimensionRatio(binding.cardView.id, "16:9")

            // SUBTITLES
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

            constraintSet.setVisibility(binding.cvVideoTitle!!.id, ConstraintSet.VISIBLE)
            binding.pipBtn?.visibility = View.VISIBLE
        }

        constraintSet.applyTo(constraintContainer)

        binding.rootLayout?.post {
            binding.rootLayout?.requestLayout()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        binding.rootLayout?.postDelayed({
            updateLayoutForOrientation()
        }, 100)

    }


    @OptIn(UnstableApi::class)
    private fun rebindPlayerView() {
        val player = binding.playerView.player ?: return

        // 1. Detach player
        binding.playerView.player = null

        binding.playerView.post {
            binding.playerView.apply {
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                requestLayout()
            }

            // 3. Re-attach player
            binding.playerView.player = player
            val params = binding.cardView.layoutParams as ConstraintLayout.LayoutParams
            params.marginStart = 68
            params.marginEnd = 68
            params.topMargin = 16
            binding.cardView.layoutParams = params
            binding.cardView.radius =40f
        }


    }


    @OptIn(UnstableApi::class)
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

        val playIntent = PendingIntent.getBroadcast(
            requireContext(), PipBroadcastReceiverManager.REQUEST_PLAY,
            android.content.Intent(PipBroadcastReceiverManager.ACTION_PLAY),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val pauseIntent = PendingIntent.getBroadcast(
            requireContext(), PipBroadcastReceiverManager.REQUEST_PAUSE,
            android.content.Intent(PipBroadcastReceiverManager.ACTION_PAUSE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val forwardIntent = PendingIntent.getBroadcast(
            requireContext(), PipBroadcastReceiverManager.REQUEST_FORWARD,
            android.content.Intent(PipBroadcastReceiverManager.ACTION_FORWARD),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val rewindIntent = PendingIntent.getBroadcast(
            requireContext(), PipBroadcastReceiverManager.REQUEST_REWIND,
            android.content.Intent(PipBroadcastReceiverManager.ACTION_REWIND),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val actions = listOf(
            RemoteAction(
                Icon.createWithResource(requireContext(), R.drawable.ic_rewind),
                "Rewind", "Rewind 10s", rewindIntent
            ),
            if (player.playWhenReady && player.playbackState == Player.STATE_READY) {
                RemoteAction(
                    Icon.createWithResource(requireContext(), R.drawable.ic_pause),
                    "Pause", "Pause Video", pauseIntent
                )
            } else {
                RemoteAction(
                    Icon.createWithResource(requireContext(), R.drawable.ic_play),
                    "Play", "Play Video", playIntent
                )
            },
            RemoteAction(
                Icon.createWithResource(requireContext(), R.drawable.ic_forward),
                "Forward", "Forward 10s", forwardIntent
            )
        )

        pictureInPictureParamsBuilder?.setActions(actions)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            requireActivity().isInPictureInPictureMode
        ) {
            requireActivity().setPictureInPictureParams(
                pictureInPictureParamsBuilder!!.build()
            )
        }
    }


    private fun resetConstraintsForPip() {
        val set = ConstraintSet()
        set.clone(constraintContainer)

        // Completely clear constraints on cardView
        set.clear(binding.cardView.id)
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

        // Force MATCH_CONSTRAINT for PiP
        set.constrainWidth(binding.cardView.id, ConstraintSet.MATCH_CONSTRAINT)
        set.constrainHeight(binding.cardView.id, ConstraintSet.MATCH_CONSTRAINT)

        // Remove dimension ratio used in landscape mode
        set.setDimensionRatio(binding.cardView.id, null)

        set.applyTo(constraintContainer)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun clearPipActions() {
        pictureInPictureParamsBuilder?.setActions(emptyList())
        requireActivity().setPictureInPictureParams(
            pictureInPictureParamsBuilder!!.build()
        )
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


}