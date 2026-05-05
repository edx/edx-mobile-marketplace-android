package org.openedx.course.presentation.unit.video

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.openedx.core.BaseViewModel
import org.openedx.course.data.repository.player.PlayerController
import org.openedx.course.domain.interactor.PipInteractor
import org.openedx.course.domain.interactor.model.PipPlayerState
import org.openedx.course.domain.interactor.model.PipPlayerType

class PipViewModel(
    private val pipInteractor: PipInteractor,
) : BaseViewModel() {

    /** Observable PiP state for fragments. */
    val pipState: StateFlow<PipPlayerState> = pipInteractor.pipState

    /** One-time events for UI updates. */
    private val _pipEvent = MutableSharedFlow<PipUiEvent>(extraBufferCapacity = 1)
    val pipEvent: SharedFlow<PipUiEvent> = _pipEvent.asSharedFlow()

    // --- Player Registration ---

    fun registerPlayer(controller: PlayerController, playerType: PipPlayerType) {
        pipInteractor.registerPlayer(controller, playerType)
    }

    fun unregisterPlayer() {
        pipInteractor.unregisterPlayer()
    }

    // --- State Updates ---

    fun updatePlaybackState(isPlaying: Boolean, isEnded: Boolean = false) {
        pipInteractor.updatePlaybackState(isPlaying, isEnded)
    }

    // --- PiP Actions ---

    fun handleAction(action: PißpAction) {
        pipInteractor.handleAction(action)
    }

    // --- Lifecycle ---

    override fun onCleared() {
        super.onCleared()
        pipInteractor.unregisterPlayer()
    }
}

/**
 * One-time UI events emitted by PipViewModel.
 */
sealed class PipUiEvent {
    data object PipModeEntered : PipUiEvent()
    data object PipModeExited : PipUiEvent()
}
