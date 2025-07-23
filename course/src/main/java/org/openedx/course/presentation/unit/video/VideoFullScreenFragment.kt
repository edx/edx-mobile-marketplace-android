package org.openedx.course.presentation.unit.video

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import org.openedx.core.presentation.dialog.appreview.AppReviewManager
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.course.R
import org.openedx.course.presentation.ui.enableLongPressDoubleSpeed
import org.openedx.core.R as CoreR

class VideoFullScreenFragment : DialogFragment() {

    private val viewModel: EncodedVideoUnitViewModel by viewModels({ requireParentFragment() })
    private val appReviewManager by inject<AppReviewManager> { parametersOf(requireActivity()) }
    private val exoPlayerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            if (playbackState == Player.STATE_ENDED) {
                if (!appReviewManager.isDialogShowed) {
                    appReviewManager.tryToOpenRateDialog()
                }
                viewModel.markBlockCompleted(viewModel.blockId)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                PlayerComposeView()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, CoreR.style.Theme_OpenEdX_Dialog_FullScreen)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            statusBarColor = Color.BLACK
            navigationBarColor = Color.BLACK
            WindowCompat.getInsetsController(this, this.decorView).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
            setBackgroundDrawable(Color.BLACK.toDrawable())
            attributes = attributes.apply { dimAmount = 0f }
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    @Composable
    private fun PlayerComposeView() {
        val currentView = LocalView.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val scope = rememberCoroutineScope()
        var showDoubleSpeedBadge by remember { mutableStateOf(false) }

        DisposableEffect(Unit) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.enterFullscreen()
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                currentView.keepScreenOn = false
                viewModel.leaveFullscreen()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
                factory = { context ->
                    val playerView = PlayerView(context).apply {
                        player = viewModel.exoPlayer
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                        setShowSubtitleButton(true)
                        setFullscreenButtonClickListener {
                            dismiss()
                        }
                    }

                    playerView.enableLongPressDoubleSpeed(
                        player = viewModel.exoPlayer!!,
                        scope = scope,
                        onBadgeVisibilityChange = { showDoubleSpeedBadge = it },
                    )

                    playerView
                },
            )

            if (showDoubleSpeedBadge) {
                Image(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 6.dp),
                    painter = painterResource(R.drawable.ic_course_double_speed_badge),
                    contentDescription = stringResource(R.string.course_accessibility_double_playback_speed),
                )
            }
        }
    }

    override fun onPause() {
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        viewModel.exoPlayer?.removeListener(exoPlayerListener)
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        viewModel.exoPlayer?.addListener(exoPlayerListener)
    }

    companion object {
        const val TAG = "VideoFullScreenFragment"
        fun newInstance() = VideoFullScreenFragment()
    }
}
