package org.openedx.course.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.openedx.course.data.repository.player.PlayerController
import org.openedx.course.domain.model.PipPlayerState
import org.openedx.course.domain.model.PipPlayerType

/**
 * Repository for managing PiP player state.
 *
 * Single source of truth for:
 * - Which player is currently registered for PiP
 * - Current playback state (playing, paused, ended)
 * - PiP mode transitions
 *
 * Injected via Koin as a singleton (one per app).
 */
class PipPlayerRepository {

    private val _state = MutableStateFlow(PipPlayerState())
    val state: StateFlow<PipPlayerState> = _state.asStateFlow()

    private var _controller: PlayerController? = null
    val controller: PlayerController? get() = _controller

    /**
     * Register a player controller for PiP actions.
     * Called when a video fragment initializes its player.
     */
    fun registerPlayer(controller: PlayerController, playerType: PipPlayerType) {
        android.util.Log.d("PipRepo", "Registering player: $controller, type: $playerType")
        _controller = controller
        _state.value = _state.value.copy(
            playerType = playerType,
            isPlaying = controller.isPlaying(),
            isEnded = controller.isEnded(),
        )
    }

    /**
     * Unregister the current player controller.
     * Called when a video fragment is destroyed.
     */
    fun unregisterPlayer() {
        _controller = null
        _state.value = PipPlayerState()
    }

    /**
     * Update playback state from player listener callbacks.
     */
    fun updatePlaybackState(isPlaying: Boolean, isEnded: Boolean = false) {
        _state.value = _state.value.copy(
            isPlaying = isPlaying,
            isEnded = isEnded,
        )
    }

    /**
     * Mark that PiP mode has been entered.
     */
    fun enterPipMode() {
        _state.value = _state.value.copy(isPipMode = true)
    }

    /**
     * Mark that PiP mode has been exited.
     */
    fun exitPipMode() {
        _state.value = _state.value.copy(isPipMode = false)
    }

    // --- Player control delegation ---

    fun play() {
        _controller?.play()
    }

    fun pause() {
        _controller?.pause()
    }

    fun seekForward(millis: Long = 10_000) {
        _controller?.seekForward(millis)
    }

    fun seekBackward(millis: Long = 10_000) {
        _controller?.seekBackward(millis)
    }

    fun restart() {
        _controller?.restart()
    }
}
