package org.openedx.course.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Manages the lifecycle of a BroadcastReceiver for PiP remote actions.
 *
 * Handles registration/unregistration tied to fragment lifecycle (onStart/onStop).
 * Works identically for both ExoPlayer and YouTube player types.
 *
 * Injected via Koin; one instance per video fragment.
 */
class PipBroadcastReceiverManager(
    private val context: Context,
    private val pipPlayerRepository: PipPlayerRepository,
) {
    private var isRegistered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PLAY -> {
                    pipPlayerRepository.play()
                    pipPlayerRepository.updatePlaybackState(isPlaying = true, isEnded = false)
                }
                ACTION_PAUSE -> {
                    pipPlayerRepository.pause()
                    pipPlayerRepository.updatePlaybackState(isPlaying = false)
                }
                ACTION_FORWARD -> {
                    pipPlayerRepository.seekForward()
                }
                ACTION_REWIND -> {
                    pipPlayerRepository.seekBackward()
                }
            }
        }
    }

    /**
     * Register the broadcast receiver. Safe to call multiple times.
     * Should be called in Fragment.onStart().
     */
    fun register() {
        if (isRegistered) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter().apply {
                    addAction(ACTION_PLAY)
                    addAction(ACTION_PAUSE)
                    addAction(ACTION_FORWARD)
                    addAction(ACTION_REWIND)
                },
                ContextCompat.RECEIVER_EXPORTED,
            )
            isRegistered = true
        }
    }

    /**
     * Unregister the broadcast receiver. Safe to call multiple times.
     * Should be called in Fragment.onStop() when NOT in PiP mode.
     */
    fun unregister() {
        if (!isRegistered) return
        try {
            context.unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) {
            // Already unregistered
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
