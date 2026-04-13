package org.openedx.course.presentation.unit.video

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import org.openedx.course.R

class FullscreenYoutubeFragment : DialogFragment() {

    private var youTubePlayer: YouTubePlayer? = null
    private var startTime = 0f
    private lateinit var videoId: String

    private var isPlayerReady = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)


        arguments?.let {
            videoId = it.getString(ARG_VIDEO_ID) ?: ""
            startTime = it.getFloat(ARG_START_TIME, 0f)
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_fullscreen_youtube, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val playerView = view.findViewById<YouTubePlayerView>(R.id.youtubePlayerView)
        val closeBtn = view.findViewById<View>(R.id.closeBtn)

        lifecycle.addObserver(playerView)

        playerView.initialize(object : AbstractYouTubePlayerListener() {

            override fun onReady(player: YouTubePlayer) {
                youTubePlayer = player

                isPlayerReady = true
                //Start exactly from current position
                player.loadVideo(videoId, startTime)
            }

            override fun onCurrentSecond(
                youTubePlayer: YouTubePlayer,
                second: Float
            ) {
                startTime = second
            }
        })



        closeBtn.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                "FULLSCREEN_EXIT",
                bundleOf("time" to startTime)
            )
            dismiss()
        }
    }

    companion object {
        const val ARG_VIDEO_ID = "arg_video_id"
        const val ARG_START_TIME = "arg_start_time"

        fun newInstance(videoId: String, startTime: Float): FullscreenYoutubeFragment {
            return FullscreenYoutubeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_VIDEO_ID, videoId)
                    putFloat(ARG_START_TIME, startTime)
                }
            }
        }

    }
}