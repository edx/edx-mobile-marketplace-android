package org.openedx.course.data.repository.player

import androidx.media3.common.Player
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerTracker

interface PlayerController {
    fun play()
    fun pause()
    fun seekForward(millis: Long = 10_000)
    fun seekBackward(millis: Long = 10_000)
    fun restart()
    fun isPlaying(): Boolean
    fun isEnded(): Boolean
    fun currentPosition(): Long
    fun duration(): Long
    fun release()
}

class ExoPlayerController(
    private val player: Player
) : PlayerController {

    override fun play() {
        player.play()
    }

    override fun pause() {
        player.pause()
    }

    override fun seekForward(millis: Long) {
        player.seekTo(player.currentPosition + millis)
    }

    override fun seekBackward(millis: Long) {
        player.seekTo((player.currentPosition - millis).coerceAtLeast(0L))
    }

    override fun restart() {
        player.seekTo(0)
        player.play()
    }

    override fun isPlaying(): Boolean {
        return player.isPlaying
    }

    override fun isEnded(): Boolean {
        return player.playbackState == Player.STATE_ENDED
    }

    override fun currentPosition(): Long {
        return player.currentPosition
    }

    override fun duration(): Long {
        return player.duration
    }

    override fun release() {
        player.release()
    }
}

class YouTubePlayerController(
    private val player: YouTubePlayer,
    private val tracker: YouTubePlayerTracker,
) : PlayerController {

    override fun play() {
        player.play()
    }

    override fun pause() {
        player.pause()
    }

    override fun seekForward(millis: Long) {
        val nextSeconds = tracker.currentSecond + (millis / 1000f)
        player.seekTo(nextSeconds.coerceAtMost(tracker.videoDuration))
    }

    override fun seekBackward(millis: Long) {
        val prevSeconds = tracker.currentSecond - (millis / 1000f)
        player.seekTo(prevSeconds.coerceAtLeast(0f))
    }

    override fun restart() {
        player.seekTo(0f)
        player.play()
    }

    override fun isPlaying(): Boolean {
        return tracker.state == PlayerConstants.PlayerState.PLAYING
    }

    override fun isEnded(): Boolean {
        return tracker.state == PlayerConstants.PlayerState.ENDED
    }

    override fun currentPosition(): Long {
        return (tracker.currentSecond * 1000L).toLong().coerceAtLeast(0L)
    }

    override fun duration(): Long {
        return (tracker.videoDuration * 1000L).toLong().coerceAtLeast(0L)
    }

    override fun release() {
        // YouTubePlayer lifecycle is owned by YouTubePlayerView.
    }
}
