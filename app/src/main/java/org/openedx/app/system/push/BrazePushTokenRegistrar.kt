package org.openedx.app.system.push

import android.content.Context
import com.braze.Braze
import org.openedx.core.config.Config
import org.openedx.core.utils.Logger

interface PushTokenRegistrar {
    fun register(token: String)
}

class BrazePushTokenRegistrar(
    private val context: Context,
    private val config: Config,
) : PushTokenRegistrar {

    private val logger = Logger(TAG)

    override fun register(token: String) {
        if (token.isBlank() || !isBrazePushEnabled()) return

        try {
            Braze.getInstance(context).registeredPushToken = token
            logger.d { "Braze push token registered" }
        } catch (e: Exception) {
            logger.e(throwable = e)
        }
    }

    private fun isBrazePushEnabled(): Boolean {
        val brazeConfig = config.getBrazeConfig()
        val firebaseConfig = config.getFirebaseConfig()
        return brazeConfig.isEnabled && brazeConfig.isPushNotificationsEnabled &&
                firebaseConfig.enabled && firebaseConfig.isCloudMessagingEnabled
    }

    private companion object {
        const val TAG = "BrazePushTokenRegistrar"
    }
}

class NoOpPushTokenRegistrar : PushTokenRegistrar {
    override fun register(token: String) = Unit
}
