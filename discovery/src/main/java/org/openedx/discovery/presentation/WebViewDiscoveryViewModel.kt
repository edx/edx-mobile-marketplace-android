package org.openedx.discovery.presentation

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.module.subscriptionBanner.SubscriptionAlertBanner
import org.openedx.core.presentation.global.AppData
import org.openedx.core.presentation.global.ErrorType
import org.openedx.core.presentation.global.webview.WebViewUIState
import org.openedx.core.system.AppCookieManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.utils.Logger
import org.openedx.core.utils.UrlUtils

class WebViewDiscoveryViewModel(
    private val querySearch: String,
    private val appData: AppData,
    private val config: Config,
    private val networkConnection: NetworkConnection,
    private val corePreferences: CorePreferences,
    private val router: DiscoveryRouter,
    private val analytics: DiscoveryAnalytics,
    private val appCookieManager: AppCookieManager,
    private val subscriptionAlertBanner: SubscriptionAlertBanner,
) : BaseViewModel() {

    private val logger = Logger("WebViewDiscoveryViewModel")

    private val _uiState = MutableStateFlow<WebViewUIState>(WebViewUIState.Loading)
    val uiState: StateFlow<WebViewUIState> = _uiState.asStateFlow()

    private val _cookiesReady = MutableStateFlow(false)
    val cookiesReady: StateFlow<Boolean> = _cookiesReady.asStateFlow()

    private val _isSubscriptionBannerVisible = MutableStateFlow(false)
    val isSubscriptionBannerVisible: StateFlow<Boolean> = _isSubscriptionBannerVisible.asStateFlow()

    val subscriptionBannerUrl: String
        get() = subscriptionAlertBanner.getBannerUrl()

    val uriScheme: String get() = config.getUriScheme()

    private val webViewConfig get() = config.getDiscoveryConfig().webViewConfig

    val isPreLogin get() = config.isPreLoginExperienceEnabled() && corePreferences.user == null

    val appUserAgent get() = appData.appUserAgent

    private var _discoveryUrl = webViewConfig.baseUrl
    val discoveryUrl: String
        get() {
            return if (querySearch.isNotBlank()) {
                val queryParams: MutableMap<String, String> = HashMap()
                queryParams[UrlUtils.QUERY_PARAM_SEARCH] = querySearch
                UrlUtils.buildUrlWithQueryParams(_discoveryUrl, queryParams)
            } else {
                _discoveryUrl
            }
        }

    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

    init {
        refreshSubscriptionBannerVisibility()
        checkAndRefreshCookies()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        refreshSubscriptionBannerVisibility()
    }

    fun refreshSubscriptionBannerVisibility() {
        _isSubscriptionBannerVisible.value = subscriptionAlertBanner.isBannerVisible(
            SubscriptionAlertBanner.Screen.DISCOVERY
        )
    }

    private fun checkAndRefreshCookies() {
        viewModelScope.launch {
            try {
                if (appCookieManager.isSessionCookieMissingOrExpired()) {
                    appCookieManager.tryToRefreshSessionCookie()
                }
            } catch (e: Exception) {
                logger.e(throwable = e)
            } finally {
                _cookiesReady.value = true
            }
        }
    }

    fun refreshSessionCookie() {
        viewModelScope.launch {
            try {
                if (appCookieManager.isSessionCookieMissingOrExpired()) {
                    appCookieManager.tryToRefreshSessionCookie()
                }
            } catch (e: Exception) {
                logger.e(throwable = e)
            }
        }
    }

    fun onWebPageLoading() {
        _uiState.value = WebViewUIState.Loading
    }

    fun onWebPageLoaded() {
        _uiState.value = WebViewUIState.Loaded
    }

    fun onWebPageLoadError() {
        _uiState.value =
            WebViewUIState.Error(if (networkConnection.isOnline()) ErrorType.UNKNOWN_ERROR else ErrorType.CONNECTION_ERROR)
    }

    fun updateDiscoveryUrl(url: String) {
        if (url.isNotEmpty()) {
            _discoveryUrl = url
        }
    }

    fun infoCardClicked(fragmentManager: FragmentManager, pathId: String, infoType: String) {
        if (pathId.isNotEmpty() && infoType.isNotEmpty()) {
            router.navigateToCourseInfo(
                fragmentManager,
                pathId,
                infoType
            )
        }
    }

    fun navigateToSignUp(fragmentManager: FragmentManager) {
        router.navigateToSignUp(fragmentManager, null)
    }

    fun navigateToSignIn(fragmentManager: FragmentManager) {
        router.navigateToSignIn(fragmentManager, null, null)
    }

    fun courseInfoClickedEvent(courseId: String) {
        logEvent(DiscoveryAnalyticsEvent.COURSE_INFO, courseId)
    }

    fun programInfoClickedEvent(courseId: String) {
        logEvent(DiscoveryAnalyticsEvent.PROGRAM_INFO, courseId)
    }

    private fun logEvent(
        event: DiscoveryAnalyticsEvent,
        courseId: String,
    ) {
        analytics.logScreenEvent(
            event.eventName,
            buildMap {
                put(DiscoveryAnalyticsKey.NAME.key, event.biValue)
                put(DiscoveryAnalyticsKey.COURSE_ID.key, courseId)
                put(DiscoveryAnalyticsKey.CATEGORY.key, DiscoveryAnalyticsKey.DISCOVERY.key)
            }
        )
    }

    fun dismissSubscriptionBanner() {
        subscriptionAlertBanner.dismiss(SubscriptionAlertBanner.Screen.DISCOVERY)
        _isSubscriptionBannerVisible.value = false
    }
}
