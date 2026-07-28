package org.openedx.course.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat

class PipBroadcastReceiverManager(
    private val context: Context,
    private val pipPlayerRepository: PipPlayerRepository,
) {
    private var isRegistered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val action = intent?.action
            android.util.Log.d("PipReceiver", "Action received: $action, Controller: ${pipPlayerRepository.controller}")

            when (action) {
                ACTION_PLAY -> {
                    android.util.Log.d("PipReceiver", "Playing...")
                    pipPlayerRepository.play()
                    pipPlayerRepository.updatePlaybackState(isPlaying = true, isEnded = false)
                }
                ACTION_PAUSE -> {
                    android.util.Log.d("PipReceiver", "Pausing...")
                    pipPlayerRepository.pause()
                    pipPlayerRepository.updatePlaybackState(isPlaying = false)
                }
                ACTION_FORWARD -> {
                    android.util.Log.d("PipReceiver", "Seeking forward...")
                    pipPlayerRepository.seekForward()
                }
                ACTION_REWIND -> {
                    android.util.Log.d("PipReceiver", "Seeking backward...")
                    pipPlayerRepository.seekBackward()
                }
            }
        }
    }

    fun register() {
        if (isRegistered) return
        try {
            val intentFilter = IntentFilter().apply {
                addAction(ACTION_PLAY)
                addAction(ACTION_PAUSE)
                addAction(ACTION_FORWARD)
                addAction(ACTION_REWIND)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.registerReceiver(
                    context,
                    receiver,
                    intentFilter,
                    ContextCompat.RECEIVER_EXPORTED,
                )
            } else {
                ContextCompat.registerReceiver(
                    context,
                    receiver,
                    intentFilter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
            }
            isRegistered = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun unregister() {
        if (!isRegistered) return
        try {
            context.unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) {
        }
        isRegistered = false
    }

    companion object {
        const val ACTION_PLAY = "pip_play"
        const val ACTION_PAUSE = "pip_pause"
        const val ACTION_FORWARD = "pip_forward"
        const val ACTION_REWIND = "pip_rewind"

        const val REQUEST_PLAY = 101
        const val REQUEST_PAUSE = 102
        const val REQUEST_FORWARD = 103
        const val REQUEST_REWIND = 104
    }
}
