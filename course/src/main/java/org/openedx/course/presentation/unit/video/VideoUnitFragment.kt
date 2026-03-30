package org.openedx.course.presentation.unit.video

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.ui.AspectRatioFrameLayout
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

    private var windowSize: WindowSize? = null

    private var lastPlayState: Boolean? = null

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
            viewModel.isDownloaded = getBoolean(ARG_DOWNLOADED)
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

        viewModel.exoPlayer?.addListener(object : Player.Listener {

            override fun onPlaybackStateChanged(playbackState: Int) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    requireActivity().isInPictureInPictureMode
                ) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            updatePipActions()
                        }
                    }
                    if (playbackState == Player.STATE_ENDED) {
                        if (requireActivity().isInPictureInPictureMode) {
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    requireActivity().isInPictureInPictureMode
                ) {
                    if (lastPlayState == null || lastPlayState != isPlaying) {
                        lastPlayState = isPlaying
                        updatePipActions()
                    }
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
                        .show(childFragmentManager,      VideoFullScreenFragment.TAG)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        super.onPause()
    }

    @UnstableApi
    override fun onDestroy() {
        if (!requireActivity().isChangingConfigurations) {
            viewModel.releasePlayers()
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

    @RequiresApi(Build.VERSION_CODES.S)
    @OptIn(UnstableApi::class)
    private fun enablePipMode() {
        binding.subtitles.isVisible = false
        binding.cvVideoTitle?.isVisible = false
        binding.pipBtn?.isVisible = false
        sharedViewModel.buttonVisibility.value = false
        binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        binding.playerView.useController = false

        val cs = ConstraintSet()
        cs.clone(binding.rootLayout as ConstraintLayout)
        // Clear any existing ratio on the card
        cs.setDimensionRatio(binding.cardView.id, null) // If your ConstraintSet version doesn’t accept null, set "0:0"
        // Make the card follow content
        cs.constrainWidth(binding.cardView.id, ConstraintSet.MATCH_CONSTRAINT)
        cs.constrainHeight(binding.cardView.id, ConstraintSet.WRAP_CONTENT)
        cs.applyTo(binding.rootLayout as ConstraintLayout)


        resetConstraintsForPip()

        // Prefer the actual video aspect if known
        lastVideoAspectRatio?.let { pictureInPictureParamsBuilder?.setAspectRatio(it) }

        // Android 12+ for smoother resize
        pictureInPictureParamsBuilder?.setSeamlessResizeEnabled(true)

        updatePipActions()
        pictureInPictureParamsBuilder?.build()?.let {
            requireActivity().enterPictureInPictureMode(it)
        }

    }



    override fun onStop() {
        super.onStop()

        requireContext().unregisterReceiver(pipActionReceiver)

        // Do NOT stop when in PiP
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            requireActivity().isInPictureInPictureMode
        ) {
            return
        }

        // Only stop if truly backgrounded without PiP
        binding.playerView.player?.let { player ->
            if (player.isPlaying) player.pause() // Prefer pause to stop
        }

    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(requireContext(), pipActionReceiver, IntentFilter().apply {
            addAction(ACTION_PLAY)
            addAction(ACTION_PAUSE)
            addAction(ACTION_FORWARD)
            addAction(ACTION_REWIND)
        }, ContextCompat.RECEIVER_EXPORTED)



    }


    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(UnstableApi::class)
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        if (isInPictureInPictureMode) {
            binding.subtitles.isVisible = false
            binding.pipBtn?.isVisible = false
            binding.playerView.useController = false
            sharedViewModel.buttonVisibility.value = false
            binding.cvVideoTitle!!.visibility = View.GONE
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
            binding.subtitles.visibility = View.VISIBLE
            binding.pipBtn?.visibility = View.VISIBLE
            binding.playerView.useController = true
            sharedViewModel.buttonVisibility.value = true
            binding.cvVideoTitle!!.visibility = View.GONE

            // Clear everything and reset
            clearAllMarginsAndConstraints()

            binding.cardView.radius = resources.getDimension(R.dimen.subtitle_margin_top)

            (binding.playerView.layoutParams as FrameLayout.LayoutParams).apply {
                width = FrameLayout.LayoutParams.MATCH_PARENT
                height = FrameLayout.LayoutParams.MATCH_PARENT
            }

            binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH

            binding.rootLayout?.post {
                updateLayoutForOrientation()
            }
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

        val constraintLayout = binding.rootLayout
        val constraintSet = androidx.constraintlayout.widget.ConstraintSet()
        constraintSet.clone(constraintLayout as ConstraintLayout)

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
            val playerMarginTop = resources.getDimensionPixelSize(R.dimen.potrait_video_margin_top)

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

        constraintSet.applyTo(constraintLayout)

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
        //if (player.playbackState != Player.STATE_READY) return
        if (player.playbackState == Player.STATE_ENDED) return

        val playIntent = PendingIntent.getBroadcast(
            requireContext(), REQUEST_PLAY,
            Intent(ACTION_PLAY),
            PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = PendingIntent.getBroadcast(
            requireContext(), REQUEST_PAUSE,
            Intent(ACTION_PAUSE),
            PendingIntent.FLAG_IMMUTABLE
        )

        val forwardIntent = PendingIntent.getBroadcast(
            requireContext(), REQUEST_FORWARD,
            Intent(ACTION_FORWARD),
            PendingIntent.FLAG_IMMUTABLE
        )

        val rewindIntent = PendingIntent.getBroadcast(
            requireContext(), REQUEST_REWIND,
            Intent(ACTION_REWIND),
            PendingIntent.FLAG_IMMUTABLE
        )

        val actions = listOf(
            RemoteAction(
                Icon.createWithResource(requireContext(), R.drawable.ic_rewind),
                "Rewind",
                "Rewind 10s",
                rewindIntent
            ),
            if (viewModel.exoPlayer?.isPlaying == true) {
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
                "Forward",
                "Forward 10s",
                forwardIntent
            )
        )

        pictureInPictureParamsBuilder?.setActions(actions)
        requireActivity().setPictureInPictureParams(pictureInPictureParamsBuilder!!.build())

    }

    private val pipActionReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {

                ACTION_PLAY -> viewModel.exoPlayer?.apply {
                    // ✅ Only restart if video ended
                    if (playbackState == Player.STATE_ENDED) {
                        seekTo(0)
                    }
                    play()
                }

                ACTION_PAUSE -> viewModel.exoPlayer?.pause()

                ACTION_FORWARD -> seekBy(10000)

                ACTION_REWIND -> seekBy(-10000)
            }

            // ✅ Update PiP controls to reflect play/pause state
            updatePipActions()
        }
    }


    private fun resetConstraintsForPip() {
        val set = ConstraintSet()
        set.clone(binding.rootLayout as ConstraintLayout)

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

        set.applyTo(binding.rootLayout as ConstraintLayout)
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
        val replayIntent = PendingIntent.getBroadcast(
            requireContext(),
            105,
            Intent(ACTION_PLAY), // reuse play
            PendingIntent.FLAG_IMMUTABLE
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

}







