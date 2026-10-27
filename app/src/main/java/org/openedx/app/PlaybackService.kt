package org.openedx.app

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import org.koin.android.ext.android.inject
import org.openedx.core.ExoPlayerFactory

class PlaybackService : MediaSessionService() {

    private val exoPlayerFactory: ExoPlayerFactory by inject()

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession

    override fun onCreate() {
        super.onCreate()
        try {
            player = exoPlayerFactory.createExoPlayer(this)
            mediaSession = MediaSession.Builder(this, player)
                .setId("${packageName}.SESSION_ID")
                .build()
        } catch (e: Exception) {
            stopSelf()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
        return mediaSession
    }

    override fun onDestroy() {
        try {
            mediaSession.release()
            player.release()
        } catch (e: Exception) {
        }
        super.onDestroy()
    }
}