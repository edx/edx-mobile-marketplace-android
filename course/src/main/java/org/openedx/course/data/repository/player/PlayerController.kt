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