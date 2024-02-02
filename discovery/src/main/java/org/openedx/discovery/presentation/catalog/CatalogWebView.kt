package org.openedx.discovery.presentation.catalog

import android.annotation.SuppressLint
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import org.openedx.core.extension.applyDarkModeIfEnabled
import org.openedx.core.extension.equalsHost
import org.openedx.discovery.presentation.catalog.WebViewLink.Authority as linkAuthority

@SuppressLint("SetJavaScriptEnabled", "ComposableNaming")
@Composable
fun CatalogWebViewScreen(
    url: String,
    uriScheme: String,
    isAllLinksExternal: Boolean = false,
    onWebPageLoaded: () -> Unit,
    refreshSessionCookie: () -> Unit = {},
    onWebPageUpdated: (String) -> Unit = {},
    onUriClick: (String, linkAuthority) -> Unit,
    onWebPageLoadError: () -> Unit
): WebView {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    return remember {
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
                    onWebPageLoaded()
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
                    if (view.url.equalsHost(request.url.host)) {
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
            }
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false

            loadUrl(url)
            applyDarkModeIfEnabled(isDarkTheme)
        }
    }
}
