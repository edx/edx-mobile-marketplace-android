package org.openedx.discovery.presentation.catalog

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import org.openedx.core.extension.isEmailValid
import org.openedx.core.utils.EmailUtil

open class DefaultWebViewClient(
    val context: Context,
    val webView: WebView,
    val isAllLinksExternal: Boolean,
    val refreshSessionCookie: () -> Unit,
    val onUriClick: (String, WebViewLink.Authority) -> Unit,
    private val maxRetries: Int = 1,
) : WebViewClient() {

    private var hostForThisPage: String? = null
    private var isPossibleRedirection = true
    private var retryCount = 0
    private val retryUrls = mutableSetOf<String>()

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)

        if (hostForThisPage == null && url != null) {
            hostForThisPage = Uri.parse(url).host
        }
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val clickUrl = request?.url?.toString() ?: ""

        if (clickUrl.isNotEmpty() && (isAllLinksExternal || isExternalLink(clickUrl)) && !isPossibleRedirection) {
            onUriClick(clickUrl, WebViewLink.Authority.EXTERNAL)
            return true
        }

        return if (clickUrl.startsWith("mailto:")) {
            val email = clickUrl.replace("mailto:", "")
            if (email.isEmailValid()) {
                EmailUtil.sendEmailIntent(context, email, "", "")
                true
            } else {
                false
            }
        } else {
            false
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        isPossibleRedirection = false
    }

    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse,
    ) {
        val requestUrl = request.url.toString()
        if (requestUrl == view.url && retryCount < maxRetries) {
            when (errorResponse.statusCode) {
                403, 401, 404 -> {
                    // Track that we've retried this URL and increment retry count
                    if (!retryUrls.contains(requestUrl)) {
                        retryUrls.add(requestUrl)
                        retryCount++
                        refreshSessionCookie()
                        webView.loadUrl(requestUrl)
                        return
                    }
                }
            }
        }
        super.onReceivedHttpError(view, request, errorResponse)
    }

    private fun isExternalLink(strUrl: String?): Boolean {
        return strUrl?.let { url ->
            val uri = Uri.parse(url)
            val externalLinkValue = if (uri.isHierarchical) {
                uri.getQueryParameter(QUERY_PARAM_EXTERNAL_LINK)
            } else {
                null
            }
            hostForThisPage != null && hostForThisPage != uri.host ||
                    externalLinkValue?.toBoolean() == true
        } ?: false
    }

    companion object {
        const val QUERY_PARAM_EXTERNAL_LINK = "external_link"
    }
}
