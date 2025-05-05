package org.openedx.course.module

import android.content.Context
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.SessionAvailabilityListener
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastState
import org.openedx.core.utils.Logger
import org.openedx.course.extension.matches
import java.util.concurrent.Executors

@androidx.annotation.OptIn(UnstableApi::class)
class CastManager(val context: Context) {

    private val logger = Logger(TAG)

    var castPlayer: CastPlayer? = null
        private set

    private var onAction: ((state: Int) -> Unit)? = null

    init {
        initializeCastPlayer()
    }

    private fun initializeCastPlayer() {
        val executor = Executors.newSingleThreadExecutor()
        CastContext.getSharedInstance(context, executor).addOnSuccessListener { castContext ->
            castPlayer = CastPlayer(castContext)
            setUpCastListener()
        }.addOnFailureListener {
            logger.e(it, true)
        }
    }

    private fun setUpCastListener() {
        castPlayer?.setSessionAvailabilityListener(object : SessionAvailabilityListener {
            override fun onCastSessionAvailable() {
                onAction?.invoke(CastState.CONNECTED)
            }

            override fun onCastSessionUnavailable() {
                onAction?.invoke(CastState.NOT_CONNECTED)
            }
        })
    }

    fun setMediaItem(mediaItem: MediaItem, currentVideoTime: Long) {
        val currentItem = castPlayer?.currentMediaItem

        if (currentItem != null && mediaItem.matches(currentItem)) {
            castPlayer?.seekTo(currentVideoTime)
            castPlayer?.playWhenReady = true
            return
        }

        castPlayer?.setMediaItem(mediaItem, currentVideoTime)
        castPlayer?.playWhenReady = true
    }

    fun attachCastPlayer(onAction: (state: Int) -> Unit) {
        this.onAction = onAction
        if (castPlayer == null) {
            initializeCastPlayer()
        } else {
            castPlayer?.isCastSessionAvailable?.let { isAvailable ->
                if (isAvailable) {
                    onAction(CastState.CONNECTED)
                }
            }
        }
    }

    fun stopPlayer() {
        castPlayer?.stop()
    }

    fun detachCastPlayer() {
        onAction = null
    }

    fun getCurrentPosition() = castPlayer?.currentPosition ?: 0L

    private companion object {
        private const val TAG = "CastManager"
    }
}
