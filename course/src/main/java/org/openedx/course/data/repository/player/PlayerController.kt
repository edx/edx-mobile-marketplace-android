package org.openedx.course.data.repository.player

import androidx.media3.common.Player

/**
 * Abstraction for player controls - allows unified PiP control
 * for both ExoPlayer and YouTube player types.
 *
 * All PiP actions route through this interface so both players
 * are controlled with identical code paths.
 */
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

/**
 * ExoPlayer implementation of [PlayerController].
 */
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

    override fun release() {
        // ExoPlayer lifecycle managed by EncodedVideoUnitViewModel, not here
    }
}

