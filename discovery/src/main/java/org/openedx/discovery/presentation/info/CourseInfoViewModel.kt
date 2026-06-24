package org.openedx.discovery.presentation.info

import android.content.Context
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openedx.core.BaseViewModel
import org.openedx.core.UIMessage
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.interactor.IAPInteractor
import org.openedx.core.domain.model.iap.IAPFlow
import org.openedx.core.domain.model.iap.IAPFlowSource
import org.openedx.core.domain.model.iap.ProductInfo
import org.openedx.core.domain.model.iap.PurchaseFlowData
import org.openedx.core.exception.iap.IAPException
import org.openedx.core.extension.isInternetError
import org.openedx.core.extension.toIAPException
import org.openedx.core.module.billing.BillingProcessor
import org.openedx.core.module.billing.getCourseId
import org.openedx.core.module.billing.getPriceAmount
import org.openedx.core.presentation.CoreAnalyticsKey
import org.openedx.core.presentation.IAPAnalytics
import org.openedx.core.presentation.global.AppData
import org.openedx.core.presentation.global.ErrorType
import org.openedx.core.presentation.global.webview.WebViewUIState
import org.openedx.core.presentation.iap.IAPAction
import org.openedx.core.presentation.iap.IAPEventLogger
import org.openedx.core.presentation.iap.IAPLoaderType
import org.openedx.core.presentation.iap.IAPRequestType
import org.openedx.core.presentation.iap.IAPUIState
import org.openedx.core.system.AppCookieManager
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseDashboardUpdate
import org.openedx.core.system.notifier.DiscoveryNotifier
import org.openedx.core.utils.Logger
import org.openedx.core.utils.TimeUtils
import org.openedx.discovery.R
import org.openedx.discovery.domain.interactor.DiscoveryInteractor
import org.openedx.discovery.presentation.DiscoveryAnalytics
import org.openedx.discovery.presentation.DiscoveryAnalyticsEvent
import org.openedx.discovery.presentation.DiscoveryAnalyticsKey
import org.openedx.discovery.presentation.DiscoveryRouter
import org.openedx.discovery.presentation.catalog.WebViewLink
import java.util.concurrent.atomic.AtomicReference
import org.openedx.core.R as CoreR

