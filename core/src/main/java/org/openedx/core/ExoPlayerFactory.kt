package org.openedx.core

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer

interface ExoPlayerFactory {
    fun createExoPlayer(context: Context): ExoPlayer
}

class ExoPlayerFactoryImpl : ExoPlayerFactory {
    override fun createExoPlayer(context: Context): ExoPlayer {
        return ExoPlayer.Builder(context)
            .build()
    }
}