package org.openedx.course.domain.interactor

import kotlinx.coroutines.flow.StateFlow
import org.openedx.course.data.repository.PIPPlayerRepository
import org.openedx.course.data.repository.player.PlayerController
import org.openedx.course.domain.model.PipAction
import org.openedx.course.domain.model.PipPlayerState
import org.openedx.course.domain.model.PipPlayerType

/**
 * Business logic orchestrator for PiP feature.
 *
 * Pure Kotlin (no Android dependencies). Routes actions to the repository
 * and exposes state as a Flow for the ViewModel to observe.
 *
 * Injected via Koin as a factory (new instance per use).
 */
class PipInteractor(
    private val repository: PIPPlayerRepository
) {
    /** Observable PiP state. */
    val pipState: StateFlow<PipPlayerState> = repository.state

    /** Register a player controller for PiP actions. */
    fun registerPlayer(controller: PlayerController, playerType: PipPlayerType) {
        repository.registerPlayer(controller, playerType)
    }

    /** Unregister the current player. */
    fun unregisterPlayer() {
        repository.unregisterPlayer()
    }

    /** Update playback state from player listener callbacks. */
    fun updatePlaybackState(isPlaying: Boolean, isEnded: Boolean = false) {
        repository.updatePlaybackState(isPlaying, isEnded)
    }

    /** Mark PiP mode entered. */
    fun enterPipMode() {
        repository.enterPipMode()
    }

    /** Mark PiP mode exited. */
    fun exitPipMode() {
        repository.exitPipMode()
    }

    /** Handle a PiP remote action. */
    fun handleAction(action: PipAction) {
        when (action) {
            is PipAction.Play -> repository.play()
            is PipAction.Pause -> repository.pause()
            is PipAction.SeekForward -> repository.seekForward()
            is PipAction.SeekBackward -> repository.seekBackward()
            is PipAction.Replay -> repository.restart()
        }
    }

    /** Check if a player is currently registered. */
    fun hasPlayer(): Boolean = repository.controller != null

    /** Get the current player controller (nullable). */
    fun getController(): PlayerController? = repository.controller
}