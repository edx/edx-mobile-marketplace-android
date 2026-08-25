package org.openedx.core.presentation

import android.webkit.WebView

interface WebViewTrackingAnalytics {
    fun enableWebViewTracking(webView: WebView, url: String)
}

