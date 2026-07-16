package org.openedx.course.data.repository.player

import androidx.media3.common.Player

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
}

class ExoPlayerController(
    private val player: Player
) : PlayerController {

    override fun play() {
        if (player.playbackState == Player.STATE_ENDED) {
            restart()
        } else {
            player.play()
        }
    }

    override fun pause() {
        player.pause()
    }

    override fun seekForward(millis: Long) {
        val position = (player.currentPosition + millis).coerceAtMost(player.duration)
        player.seekTo(position)
    }

    override fun seekBackward(millis: Long) {
        val position = (player.currentPosition - millis).coerceAtLeast(0)
        player.seekTo(position)
    }

    override fun restart() {
        player.seekTo(0)
        player.play()
    }

    override fun isPlaying(): Boolean {
        return player.playWhenReady && player.playbackState == Player.STATE_READY
    }

    override fun isEnded(): Boolean {
        return player.playbackState == Player.STATE_ENDED
    }

    override fun currentPosition(): Long = player.currentPosition

    override fun duration(): Long = player.duration
}

class YouTubePlayerController(
    private val player: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
) : PlayerController {

    private var _isPlaying: Boolean = false
    private var _isEnded: Boolean = false
    private var _currentSeconds: Float = 0f
    private var _duration: Float = 0f

    fun updateState(isPlaying: Boolean, isEnded: Boolean) {
        _isPlaying = isPlaying
        _isEnded = isEnded
    }

    fun updateTime(currentSeconds: Float, duration: Float) {
        _currentSeconds = currentSeconds
        _duration = duration
    }

    override fun play() {
        if (_isEnded) {
            restart()
        } else {
            player.play()
        }
        _isPlaying = true
        _isEnded = false
    }

    override fun pause() {
        player.pause()
        _isPlaying = false
    }

    override fun seekForward(millis: Long) {
        val newPos = (_currentSeconds + millis / 1000f).coerceAtMost(_duration)
        player.seekTo(newPos)
    }

    override fun seekBackward(millis: Long) {
        val newPos = (_currentSeconds - millis / 1000f).coerceAtLeast(0f)
        player.seekTo(newPos)
    }

    override fun restart() {
        player.seekTo(0f)
        player.play()
        _isPlaying = true
        _isEnded = false
    }

    override fun isPlaying(): Boolean = _isPlaying

    override fun isEnded(): Boolean = _isEnded

    override fun currentPosition(): Long = (_currentSeconds * 1000).toLong()

    override fun duration(): Long = (_duration * 1000).toLong()
}