package org.openedx.course.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.openedx.course.data.repository.player.PlayerController
import org.openedx.course.domain.interactor.model.PipPlayerState
import org.openedx.course.domain.interactor.model.PipPlayerType

class PipPlayerRepository {

    private val _state = MutableStateFlow(PipPlayerState())
    val state: StateFlow<PipPlayerState> = _state.asStateFlow()

    private var _controller: PlayerController? = null
    val controller: PlayerController? get() = _controller

    fun registerPlayer(controller: PlayerController, playerType: PipPlayerType) {
        _controller = controller
        _state.value = _state.value.copy(
            playerType = playerType,
            isPlaying = controller.isPlaying(),
            isEnded = controller.isEnded(),
            position = controller.currentPosition(),
            duration = controller.duration(),
        )
    }

    fun unregisterPlayer() {
        _controller = null
        _state.value = PipPlayerState()
    }

    fun updatePlaybackState(isPlaying: Boolean, isEnded: Boolean = false) {
        _state.value = _state.value.copy(
            isPlaying = isPlaying,
            isEnded = isEnded,
        )
    }

    fun updateProgress(position: Long, duration: Long) {
        _state.value = _state.value.copy(
            position = position.coerceAtLeast(0L),
            duration = duration.coerceAtLeast(0L),
        )
    }

    fun updatePipMode(isPipMode: Boolean) {
        _state.value = _state.value.copy(
            isPipMode = isPipMode,
        )
    }

    fun syncFromController() {
        val currentController = _controller ?: return
        _state.value = _state.value.copy(
            isPlaying = currentController.isPlaying(),
            isEnded = currentController.isEnded(),
            position = currentController.currentPosition(),
            duration = currentController.duration(),
        )
    }
}