class CourseInfoViewModel(
    val pathId: String,
    val infoType: String,
    private val appData: AppData,
    private val config: Config,
    private val networkConnection: NetworkConnection,
    private val router: DiscoveryRouter,
    private val interactor: DiscoveryInteractor,
    private val notifier: DiscoveryNotifier,
    private val resourceManager: ResourceManager,
    private val analytics: DiscoveryAnalytics,
    private val edxCookieManager: AppCookieManager,
    corePreferences: CorePreferences,
    private val appCookieManager: AppCookieManager,
    private val iapInteractor: IAPInteractor,
    iapAnalytics: IAPAnalytics,
) : BaseViewModel() {
    private val logger = Logger(TAG)

    // ── existing UI state ─────────────────────────────────────────────────────
    private val _uiState =
        MutableStateFlow(
            CourseInfoUIState.CourseInfo(
                initialUrl = getInitialUrl(),
                isPreLogin = config.isPreLoginExperienceEnabled() && corePreferences.user == null
            )
        )
    internal val uiState: StateFlow<CourseInfoUIState> = _uiState

    private val _webViewUIState = MutableStateFlow<WebViewUIState>(WebViewUIState.Loading)
    val webViewState
        get() = _webViewUIState.asStateFlow()

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage: SharedFlow<UIMessage>
        get() = _uiMessage.asSharedFlow()

    private val _showAlert = MutableSharedFlow<Boolean>()
    val showAlert: SharedFlow<Boolean>
        get() = _showAlert.asSharedFlow()

    private val _cookiesReady = MutableStateFlow(false)
    val cookiesReady: StateFlow<Boolean> = _cookiesReady.asStateFlow()

    // ── IAP state ─────────────────────────────────────────────────────────────
    val purchaseFlowData = PurchaseFlowData()
    val eventLogger = IAPEventLogger(analytics = iapAnalytics, purchaseFlowData = purchaseFlowData)

    private val _iapState = MutableStateFlow<IAPUIState>(IAPUIState.Clear)
    val iapState: StateFlow<IAPUIState> = _iapState.asStateFlow()

    /** True while we should auto-start the purchase as soon as the price is loaded. */
    var shouldAutoStartPurchase = false
        private set

    private val purchaseListeners = object : BillingProcessor.PurchaseListeners {
        override fun onPurchaseComplete(purchase: Purchase) {
            if (purchase.getCourseId() == purchaseFlowData.courseId) {
                _iapState.value = IAPUIState.Loading(loaderType = IAPLoaderType.FULL_SCREEN)
                purchaseFlowData.purchaseToken = purchase.purchaseToken
                createOrder(purchaseFlowData)
            }
        }

        override fun onPurchaseCancel(responseCode: Int, message: String) {
            updateErrorState(
                IAPException(
                    IAPRequestType.PAYMENT_SDK_CODE,
                    httpErrorCode = responseCode,
                    errorMessage = message
                )
            )
        }
    }

    // ── misc getters ──────────────────────────────────────────────────────────
    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

    val uriScheme: String get() = config.getUriScheme()

    val appUserAgent get() = appData.appUserAgent

    val cookieManager get() = edxCookieManager

    private val webViewConfig get() = config.getDiscoveryConfig().webViewConfig

    // ── initial URL ───────────────────────────────────────────────────────────
    private fun getInitialUrl(): String {
        val urlTemplate = when (infoType) {
            WebViewLink.Authority.COURSE_INFO.name -> webViewConfig.courseUrlTemplate
            WebViewLink.Authority.PROGRAM_INFO.name -> webViewConfig.programUrlTemplate
            else -> webViewConfig.baseUrl
        }
        return if (pathId.isEmpty() || infoType.isEmpty()) {
            webViewConfig.baseUrl
        } else {
            urlTemplate.replace("{${ARG_PATH_ID}}", pathId)
        }
    }

    // ── enrollment ────────────────────────────────────────────────────────────
    fun enrollInACourse(courseId: String) {
        viewModelScope.launch {
            _showAlert.emit(false)
            try {
                val isCourseEnrolled = withContext(Dispatchers.IO) {
                    interactor.getCourseDetails(courseId)
                }.isEnrolled

                if (isCourseEnrolled) {
                    _uiMessage.emit(
                        UIMessage.ToastMessage(resourceManager.getString(R.string.discovery_you_are_already_enrolled))
                    )
                    _uiState.update {
                        it.copy(
                            hadEnrollment = AtomicReference(true),
                            enrollmentSuccess = AtomicReference(courseId)
                        )
                    }
                    return@launch
                }

                interactor.enrollInACourse(courseId)
                courseEnrollSuccessEvent(courseId)
                notifier.send(CourseDashboardUpdate())
                _uiMessage.emit(
                    UIMessage.ToastMessage(resourceManager.getString(R.string.discovery_enrolled_successfully))
                )
                _uiState.update {
                    it.copy(
                        hadEnrollment = AtomicReference(false),
                        enrollmentSuccess = AtomicReference(courseId)
                    )
                }
            } catch (e: Exception) {
                logger.e(throwable = e, metadata = mapOf("courseId" to courseId))
                if (e.isInternetError()) {
                    _uiMessage.emit(
                        UIMessage.SnackBarMessage(resourceManager.getString(CoreR.string.core_error_no_connection))
                    )
                } else {
                    _showAlert.emit(true)
                }
            }
        }
    }

    fun onSuccessfulCourseEnrollment(fragmentManager: FragmentManager, courseId: String, showTrackSelection: Boolean = false) {
        if (courseId.isNotEmpty()) {
            router.navigateToCourseOutline(
                fm = fragmentManager,
                courseId = courseId,
                courseTitle = "",
                showTrackSelection = showTrackSelection
            )
        }
    }

    fun infoCardClicked(fragmentManager: FragmentManager, pathId: String, infoType: String) {
        if (pathId.isNotEmpty() && infoType.isNotEmpty()) {
            router.navigateToCourseInfo(
                fm = fragmentManager,
                courseId = pathId,
                infoType = infoType
            )
        }
    }

    fun enrolledProgramInfoClicked(fragmentManager: FragmentManager, pathId: String) {
        if (pathId.isNotEmpty()) {
            router.navigateToEnrolledProgramInfo(
                fm = fragmentManager,
                pathId = pathId,
            )
        }
    }

    fun navigateToSignUp(fragmentManager: FragmentManager, courseId: String?, infoType: String) {
        router.navigateToSignUp(fragmentManager, courseId, infoType)
    }

    fun navigateToSignIn(fragmentManager: FragmentManager, courseId: String, infoType: String) {
        router.navigateToSignIn(fragmentManager, courseId, infoType)
    }

    // ── IAP purchase flow ─────────────────────────────────────────────────────

    /**
     * Parses the [rawLink] URL (earn_certificate link), populates [purchaseFlowData],
     * and triggers a price load.  The fragment should call [startPurchaseFlow] once
     * [iapState] transitions to [IAPUIState.ProductData] and [shouldAutoStartPurchase]
     * is true.
     */
    fun setupAndLoadPurchase(rawLink: String) {
        val uri = runCatching {
            Uri.parse(rawLink.replace("+", "%2B"))
        }.getOrNull()

        val courseId = uri?.getQueryParameter(WebViewLink.Param.COURSE_ID)
            ?.takeIf { it.isNotBlank() }
            ?: uri?.getQueryParameter(WebViewLink.Param.PATH_ID)
                ?.takeIf { it.isNotBlank() }
            ?: rawLink.takeIf { it.isNotBlank() && !it.contains("://") }

        val storeSku = uri?.getQueryParameter(WebViewLink.Param.STORE_SKU)
            ?.takeIf { it.isNotBlank() }
            ?: uri?.getQueryParameter("sku").orEmpty()

        val price = uri?.getQueryParameter(WebViewLink.Param.PRICE)?.toDoubleOrNull() ?: 0.0
        val title = uri?.getQueryParameter(WebViewLink.Param.TITLE)?.takeIf { it.isNotBlank() }

        if (courseId.isNullOrBlank() || storeSku.isBlank()) {
            updateErrorState(
                IAPException(
                    requestType = IAPRequestType.NO_SKU_CODE,
                    httpErrorCode = IAPRequestType.NO_SKU_CODE.hashCode(),
                    errorMessage = ""
                )
            )
            return
        }

        purchaseFlowData.apply {
            this.courseId = courseId
            this.courseName = title
            this.iapFlow = IAPFlow.USER_INITIATED
            this.screenName = IAPFlowSource.COURSE_ENROLLMENT.screen
            this.productInfo = ProductInfo(storeSku = storeSku, lmsUSDPrice = price)
        }
        shouldAutoStartPurchase = true
        loadPrice()
    }

    fun loadPrice() {
        eventLogger.loadIAPScreenEvent()
        viewModelScope.launch(Dispatchers.IO) {
            purchaseFlowData.takeIf { it.courseId != null && it.productInfo != null }
                ?.apply {
                    _iapState.value = IAPUIState.Loading(loaderType = IAPLoaderType.PRICE)
                    runCatching {
                        iapInteractor.loadPrice(purchaseFlowData.productInfo?.storeSku!!)
                    }.onSuccess {
                        this.formattedPrice = it.formattedPrice
                        this.price = it.getPriceAmount()
                        this.currencyCode = it.priceCurrencyCode
                        _iapState.value = IAPUIState.ProductData(formattedPrice = it.formattedPrice)
                    }.onFailure {
                        logger.e(throwable = it)
                        updateErrorState(it)
                    }
                } ?: run {
                updateErrorState(
                    IAPException(
                        requestType = IAPRequestType.PRICE_CODE,
                        httpErrorCode = IAPRequestType.PRICE_CODE.hashCode(),
                        errorMessage = ""
                    )
                )
            }
        }
    }

    /**
     * Starts the purchase flow directly (no certificate-preview dialog), matching
     * [org.openedx.course.presentation.container.CourseContainerViewModel.startPurchaseFlow].
     * Call this from the Fragment once [iapState] == [IAPUIState.ProductData] and
     * [shouldAutoStartPurchase] is true.
     */
    fun startPurchaseFlow(activity: FragmentActivity) {
        eventLogger.upgradeNowClickedEvent()
        _iapState.value = IAPUIState.Loading(loaderType = IAPLoaderType.PURCHASE_FLOW)
        purchaseFlowData.flowStartTime = TimeUtils.getCurrentTime()
        shouldAutoStartPurchase = false

        if (purchaseFlowData.productInfo == null) {
            updateErrorState(
                IAPException(
                    requestType = IAPRequestType.NO_SKU_CODE,
                    httpErrorCode = IAPRequestType.NO_SKU_CODE.hashCode(),
                    errorMessage = ""
                )
            )
            return
        }
        // Go directly to billing — skip certificate-preview dialog.
        purchaseItem(activity)
    }

    private fun purchaseItem(activity: FragmentActivity) {
        viewModelScope.launch(Dispatchers.IO) {
            takeIf { purchaseFlowData.productInfo != null }?.apply {
                iapInteractor.purchaseItem(
                    activity,
                    purchaseFlowData.courseId!!,
                    purchaseFlowData.productInfo!!,
                    purchaseListeners
                )
            }
        }
    }

    private fun createOrder(purchaseFlowData: PurchaseFlowData) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                iapInteractor.createOrder(
                    courseId = purchaseFlowData.courseId!!,
                    currencyCode = purchaseFlowData.currencyCode,
                    price = purchaseFlowData.price,
                    purchaseToken = purchaseFlowData.purchaseToken!!,
                )
            }.onSuccess {
                consumeOrderForFurtherPurchases(purchaseFlowData)
            }.onFailure {
                logger.e(throwable = it)
                updateErrorState(it)
            }
        }
    }

    private fun consumeOrderForFurtherPurchases(purchaseFlowData: PurchaseFlowData) {
        if (purchaseFlowData.isConsumed) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                if (purchaseFlowData.purchaseToken.isNullOrEmpty()) {
                    iapInteractor.consumePurchaseByCourseId(purchaseFlowData.courseId!!)
                } else {
                    iapInteractor.consumePurchaseByToken(purchaseFlowData.purchaseToken!!)
                }
            }.onSuccess {
                eventLogger.upgradeSuccessEvent()
                purchaseFlowData.isConsumed = true
                _iapState.value = IAPUIState.CourseDataUpdated
                _uiMessage.emit(
                    UIMessage.ToastMessage(
                        resourceManager.getString(CoreR.string.iap_success_message)
                    )
                )
            }.onFailure {
                logger.e(throwable = it)
                updateErrorState(it)
            }
        }
    }

    fun refreshCourse() {
        _iapState.value = IAPUIState.Loading(IAPLoaderType.FULL_SCREEN)
        purchaseFlowData.flowStartTime = TimeUtils.getCurrentTime()
        consumeOrderForFurtherPurchases(purchaseFlowData)
    }

    fun retryCreateOrder() {
        createOrder(purchaseFlowData)
    }

    fun retryToConsumeOrder() {
        consumeOrderForFurtherPurchases(purchaseFlowData)
    }

    fun clearIAPState() {
        _iapState.value = IAPUIState.Clear
        purchaseFlowData.resetSessionData()
        shouldAutoStartPurchase = false
    }

    fun isFullScreenLoading(): Boolean {
        return _iapState.value is IAPUIState.Loading &&
                (_iapState.value as IAPUIState.Loading).loaderType == IAPLoaderType.FULL_SCREEN
    }

    fun showFeedbackScreen(context: Context, flowType: String, message: String) {
        iapInteractor.showFeedbackScreen(context, message)
        eventLogger.logIAPErrorActionEvent(flowType, IAPAction.ACTION_GET_HELP.action)
    }

    private fun updateErrorState(
        throwable: Throwable,
        requestType: IAPRequestType = IAPRequestType.UNKNOWN,
    ) {
        val iapException = throwable.toIAPException(
            requestType = requestType,
            defaultMessage = resourceManager.getString(CoreR.string.core_error_unknown_error)
        )
        eventLogger.logExceptionEvent(iapException)
        if (BillingClient.BillingResponseCode.USER_CANCELED != iapException.httpErrorCode) {
            _iapState.value = IAPUIState.Error(iapException)
        } else {
            clearIAPState()
        }
    }

    // ── analytics ─────────────────────────────────────────────────────────────
    fun courseInfoClickedEvent(courseId: String) {
        logScreenEvent(DiscoveryAnalyticsEvent.COURSE_INFO, courseId)
    }

    fun programInfoClickedEvent(courseId: String) {
        logScreenEvent(DiscoveryAnalyticsEvent.PROGRAM_INFO, courseId)
    }

    fun courseEnrollClickedEvent(courseId: String) {
        logEvent(DiscoveryAnalyticsEvent.COURSE_ENROLL_CLICKED, courseId)
    }

    private fun courseEnrollSuccessEvent(courseId: String) {
        logEvent(DiscoveryAnalyticsEvent.COURSE_ENROLL_SUCCESS, courseId)
    }

    private fun logEvent(event: DiscoveryAnalyticsEvent, courseId: String) {
        analytics.logEvent(event.eventName, buildEventDataMap(event, courseId))
    }

    private fun logScreenEvent(event: DiscoveryAnalyticsEvent, courseId: String) {
        analytics.logScreenEvent(event.eventName, buildEventDataMap(event, courseId))
    }

    private fun buildEventDataMap(event: DiscoveryAnalyticsEvent, courseId: String): Map<String, String> {
        return buildMap {
            put(DiscoveryAnalyticsKey.NAME.key, event.biValue)
            put(DiscoveryAnalyticsKey.COURSE_ID.key, courseId)
            put(DiscoveryAnalyticsKey.CATEGORY.key, CoreAnalyticsKey.DISCOVERY.key)
            put(DiscoveryAnalyticsKey.CONVERSION.key, courseId)
        }
    }

    // ── web-view helpers ──────────────────────────────────────────────────────
    fun onWebPageLoaded() {
        _webViewUIState.value = WebViewUIState.Loaded
    }

    fun onWebPageError() {
        _webViewUIState.value =
            WebViewUIState.Error(if (networkConnection.isOnline()) ErrorType.UNKNOWN_ERROR else ErrorType.CONNECTION_ERROR)
    }

    fun onWebPageLoading() {
        _webViewUIState.value = WebViewUIState.Loading
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

    init {
        checkAndRefreshCookies()
    }

    companion object {
        private const val TAG = "CourseInfoViewModel"
        private const val ARG_PATH_ID = "path_id"
    }
}
