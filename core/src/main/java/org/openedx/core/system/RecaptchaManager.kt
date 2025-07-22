package org.openedx.core.system

import com.google.android.recaptcha.RecaptchaAction
import com.google.android.recaptcha.RecaptchaClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.openedx.core.utils.Logger

class RecaptchaManager(
    private val client: RecaptchaClient,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val logger = Logger(TAG)

    suspend fun getActionThreadToken() = run(RecaptchaActionThread)

    suspend fun getActionCommentToken() = run(RecaptchaActionComment)

    private suspend fun run(action: RecaptchaAction): String = withContext(dispatcher) {
        try {
            val response = client.execute(recaptchaAction = action, timeout = 10_000L)
            response.getOrDefault("")
        } catch (e: Exception) {
            logger.e(e)
            ""
        }
    }

    companion object {
        private const val TAG = "RecaptchaManager"
        private val RecaptchaActionThread = RecaptchaAction.custom("thread")
        private val RecaptchaActionComment = RecaptchaAction.custom("comment")
    }
}
