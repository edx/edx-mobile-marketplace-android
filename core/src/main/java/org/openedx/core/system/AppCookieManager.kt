package org.openedx.core.system

import android.webkit.CookieManager
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Cookie
import okhttp3.RequestBody
import org.openedx.core.config.Config
import org.openedx.core.data.api.CookiesApi
import org.openedx.core.utils.Logger
import retrofit2.Response
import java.util.concurrent.TimeUnit

class AppCookieManager(private val config: Config, private val api: CookiesApi) {

    companion object {
        private const val TAG = "AppCookieManager"
        private val FRESHNESS_INTERVAL = TimeUnit.HOURS.toMillis(1)
    }

    private val logger = Logger(TAG)
    private var authSessionCookieExpiration: Long = -1
    private var response: Response<RequestBody>? = null

    suspend fun tryToRefreshSessionCookie() {
        try {
            response = api.userCookies()
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            clearWebViewCookie(cookieManager)
            val cookieTargets = buildSet {
                add(config.getApiHostURL())
                add(config.getDiscoveryConfig().webViewConfig.baseUrl)
                add(config.getProgramConfig().webViewConfig.programUrl)
            }.filter { url ->
                url.isNotBlank() && isValidCookieUrl(url)
            }
            for (cookie in Cookie.parseAll(response!!.raw().request.url, response!!.headers())) {
                val cookieValue = cookie.toString()
                cookieTargets.forEach { target ->
                    setCookie(cookieManager, target, cookieValue)
                }
            }
            cookieManager.flush()
            authSessionCookieExpiration = System.currentTimeMillis() + FRESHNESS_INTERVAL
        } catch (e: Exception) {
            logger.e(
                throwable = e,
                metadata = mapOf("url" to response?.raw()?.request?.url.toString())
            )
        }
    }

    fun clearWebViewCookie() {
        CookieManager.getInstance().removeAllCookies(null)
        authSessionCookieExpiration = -1
    }

    private suspend fun clearWebViewCookie(cookieManager: CookieManager) {
        suspendCancellableCoroutine { continuation ->
            cookieManager.removeAllCookies {
                if (continuation.isActive) {
                    continuation.resume(Unit)
                }
            }
        }
        authSessionCookieExpiration = -1
    }

    private suspend fun setCookie(cookieManager: CookieManager, url: String, cookie: String) {
        suspendCancellableCoroutine { continuation ->
            cookieManager.setCookie(url, cookie) {
                if (continuation.isActive) {
                    continuation.resume(Unit)
                }
            }
        }
    }

    private fun isValidCookieUrl(url: String): Boolean {
        return try {
            val uri = java.net.URI(url)
            val scheme = uri.scheme?.lowercase() ?: return false
            (scheme == "http" || scheme == "https") && uri.host != null
        } catch (e: Exception) {
            false
        }
    }

    fun isSessionCookieMissingOrExpired(): Boolean {
        return authSessionCookieExpiration < System.currentTimeMillis()
    }
}
