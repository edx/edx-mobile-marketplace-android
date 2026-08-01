package org.openedx.course.presentation.unit.video

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

@OptIn(UnstableApi::class)
class MediaPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Service created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: Service started")
        startForegroundWithNotification()
        return START_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        Log.d(TAG, "onGetSession: Returning mediaSession")
        return mediaSession
    }

    fun setMediaSession(session: MediaSession?) {
        Log.d(TAG, "setMediaSession: Setting media session, session=$session")
        mediaSession = session

        // Update notification when session is set
        if (mediaSession != null) {
            Log.d(TAG, "setMediaSession: MediaSession set successfully")
            startForegroundWithNotification()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Course Video Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notification for course video playback"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
            Log.d(TAG, "createNotificationChannel: Channel created")
        }
    }

    private fun startForegroundWithNotification() {
        try {
            val notification = createNotification()
            startForeground(NOTIFICATION_ID, notification)
            Log.d(TAG, "startForegroundWithNotification: Foreground service started")
        } catch (e: Exception) {
            Log.e(TAG, "startForegroundWithNotification: Error - ${e.message}", e)
        }
    }

    private fun createNotification(): Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent ?: Intent(),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Course Video")
            .setContentText("Video is playing in background")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: Service destroyed")
        mediaSession?.release()
        mediaSession = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        super.onDestroy()
    }

    companion object {
        private var instance: MediaPlaybackService? = null
        private const val TAG = "MediaPlaybackService"
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_CHANNEL_ID = "course_video_playback"

        fun getInstance(): MediaPlaybackService? {
            Log.d(TAG, "getInstance: Returning instance")
            return instance
        }
    }

    init {
        Log.d(TAG, "init: Service instance created")
        instance = this
    }
}