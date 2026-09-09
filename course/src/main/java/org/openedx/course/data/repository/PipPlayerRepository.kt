package org.openedx.course.data.repository

import android.app.PendingIntent
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.openedx.course.data.repository.PipBroadcastReceiverManager.Companion.REQUEST_FORWARD
import org.openedx.course.data.repository.PipBroadcastReceiverManager.Companion.REQUEST_PAUSE
import org.openedx.course.data.repository.PipBroadcastReceiverManager.Companion.REQUEST_PLAY
import org.openedx.course.data.repository.PipBroadcastReceiverManager.Companion.REQUEST_REWIND
import org.openedx.course.data.repository.player.PlayerController
import org.openedx.course.domain.model.PipPlayerState
import org.openedx.course.domain.model.PipPlayerType

class PipPlayerRepository {

    private val _state = MutableStateFlow(PipPlayerState())
    val state: StateFlow<PipPlayerState> = _state.asStateFlow()

    private var _controller: PlayerController? = null
    val controller: PlayerController? get() = _controller


    fun registerPlayer(controller: PlayerController, playerType: PipPlayerType) {
        android.util.Log.d("PipRepo", "Registering player: $controller, type: $playerType")
        _controller = controller
        _state.value = _state.value.copy(
            playerType = playerType,
            isPlaying = controller.isPlaying(),
            isEnded = controller.isEnded(),
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

    fun enterPipMode() {
        _state.value = _state.value.copy(isPipMode = true)
    }

    fun exitPipMode() {
        _state.value = _state.value.copy(isPipMode = false)
    }

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

    fun retryPlayback() {
        controller?.apply {
            restart()  // or prepare() + play()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun buildPipActions(context: Context, isPlaying: Boolean): List<RemoteAction> {
        val playIntent = PendingIntent.getBroadcast(
            context, REQUEST_PLAY,
            Intent(PipBroadcastReceiverManager.ACTION_PLAY),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val pauseIntent = PendingIntent.getBroadcast(
            context, REQUEST_PAUSE,
            Intent(PipBroadcastReceiverManager.ACTION_PAUSE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val forwardIntent = PendingIntent.getBroadcast(
            context, REQUEST_FORWARD,
            Intent(PipBroadcastReceiverManager.ACTION_FORWARD),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val rewindIntent = PendingIntent.getBroadcast(
            context, REQUEST_REWIND,
            Intent(PipBroadcastReceiverManager.ACTION_REWIND),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val rewindAction = RemoteAction(
            android.graphics.drawable.Icon.createWithResource(context, android.R.drawable.ic_media_rew),
            "Rewind", "Rewind 10s", rewindIntent
        )
        val playPauseAction = if (isPlaying) {
            RemoteAction(
                android.graphics.drawable.Icon.createWithResource(context, android.R.drawable.ic_media_pause),
                "Pause", "Pause Video", pauseIntent
            )
        } else {
            RemoteAction(
                android.graphics.drawable.Icon.createWithResource(context, android.R.drawable.ic_media_play),
                "Play", "Play Video", playIntent
            )
        }
        val forwardAction = RemoteAction(
            android.graphics.drawable.Icon.createWithResource(context, android.R.drawable.ic_media_ff),
            "Forward", "Forward 10s", forwardIntent
        )

        return listOf(rewindAction, playPauseAction, forwardAction)
    }
}
