package org.openedx.core.utils

import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.openedx.core.config.Config

/**
 * A singleton helper for Firebase Crashlytics operations.
 */
object CrashlyticsHelper : KoinComponent {

    private val config by inject<Config>()
    private val isEnabled by lazy { config.getFirebaseConfig().enabled }
    private val firebase by lazy { FirebaseCrashlytics.getInstance() }

    private inline fun runIfEnabled(invoke: FirebaseCrashlytics.() -> Unit) {
        if (isEnabled) firebase.invoke()
    }

    /**
     * Sets the current user ID in Crashlytics.
     */
    fun setUserId(userId: String) = runIfEnabled { setUserId(userId) }

    /**
     * Sets a custom key with a dynamic value type (Boolean, Number, or String).
     */
    fun setKey(key: String, value: Any) = runIfEnabled {
        when (value) {
            is Boolean -> setCustomKey(key, value)
            is Number -> setCustomKey(key, value.toDouble())
            else -> setCustomKey(key, value.toString())
        }
    }

    /**
     * Records an exception along with optional metadata as custom keys.
     */
    fun reportException(
        exception: Throwable,
        metadata: Map<String, Any> = emptyMap(),
    ) = runIfEnabled {
        metadata.forEach {
            log(it.toString())
        }
        recordException(exception)
        sendUnsentReports()
    }
}
