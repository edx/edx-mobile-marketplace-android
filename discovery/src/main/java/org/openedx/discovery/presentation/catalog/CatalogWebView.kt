package org.openedx.discovery.presentation.catalog

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
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

    val XPERT_FLOATING_UI_HIDE_SCRIPT = """
(function() {
    function apply() {
        var button = document.getElementById('xpert_chatbot__floating-action-btn');
        if (button) {
            if (button.style.display !== 'none') button.style.setProperty('display', 'none', 'important');
            if (button.style.visibility !== 'hidden') button.style.setProperty('visibility', 'hidden', 'important');
        }
        document.querySelectorAll('.xpert_chatbot__proactive-message').forEach(function(message) {
            if (message.style.display !== 'none') message.style.setProperty('display', 'none', 'important');
            if (message.style.visibility !== 'hidden') message.style.setProperty('visibility', 'hidden', 'important');
        });
    }
    apply();
    if (!window.__xpertFloatingUiObserver) {
        window.__xpertFloatingUiObserver = new MutationObserver(apply);
        window.__xpertFloatingUiObserver.observe(document.documentElement, {
            childList: true,
            subtree: true
        });
    }
})();
""".trimIndent()

     val XPERT_CHAT_INITIALIZATION_SCRIPT = """
(function() {
    if (window.__xpertInitializationStarted) return;
    window.__xpertInitializationStarted = true;

    window.__expandXpertDialog = function() {
        function apply() {
            var closeButton = document.querySelector(
                'button[aria-label="Close Xpert dialog"]'
            );
            var dialog = closeButton && closeButton.closest('[role="dialog"]');
            if (!dialog) return;

            [
                ['position', 'fixed'],
                ['inset', '0'],
                ['width', '100vw'],
                ['height', '100vh'],
                ['max-width', 'none'],
                ['max-height', 'none'],
                ['margin', '0'],
                ['border-radius', '0'],
                ['transform', 'none']
            ].forEach(function(entry) {
                if (dialog.style.getPropertyValue(entry[0]) !== entry[1] ||
                    dialog.style.getPropertyPriority(entry[0]) !== 'important') {
                    dialog.style.setProperty(entry[0], entry[1], 'important');
                }
            });
        }

        apply();

        if (!window.__xpertDialogObserver) {
            window.__xpertDialogObserver = new MutationObserver(apply);
            window.__xpertDialogObserver.observe(document.documentElement, {
                childList: true,
                subtree: true
            });
        }
    };

    window.__triggerXpertChatbotAction = function() {
        var button = document.getElementById('xpert_chatbot__floating-action-btn');
        if (!button) return false;

    if (typeof window.__expandXpertDialog === 'function') {
        window.__expandXpertDialog();
    }
        button.click();

        if (window.XpertChatBridge && window.XpertChatBridge.onChatOpen) {
            window.XpertChatBridge.onChatOpen();
        }
        return true;
    };

    var attempts = 0;
    var interval = setInterval(function() {
        attempts++;
        var button = document.getElementById('xpert_chatbot__floating-action-btn');
        if (button && window.XpertChatBridge && window.XpertChatBridge.onChatReady) {
            clearInterval(interval);
            window.XpertChatBridge.onChatReady();
        }
        if (attempts >= 200) clearInterval(interval);
    });
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

        @JavascriptInterface
        fun onChatClose() {
            webView.post {
                onXpertChatStateChanged?.invoke(false)
            }
        }

    }

    webView.apply {

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

                view?.evaluateJavascript(
                    XPERT_FLOATING_UI_HIDE_SCRIPT,
                    null
                )

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

                if (enableXpertChat) {
                    view.evaluateJavascript(
                        """
        (function() {
                    var styleId = 'xpert-floating-elements-hidden';
                    if (document.getElementById(styleId)) return;
            var style = document.createElement('style');
                    style.id = styleId;

            style.textContent = `
                .xpert_chatbot__proactive-message {
                    display: none !important;
                }
            `;

            (document.head || document.documentElement)
                .appendChild(style);
                
                if (!window.__xpertCloseListenerInstalled) {
                    window.__xpertCloseListenerInstalled = true;

                    document.addEventListener('click', function(event) {

                        var closeButton = event.target.closest(
                            'button[aria-label="Close Xpert dialog"]'
                        );

                        if (closeButton) {

                            console.log("Xpert close icon clicked");

                            if (window.XpertChatBridge &&
                                window.XpertChatBridge.onChatClose) {

                                window.XpertChatBridge.onChatClose();
                            }
                        }

                    }, true);
                }

            window.__triggerXpertChatbotAction = function() {
                var btn = document.getElementById(
                    'xpert_chatbot__floating-action-btn'
                );

                if (btn) {
                    btn.click();
                    return true;
                }

                            if (!window.__xpertCloseListenerInstalled) {
                                window.__xpertCloseListenerInstalled = true;
                                document.addEventListener('click', function(event) {
                                    var target = event.target;
                                    var closeControl = target && target.closest && target.closest(
                                        '[aria-label*="close" i], ' +
                                        '[title*="close" i], ' +
                                        'button[class*="close" i], ' +
                                        '[role="button"][class*="close" i]'
                                    );

                                    if (closeControl && window.XpertChatBridge &&
                                        window.XpertChatBridge.onChatClose) {
                                        window.XpertChatBridge.onChatClose();
                                    }
                                }, true);
                            }

                return false;
            };
        })();
        
                        """.trimIndent(),
                        null
                    )
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

        settings.apply {
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
