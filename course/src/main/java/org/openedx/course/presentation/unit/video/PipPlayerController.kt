package org.openedx.course.presentation.unit.video

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer

object PipPlayerController {
    var player: YouTubePlayer? = null

    var isPlaying: Boolean = false
    var isEnded: Boolean = false

    fun play() {
        if (isEnded) {
            restart()
        } else {
            player?.play()
        }
        isPlaying = true
        isEnded = false
    }

    fun pause() {
        player?.pause()
        isPlaying = false
    }

    fun restart() {
        player?.seekTo(0f)
        player?.play()
        isPlaying = true
        isEnded = false
    }
}