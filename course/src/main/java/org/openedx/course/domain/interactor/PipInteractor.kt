package org.openedx.course.domain.interactor

import kotlinx.coroutines.flow.StateFlow
import org.openedx.course.data.repository.PipPlayerRepository
import org.openedx.course.data.repository.player.PlayerController
import org.openedx.course.domain.model.PipAction
import org.openedx.course.domain.model.PipPlayerState
import org.openedx.course.domain.model.PipPlayerType


class PipInteractor(
    private val repository: PipPlayerRepository
) {
    val pipState: StateFlow<PipPlayerState> = repository.state

    fun registerPlayer(controller: PlayerController, playerType: PipPlayerType) {
        repository.registerPlayer(controller, playerType)
    }

    fun unregisterPlayer() {
        repository.unregisterPlayer()
    }

    fun updatePlaybackState(isPlaying: Boolean, isEnded: Boolean = false) {
        repository.updatePlaybackState(isPlaying, isEnded)
    }

    fun enterPipMode() {
        repository.enterPipMode()
    }

    fun exitPipMode() {
        repository.exitPipMode()
    }

    fun handleAction(action: PipAction) {
        when (action) {
            is PipAction.Play -> repository.play()
            is PipAction.Pause -> repository.pause()
            is PipAction.SeekForward -> repository.seekForward()
            is PipAction.SeekBackward -> repository.seekBackward()
            is PipAction.Replay -> repository.restart()
        }
    }

    fun hasPlayer(): Boolean = repository.controller != null

}