package org.openedx.core.system

import android.app.Application
import android.content.Context
import com.google.android.recaptcha.Recaptcha
import com.google.android.recaptcha.RecaptchaAction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.openedx.core.config.Config
import org.openedx.core.utils.Logger

class RecaptchaManager(
    private val context: Context,
    private val config: Config,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val logger = Logger(TAG)

    private val clientDeferred = CoroutineScope(dispatcher).async(start = CoroutineStart.LAZY) {
        Recaptcha.fetchClient(context as Application, config.getRecaptchaConfig().siteKey)
    }

    suspend fun getActionToken(action: RecaptchaAction): String {
        return try {
            if (!config.getRecaptchaConfig().isEnabled) return ""

            val client = clientDeferred.await()
            withContext(dispatcher) {
                client.execute(
                    recaptchaAction = action,
                    timeout = 10_000L,
                ).getOrDefault("")
            }
        } catch (e: Exception) {
            logger.e(e)
            ""
        }
    }

    companion object {
        private const val TAG = "RecaptchaManager"
        val RecaptchaActionThread = RecaptchaAction.custom("thread")
        val RecaptchaActionComment = RecaptchaAction.custom("comment")
        val RecaptchaActionRegistration = RecaptchaAction.custom("signup")
    }
}
