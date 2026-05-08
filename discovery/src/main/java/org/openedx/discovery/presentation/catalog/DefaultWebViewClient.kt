package org.openedx.discovery.presentation.catalog

import android.content.Context
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.net.toUri
import org.openedx.core.extension.isEmailValid
import org.openedx.core.utils.EmailUtil

open class DefaultWebViewClient(
    val context: Context,
    val webView: WebView,
    val isAllLinksExternal: Boolean,
    val refreshSessionCookie: () -> Unit,
    val onUriClick: (String, WebViewLink.Authority) -> Unit,
    val trustedHosts: Set<String> = emptySet(),
    val alwaysExternalHosts: Set<String> = emptySet(),
) : WebViewClient() {

    private var hostForThisPage: String? = null
    private var hasRetried = false
    private var hasPendingUserNavigation = false

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)

        if (hostForThisPage == null && url != null) {
            hostForThisPage = url.toUri().host
        }
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val clickUrl = request?.url?.toString() ?: ""

        val isHttpNavigation = clickUrl.startsWith("http://") || clickUrl.startsWith("https://")
        val hasGesture = request?.hasGesture() == true

        if (isHttpNavigation && hasGesture) {
            hasPendingUserNavigation = true
        }
        val isUserInitiatedNavigation = hasGesture || hasPendingUserNavigation

        val shouldOpenExternally = clickUrl.isNotEmpty() && (isAllLinksExternal || isExternalLink(clickUrl))

        if (isTrustedLogoutUrl(clickUrl)) {
            if (isUserInitiatedNavigation) {
                onUriClick(clickUrl, WebViewLink.Authority.EXTERNAL)
            }
            hasPendingUserNavigation = false
            return true
        }
        if (shouldOpenExternally && isUserInitiatedNavigation) {
            hasPendingUserNavigation = false
            onUriClick(clickUrl, WebViewLink.Authority.EXTERNAL)
            return true
        }


        return if (clickUrl.startsWith("mailto:")) {
            val email = clickUrl.replace("mailto:", "")
            if (email.isEmailValid()) {
                hasPendingUserNavigation = false
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
        hasPendingUserNavigation = false
    }

    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse,
    ) {
        hasPendingUserNavigation = false
        if (request.url.toString() == view.url && !hasRetried) {
            when (errorResponse.statusCode) {
                403, 401, 404 -> {
                    hasRetried = true
                    refreshSessionCookie()
                    webView.loadUrl(request.url.toString())
                }
            }
        }
        super.onReceivedHttpError(view, request, errorResponse)
    }

    private fun isExternalLink(strUrl: String?): Boolean {
        return strUrl?.let { url ->
            val uri = url.toUri()
            val host = uri.host ?: return@let false
            val externalLinkValue = if (uri.isHierarchical) {
                uri.getQueryParameter(QUERY_PARAM_EXTERNAL_LINK)
            } else {
                null
            }

            if (externalLinkValue?.toBoolean() == true) return@let true

            if (isAlwaysExternalUrl(uri)) return@let true

             if (isTrustedDomain(host)) return@let false

            (hostForThisPage != null && hostForThisPage != host) ||
                    externalLinkValue?.toBoolean() == true
        } ?: false
    }

    private fun isTrustedLogoutUrl(strUrl: String?): Boolean {
        return strUrl?.let { url ->
            val uri = url.toUri()
            val host = uri.host ?: return@let false
            if (isAlwaysExternalUrl(uri)) return@let false
            if (!isTrustedDomain(host)) return@let false

            val normalizedPath = uri.path?.trimEnd('/') ?: return@let false
            normalizedPath.equals("/logout", ignoreCase = true)
        } ?: false
    }

    private fun isAlwaysExternalUrl(uri: android.net.Uri): Boolean {
        val host = uri.host ?: return false
        return isAlwaysExternalHost(host) || isKnownExternalPath(uri.path)
    }

    private fun isAlwaysExternalHost(host: String): Boolean {
        return isKnownExternalHost(host) || alwaysExternalHosts.any { externalHost ->
            host == externalHost || host.endsWith(".$externalHost")
        }
    }

    private fun isKnownExternalHost(host: String): Boolean {
        val normalizedHost = host.lowercase()
        return normalizedHost.contains("commerce") ||
                normalizedHost.contains("checkout") ||
                normalizedHost.contains("payment")
    }

    private fun isKnownExternalPath(path: String?): Boolean {
        val normalizedPath = path?.trimEnd('/')?.lowercase() ?: return false
        return EXTERNAL_PATH_PREFIXES.any { normalizedPath.startsWith(it) }
    }

    private fun isTrustedDomain(host: String): Boolean {
            return trustedHosts.contains(host)
    }

    companion object {
        const val QUERY_PARAM_EXTERNAL_LINK = "external_link"

        private val EXTERNAL_PATH_PREFIXES = listOf(
            "/lms/payment_page_redirect",
            "/payment_page_redirect",
            "/course_modes/choose",
            "/verify_student/start-flow",
            "/basket",
            "/checkout",
        )
    }
}
