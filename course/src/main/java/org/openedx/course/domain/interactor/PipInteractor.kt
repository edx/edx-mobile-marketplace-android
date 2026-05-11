package org.openedx.course.domain.interactor

import kotlinx.coroutines.flow.StateFlow
import org.openedx.course.data.repository.PipPlayerRepository
import org.openedx.course.data.repository.player.PlayerController
import org.openedx.course.domain.interactor.model.PipAction
import org.openedx.course.domain.interactor.model.PipPlayerState
import org.openedx.course.domain.interactor.model.PipPlayerType

class PipInteractor(
    private val repository: PipPlayerRepository
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

    /** Handle a PiP remote action. */
    fun handleAction(action: PipAction) {
        when (action) {
            is PipAction.Play -> {
                repository.controller?.play()
                repository.updatePlaybackState(isPlaying = true, isEnded = false)
            }

            is PipAction.Pause -> {
                repository.controller?.pause()
                repository.updatePlaybackState(isPlaying = false)
            }

            is PipAction.SeekForward -> repository.controller?.seekForward()
            is PipAction.SeekBackward -> repository.controller?.seekBackward()
            is PipAction.Replay -> {
                repository.controller?.restart()
                repository.updatePlaybackState(isPlaying = true, isEnded = false)
            }
        }
        repository.syncFromController()
    }

    /** Check if a player is currently registered. */
    fun hasPlayer(): Boolean = repository.controller != null

    /** Get the current player controller (nullable). */
    fun getController(): PlayerController? = repository.controller

    fun exitPipMode() {
        repository.updatePipMode(false)
        repository.syncFromController()
    }

    fun enterPipMode() {
        repository.updatePipMode(true)
        repository.syncFromController()
    }
}
