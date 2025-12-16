package org.openedx.core.extension

import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.openedx.core.system.AppCookieManager
import org.openedx.core.utils.Logger

fun WebView.loadUrl(url: String, scope: CoroutineScope, cookieManager: AppCookieManager) {
    if (cookieManager.isSessionCookieMissingOrExpired()) {
        scope.launch {
            cookieManager.tryToRefreshSessionCookie()
            loadUrl(url)
        }
    } else {
        loadUrl(url)
    }
}

fun WebView.applyDarkModeIfEnabled(isDarkTheme: Boolean) {
    if (isDarkTheme && WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
        try {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, true)
        } catch (e: Exception) {
            Logger("ViewExt").e(throwable = e)
        }
    }
}
