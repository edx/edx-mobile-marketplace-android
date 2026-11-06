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

    // ==========================================
    // HIDE COOKIE CONSENT POPUP UI
    // ==========================================

    const cookieSelectors = [
        '#onetrust-banner-sdk',
        '#onetrust-consent-sdk',
        '#CybotCookiebotDialog',
        '.cky-consent-container',
        '.osano-cm-window',
        '.qc-cmp2-container',
        '#cookie-banner',
        '#cookie-consent',
        '.cookie-banner',
        '.cookie-consent',
        '.cookie-popup',
        '.cookie-notice',
        '.cc-window',
        '.cc-banner',
        '[class*="cookie-banner"]',
        '[class*="cookie-consent"]',
        '[id*="cookie-banner"]',
        '[id*="cookie-consent"]',
        '[id*="consent-banner"]',
        '[class*="CookieConsent"]',
        '[id*="CookieConsent"]'
    ];

    function hideCookiePopup() {
        cookieSelectors.forEach(function(selector) {
            document.querySelectorAll(selector).forEach(function(element) {
                element.style.setProperty('display', 'none', 'important');
                element.style.setProperty('visibility', 'hidden', 'important');
                element.style.setProperty('opacity', '0', 'important');
                element.style.setProperty('pointer-events', 'none', 'important');
            });
        });
    }

    // Hide existing cookie popups
    hideCookiePopup();

    // Observe dynamically added cookie popups
    const cookieObserver = new MutationObserver(function() {
        hideCookiePopup();
    });

    cookieObserver.observe(document.documentElement, {
        childList: true,
        subtree: true
    });


    // ==========================================
    // XPERT CHATBOT CUSTOMIZATION
    // ==========================================

    var style = document.createElement('style');
    style.id = 'xpert-hide-style';

    style.textContent = `
        /* Hide Xpert floating button */
        #xpert_chatbot__floating-action-btn {
            display: none !important;
            visibility: hidden !important;
            opacity: 0 !important;
        }

        /* Hide Xpert greeting messages */
        [class*="xpert"][class*="greeting"],
        [class*="xpert"][class*="welcome"],
        [class*="xpert"][class*="message"],
        .xpert-greeting,
        .xpert-welcome-message,
        #xpert-greeting,
        #xpert-welcome {
            display: none !important;
            visibility: hidden !important;
            opacity: 0 !important;
        }

        /* Full-screen chatbot dialog */
        [role="dialog"] {
            position: fixed !important;
            width: 100% !important;
            height: 100dvh !important;
            max-height: 100dvh !important;
            top: 0 !important;
            left: 0 !important;
            right: 0 !important;
            bottom: 0 !important;
            border-radius: 0 !important;
            margin: 0 !important;
            z-index: 999999 !important;
            overflow: hidden !important;
            display: flex !important;
            flex-direction: column !important;
        }

        /* Keep chatbot input visible */
        [role="dialog"] textarea,
        [role="dialog"] input,
        [role="dialog"] [contenteditable="true"] {
            scroll-margin-bottom: 10px;
        }

        /* Hide common cookie popup UI */
        #onetrust-banner-sdk,
        #onetrust-consent-sdk,
        #CybotCookiebotDialog,
        .cky-consent-container,
        .osano-cm-window,
        .qc-cmp2-container,
        #cookie-banner,
        #cookie-consent,
        .cookie-banner,
        .cookie-consent,
        .cookie-popup,
        .cookie-notice,
        .cc-window,
        .cc-banner,
        [class*="cookie-banner"],
        [class*="cookie-consent"],
        [id*="cookie-banner"],
        [id*="cookie-consent"],
        [id*="consent-banner"],
        [class*="CookieConsent"],
        [id*="CookieConsent"] {
            display: none !important;
            visibility: hidden !important;
            opacity: 0 !important;
            pointer-events: none !important;
        }
    `;

    (document.head || document.documentElement).appendChild(style);


    // ==========================================
    // MONITOR XPERT CHAT MODAL
    // ==========================================

    var wasModalOpen = false;

    setInterval(function() {

        var modal = document.querySelector('[role="dialog"]');

        var isModalOpen = modal !== null &&
            modal.getClientRects().length > 0 &&
            window.getComputedStyle(modal).visibility !== 'hidden';

        var viewportHeight = window.visualViewport
            ? window.visualViewport.height
            : window.innerHeight;

        if (isModalOpen && modal) {
            modal.style.setProperty('width', '100%', 'important');
            modal.style.setProperty('height', viewportHeight + 'px', 'important');
            modal.style.setProperty('max-width', '100%', 'important');
            modal.style.setProperty('max-height', '100%', 'important');
            modal.style.setProperty('top', '0', 'important');
            modal.style.setProperty('left', '0', 'important');
            modal.style.setProperty('right', '0', 'important');
            modal.style.setProperty('bottom', '0', 'important');
            modal.style.setProperty('border-radius', '0', 'important');
            modal.style.setProperty('margin', '0', 'important');
        }

        if (isModalOpen !== wasModalOpen) {
            wasModalOpen = isModalOpen;

            if (window.XpertChatBridge) {
                if (isModalOpen) {
                    window.XpertChatBridge.onChatOpen();
                } else {
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
                    'display: none !important; visibility: hidden !important;';
            }
        }

    }, 300);


    // ==========================================
    // XPERT CHATBOT TRIGGER
    // ==========================================

    window.__triggerXpertChatbotAction = function() {

        var btn = document.getElementById(
            'xpert_chatbot__floating-action-btn'
        );

        if (btn) {
            btn.style.cssText =
                'display: block !important; visibility: visible !important;';

            btn.click();
        }
    };


    // ==========================================
    // KEYBOARD VISIBILITY HANDLING
    // ==========================================

    if (window.visualViewport) {

        window.visualViewport.addEventListener('resize', function() {

            var modal = document.querySelector('[role="dialog"]');

            if (!modal) return;

            var viewportHeight = window.visualViewport.height;

            modal.style.setProperty(
                'height',
                viewportHeight + 'px',
                'important'
            );

            modal.style.setProperty(
                'max-height',
                viewportHeight + 'px',
                'important'
            );

            var activeElement = document.activeElement;

            if (
                activeElement &&
                (
                    activeElement.tagName === 'INPUT' ||
                    activeElement.tagName === 'TEXTAREA' ||
                    activeElement.isContentEditable
                )
            ) {
                setTimeout(function() {
                    activeElement.scrollIntoView({
                        behavior: 'smooth',
                        block: 'center'
                    });
                }, 150);
            }
        });
    }


    // ==========================================
    // CHECK FOR 404 PAGE
    // ==========================================

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

        with(settings) {
            javaScriptEnabled = true
            loadWithOverviewMode = false
            useWideViewPort = true
            builtInZoomControls = false
            setSupportZoom(false)
            loadsImagesAutomatically = true
            domStorageEnabled = true
            userAgentString = "$userAgentString $userAgent"
            setSupportMultipleWindows(false)
        }

        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false

        webViewTrackingAnalytics.enableWebViewTracking(this, url)

        applyDarkModeIfEnabled(isDarkTheme)

        loadUrl(url)
    }
}