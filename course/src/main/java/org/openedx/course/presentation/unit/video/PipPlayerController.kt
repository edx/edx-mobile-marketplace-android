package org.openedx.course.presentation.unit.video

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer

object  PipPlayerController {

    var player: YouTubePlayer? = null

    var isPlaying = false

    fun play() {
        player?.play()
        isPlaying = true
    }

    fun pause() {
        player?.pause()
        isPlaying = false
    }
}