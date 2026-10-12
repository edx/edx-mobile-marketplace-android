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
                        // Hide the Xpert chatbot floating button initially
                        var style = document.createElement('style');
                        style.id = 'xpert-hide-style';
                        style.textContent = `
                            #xpert_chatbot__floating-action-btn { display: none !important; }
                            [class*="xpert"], 
                            [id*="xpert"],
                            [class*="chatbot"],
                            [id*="chatbot"] { display: none !important; }
                        `;
                        (document.head || document.documentElement).appendChild(style);

                        // Set up the trigger function for the chatbot
                        window.__triggerXpertChatbotAction = function() {
                            // Remove the hide style to show the chatbot
                            var hideStyle = document.getElementById('xpert-hide-style');
                            if (hideStyle) {
                                hideStyle.remove();
                            }
                            
                            var btn = document.getElementById('xpert_chatbot__floating-action-btn');
                            if (btn) { 
                                btn.style.display = 'block';
                                btn.click(); 
                            }
                        };
                        
                        // Notify the native side
                        window.webkit.messageHandlers.xpertChatbotReady.postMessage('');
                        
                        // Check for 404 page
                        const text = document.body.innerText || "";
                        return text.includes("Page not found") || text.includes("404");
                    })();
                    """
                    ) { result ->
                        if (result == "true") {
                            onWebPageLoadError()
                        } else {
                            onWebPageLoaded()
                            // Notify that WebView is ready
                            onXpertChatReady?.invoke(view!!)
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
                loadWithOverviewMode = true
                builtInZoomControls = false
                setSupportZoom(true)
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
    return webView
}