package org.openedx.course.presentation.unit.video

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.viewinterop.AndroidView
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
            setBackgroundDrawable(ColorDrawable(Color.BLACK))
            attributes = attributes.apply { dimAmount = 0f }
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    @Composable
    private fun PlayerComposeView() {
        val currentView = LocalView.current
        val lifecycleOwner = LocalLifecycleOwner.current

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
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            factory = {
                currentView.keepScreenOn = true
                PlayerView(it).apply {
                    player = viewModel.exoPlayer
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    setShowSubtitleButton(true)
                    setFullscreenButtonClickListener { _ ->
                        dismiss()
                    }
                }
            },
        )
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
