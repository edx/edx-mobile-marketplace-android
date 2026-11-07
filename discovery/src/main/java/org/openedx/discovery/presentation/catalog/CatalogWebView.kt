package org.openedx.discovery.presentation.catalog

import android.annotation.SuppressLint
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import org.koin.compose.koinInject
import org.openedx.core.extension.applyDarkModeIfEnabled
import org.openedx.core.presentation.WebViewTrackingAnalytics
import org.openedx.discovery.presentation.catalog.WebViewLink.Authority as linkAuthority

@SuppressLint("SetJavaScriptEnabled", "ComposableNaming")
@Composable
fun CatalogWebViewScreen(
    url: String,
    uriScheme: String,
    userAgent: String,
    isAllLinksExternal: Boolean = false,
    onWebPageLoaded: () -> Unit,
    refreshSessionCookie: () -> Unit = {},
    onWebPageUpdated: (String) -> Unit = {},
    onUriClick: (String, linkAuthority) -> Unit,
    onWebPageLoadError: () -> Unit,
    onXpertChatReady: ((WebView) -> Unit)? = null,
): WebView {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    val webViewTrackingAnalytics = koinInject<WebViewTrackingAnalytics>()
    val webView = remember {
        WebView(context).apply {
            configureCatalogWebView(
                webView = this,
                url = url,
                uriScheme = uriScheme,
                userAgent = userAgent,
                isDarkTheme = isDarkTheme,
                webViewTrackingAnalytics = webViewTrackingAnalytics,
                isAllLinksExternal = isAllLinksExternal,
                onWebPageLoaded = onWebPageLoaded,
                refreshSessionCookie = refreshSessionCookie,
                onWebPageUpdated = onWebPageUpdated,
                onUriClick = onUriClick,
                onWebPageLoadError = onWebPageLoadError,
                onXpertChatReady = onXpertChatReady,
            )
        }
    }
    return webView
}


@SuppressLint("SetJavaScriptEnabled")
fun configureCatalogWebView(
    webView: WebView,
    url: String,
    uriScheme: String,
    userAgent: String,
    isDarkTheme: Boolean,
    webViewTrackingAnalytics: WebViewTrackingAnalytics,
     enableXpertChat: Boolean = false,
    isAllLinksExternal: Boolean = false,
    onWebPageLoaded: () -> Unit,
    refreshSessionCookie: () -> Unit = {},
    onWebPageUpdated: (String) -> Unit = {},
    onUriClick: (String, linkAuthority) -> Unit,
    onWebPageLoadError: () -> Unit,
    onXpertChatReady: ((WebView) -> Unit)? = null,
    onXpertChatStateChanged: ((Boolean) -> Unit)? = null,
) {
    val context = webView.context
     val XPERT_CHAT_INITIALIZATION_SCRIPT = """
(function() {
    if (window.__xpertInitializationStarted) {
        return;
    }

    window.__xpertInitializationStarted = true;

    window.__triggerXpertChatbotAction = function() {
        var button = document.getElementById(
            'xpert_chatbot__floating-action-btn'
        );

        if (!button) {
            return false;
        }

        button.click();
        return true;
    };

    var attempts = 0;
    var maxAttempts = 200;

    var interval = setInterval(function() {

        attempts++;

        var button = document.getElementById(
            'xpert_chatbot__floating-action-btn'
        );

        if (button) {

            clearInterval(interval);

            if (window.XpertChatBridge &&
                window.XpertChatBridge.onChatReady) {

                window.XpertChatBridge.onChatReady();
            }
        }

        if (attempts >= maxAttempts) {
            clearInterval(interval);
        }

    }, 300);

})();
""".trimIndent()
    class XpertChatBridge {

        @android.webkit.JavascriptInterface
        fun onChatReady() {
            webView.post {
                onXpertChatReady?.invoke(webView)
            }
        }

        @android.webkit.JavascriptInterface
        fun onChatOpen() {
            webView.post {
                onXpertChatStateChanged?.invoke(true)
            }
        }

        @android.webkit.JavascriptInterface
        fun onChatClose() {
            webView.post {
                onXpertChatStateChanged?.invoke(false)
            }
        }
    }

    webView.apply {

        // Register Android bridge before loading the page.
        addJavascriptInterface(XpertChatBridge(), "XpertChatBridge")

        webViewClient = object : DefaultWebViewClient(
            context = context,
            webView = this@apply,
            isAllLinksExternal = isAllLinksExternal,
            onUriClick = onUriClick,
            refreshSessionCookie = refreshSessionCookie,
        ) {

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                url?.let { onWebPageUpdated(it) }

                onWebPageLoaded()

                if (enableXpertChat) {
                    view?.evaluateJavascript(
                        XPERT_CHAT_INITIALIZATION_SCRIPT,
                        null
                    )
                }
            }

            override fun onPageCommitVisible(view: WebView?, url: String?) {
                super.onPageCommitVisible(view, url)

                view ?: return

                // Inject Xpert-specific JavaScript only for the Xpert WebView.
                view.evaluateJavascript(
                    """
        (function() {
            var style = document.createElement('style');

            style.textContent = `
                #xpert_chatbot__floating-action-btn {
                    display: none !important;
                }

                .xpert_chatbot__proactive-message {
                    display: none !important;
                }
            `;

            (document.head || document.documentElement)
                .appendChild(style);

            window.__triggerXpertChatbotAction = function() {
                var btn = document.getElementById(
                    'xpert_chatbot__floating-action-btn'
                );

                if (btn) {
                    btn.click();
                    return true;
                }

                return false;
            };
        })();
        """.trimIndent(),
                    null
                )
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val clickUrl = request?.url?.toString() ?: ""

                if (handleRecognizedLink(clickUrl)) {
                    return true
                }

                return super.shouldOverrideUrlLoading(view, request)
            }


            private fun handleRecognizedLink(clickUrl: String): Boolean {
                val link = WebViewLink.parse(clickUrl, uriScheme)
                    ?: return false

                return when (link.authority) {

                    linkAuthority.COURSE_INFO,
                    linkAuthority.PROGRAM_INFO,
                    linkAuthority.ENROLLED_PROGRAM_INFO -> {

                        val pathId =
                            link.params[WebViewLink.Param.PATH_ID] ?: ""

                        onUriClick(pathId, link.authority)
                        true
                    }

                    linkAuthority.ENROLL,
                    linkAuthority.ENROLLED_COURSE_INFO -> {

                        val courseId =
                            link.params[WebViewLink.Param.COURSE_ID] ?: ""

                        onUriClick(courseId, link.authority)
                        true
                    }

                    linkAuthority.COURSE -> {
                        onUriClick("", link.authority)
                        true
                    }

                    else -> false
                }
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.url.toString() == view.url) {
                    onWebPageLoadError()
                }

                super.onReceivedError(view, request, error)
            }
        }

        settings.apply {
            javaScriptEnabled = true
            loadWithOverviewMode = false
            useWideViewPort = true
            builtInZoomControls = false
            setSupportZoom(false)
            loadsImagesAutomatically = true
            domStorageEnabled = true
            userAgentString = "$userAgentString $userAgent"

            // Important for keyboard interaction.
            setSupportMultipleWindows(false)
        }

        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false

        webViewTrackingAnalytics.enableWebViewTracking(this, url)

        applyDarkModeIfEnabled(isDarkTheme)

        loadUrl(url)
    }
}
	