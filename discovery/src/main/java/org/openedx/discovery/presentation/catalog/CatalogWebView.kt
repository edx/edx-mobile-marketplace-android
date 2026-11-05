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

    @Suppress("unused")
    class XpertChatBridge {
        @android.webkit.JavascriptInterface
        fun onChatOpen() {
            webView.post { onXpertChatStateChanged?.invoke(true) }
        }

        @android.webkit.JavascriptInterface
        fun onChatClose() {
            webView.post { onXpertChatStateChanged?.invoke(false) }
        }
    }

    webView.addJavascriptInterface(XpertChatBridge(), "XpertChatBridge")

    webView.apply {
        webViewClient = object : DefaultWebViewClient(
            context = context,
            webView = this@apply,
            isAllLinksExternal = isAllLinksExternal,
            onUriClick = onUriClick,
            refreshSessionCookie = refreshSessionCookie,
        ) {
            override fun onPageFinished(view: WebView?, url: String?) {
                url?.let { onWebPageUpdated(it) }
                super.onPageFinished(view, url)
            }

            override fun onPageCommitVisible(view: WebView?, url: String?) {
                super.onPageCommitVisible(view, url)

                view?.evaluateJavascript(
                    """
(function() {

    // Hide the Xpert floating button and greeting.
    var style = document.createElement('style');
    style.id = 'xpert-hide-style';

    style.textContent = `
        #xpert_chatbot__floating-action-btn {
            display: none !important;
            visibility: hidden !important;
            opacity: 0 !important;
        }

        [class*="xpert"][class*="greeting"],
        [class*="xpert"][class*="welcome"],
        .xpert-greeting,
        .xpert-welcome-message,
        #xpert-greeting,
        #xpert-welcome {
            display: none !important;
            visibility: hidden !important;
            opacity: 0 !important;
        }

        [role="dialog"] {
    position: fixed !important;
    width: 100vw !important;
    height: 100dvh !important;
    max-width: 100vw !important;
    max-height: 100dvh !important;
    top: 0 !important;
    left: 0 !important;
    right: 0 !important;
    bottom: 0 !important;
    border-radius: 0 !important;
    margin: 0 !important;
    padding-bottom: 0 !important;
    box-sizing: border-box !important;
    z-index: 2147483647 !important;
}
    `;

    (document.head || document.documentElement).appendChild(style);

    var wasModalOpen = false;
    var hiddenElements = [];

    function hideDiscoveryContent(modal) {

        hiddenElements = [];

        // Hide siblings of the modal and its ancestors,
        // preserving the modal itself.
        var current = modal;

        while (current && current !== document.body) {

            var parent = current.parentElement;

            if (!parent) break;

            Array.from(parent.children).forEach(function(element) {

                if (element !== current) {

                    hiddenElements.push({
                        element: element,
                        visibility: element.style.visibility
                    });

                    element.style.setProperty(
                        'visibility',
                        'hidden',
                        'important'
                    );
                }
            });

            current = parent;
        }
    }

    function restoreDiscoveryContent() {

        hiddenElements.forEach(function(item) {
            item.element.style.visibility = item.visibility;
        });

        hiddenElements = [];
    }

    function checkModal() {

        var modal = document.querySelector('[role="dialog"]');

        var isModalOpen =
            modal !== null &&
            modal.getClientRects().length > 0 &&
            window.getComputedStyle(modal).visibility !== 'hidden';

        if (isModalOpen && modal) {

            modal.style.setProperty('position', 'fixed', 'important');
            modal.style.setProperty('width', '100vw', 'important');
            modal.style.setProperty('height', '100dvh', 'important');
            modal.style.setProperty('max-height', '100dvh', 'important');
            modal.style.setProperty('min-height', '0', 'important');
            modal.style.setProperty('padding-bottom', '0', 'important');
            modal.style.setProperty('box-sizing', 'border-box', 'important');
            modal.style.setProperty('max-width', '100vw', 'important');
            modal.style.setProperty('top', '0', 'important');
            modal.style.setProperty('left', '0', 'important');
            modal.style.setProperty('right', '0', 'important');
            modal.style.setProperty('bottom', '0', 'important');
            modal.style.setProperty('border-radius', '0', 'important');
            modal.style.setProperty('margin', '0', 'important');
            modal.style.setProperty('z-index', '2147483647', 'important');
        }

        if (isModalOpen !== wasModalOpen) {

            wasModalOpen = isModalOpen;

            if (isModalOpen) {
                hideDiscoveryContent(modal);

                if (window.XpertChatBridge) {
                    window.XpertChatBridge.onChatOpen();
                }

            } else {

                restoreDiscoveryContent();

                if (window.XpertChatBridge) {
                    window.XpertChatBridge.onChatClose();
                }
            }
        }

        if (!isModalOpen) {
            var btn = document.getElementById(
                'xpert_chatbot__floating-action-btn'
            );

            if (btn) {
                btn.style.cssText =
                    'display:none !important; visibility:hidden !important;';
            }
        }
    }

    setInterval(checkModal, 300);

    window.__triggerXpertChatbotAction = function() {

    var btn = document.getElementById(
        'xpert_chatbot__floating-action-btn'
    );

    if (!btn) {
        console.log('Xpert chatbot button not found');
        return false;
    }

    btn.style.setProperty('display', 'block', 'important');
    btn.style.setProperty('visibility', 'visible', 'important');

    btn.click();

    return true;
};

    const text = document.body.innerText || "";

    return text.includes("Page not found") ||
           text.includes("404");

})();
"""
                ) { result ->
                    if (result == "true") {
                        onWebPageLoadError()
                    } else {
                        onWebPageLoaded()
                        onXpertChatReady?.invoke(view)
                    }
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?, request: WebResourceRequest?
            ): Boolean {
                val clickUrl = request?.url?.toString() ?: ""
                if (handleRecognizedLink(clickUrl)) {
                    return true
                }

                return super.shouldOverrideUrlLoading(view, request)
            }

            private fun handleRecognizedLink(clickUrl: String): Boolean {
                val link = WebViewLink.parse(clickUrl, uriScheme) ?: return false

                return when (link.authority) {
                    linkAuthority.COURSE_INFO,
                    linkAuthority.PROGRAM_INFO,
                    linkAuthority.ENROLLED_PROGRAM_INFO -> {
                        val pathId = link.params[WebViewLink.Param.PATH_ID] ?: ""
                        onUriClick(pathId, link.authority)
                        true
                    }

                    linkAuthority.ENROLL,
                    linkAuthority.ENROLLED_COURSE_INFO -> {
                        val courseId = link.params[WebViewLink.Param.COURSE_ID] ?: ""
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
                view: WebView, request: WebResourceRequest, error: WebResourceError
            ) {
                if (request.url.toString() == view.url) {
                    onWebPageLoadError()
                }
                super.onReceivedError(view, request, error)
            }
        }

        with(settings) {
            javaScriptEnabled = true
            loadWithOverviewMode = false
            useWideViewPort = true
            builtInZoomControls = false
            setSupportZoom(false)
            loadsImagesAutomatically = true
            domStorageEnabled = true
            userAgentString = "$userAgentString $userAgent"
        }
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        webViewTrackingAnalytics.enableWebViewTracking(this, url)
        loadUrl(url)
        applyDarkModeIfEnabled(isDarkTheme)
    }
}