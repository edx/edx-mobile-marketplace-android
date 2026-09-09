package org.openedx.course.presentation.unit.video

import android.app.AppOpsManager
import android.app.RemoteAction
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.course.data.repository.player.PlayerController
import org.openedx.course.domain.interactor.PipInteractor
import org.openedx.course.domain.model.PipAction
import org.openedx.course.domain.model.PipPlayerState
import org.openedx.course.domain.model.PipPlayerType


class PipViewModel(
    private val pipInteractor: PipInteractor,
) : BaseViewModel() {

    /** Observable PiP state for fragments. */
    val pipState: StateFlow<PipPlayerState> = pipInteractor.pipState

    /** One-time events for UI updates. */
    private val _pipEvent = MutableSharedFlow<PipUiEvent>(extraBufferCapacity = 1)
    val pipEvent: SharedFlow<PipUiEvent> = _pipEvent.asSharedFlow()
    private val _buttonVisibility = MutableLiveData<Boolean>(true)
    val buttonVisibility: LiveData<Boolean> = _buttonVisibility

    private val _pipActions = MutableLiveData<List<RemoteAction>>()
    val pipActions: LiveData<List<RemoteAction>> = _pipActions

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadPipActions(context: Context, isPlaying: Boolean) {
        viewModelScope.launch {
            val actions = pipInteractor.buildPipActions(context, isPlaying)
            _pipActions.value = actions
        }
    }

    internal fun updateButtonVisibility(visible: Boolean) {
        _buttonVisibility.value = visible
    }

    fun registerPlayer(controller: PlayerController, playerType: PipPlayerType) {
        viewModelScope.launch {
            pipInteractor.registerPlayer(controller, playerType)
        }
    }

    fun unregisterPlayer() {
        viewModelScope.launch {
            pipInteractor.unregisterPlayer()
        }
    }

    fun updatePlaybackState(isPlaying: Boolean, isEnded: Boolean = false) {
        viewModelScope.launch {
            pipInteractor.updatePlaybackState(isPlaying, isEnded)
        }
    }

    fun enterPipMode() {
        viewModelScope.launch {
            pipInteractor.enterPipMode()
            _pipEvent.emit(PipUiEvent.PipModeEntered)
        }
    }

    fun exitPipMode() {
        viewModelScope.launch {
            pipInteractor.exitPipMode()
            _pipEvent.emit(PipUiEvent.PipModeExited)
        }
    }

    fun requestPipMode() {
        viewModelScope.launch {
            _pipEvent.emit(PipUiEvent.PipModeRequested)
        }
    }

    // --- PiP Actions ---

    fun handleAction(action: PipAction) {
        viewModelScope.launch {
            pipInteractor.handleAction(action)
        }
    }

    fun retryPlayback() {
        viewModelScope.launch {
            pipInteractor.retryPlayback()
        }
    }

    // Permission checking
    fun isPipPermissionGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun getPipActions(context: Context, isPlaying: Boolean) {
        viewModelScope.launch {
            val actions = pipInteractor.buildPipActions(context, isPlaying)
            // Emit or store actions as needed
        }
    }
}

/**
 * One-time UI events emitted by PipViewModel.
 */
sealed class PipUiEvent {
    data object PipModeEntered : PipUiEvent()
    data object PipModeExited : PipUiEvent()
    data object PipModeRequested : PipUiEvent()
}