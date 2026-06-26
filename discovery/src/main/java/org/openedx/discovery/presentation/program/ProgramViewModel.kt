package org.openedx.discovery.presentation.program

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
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.core.config.Config
import org.openedx.core.domain.interactor.IAPInteractor
import org.openedx.core.domain.model.iap.PurchaseFlowData
import org.openedx.core.exception.iap.IAPException
import org.openedx.core.extension.isInternetError
import org.openedx.core.extension.toIAPException
import org.openedx.core.module.billing.BillingProcessor
import org.openedx.core.module.billing.getCourseId
import org.openedx.core.module.billing.getPriceAmount
import org.openedx.core.presentation.IAPAnalytics
import org.openedx.core.presentation.global.AppData
import org.openedx.core.presentation.global.ErrorType
import org.openedx.core.presentation.iap.IAPEventLogger
import org.openedx.core.presentation.iap.IAPLoaderType
import org.openedx.core.presentation.iap.IAPRequestType
import org.openedx.core.presentation.iap.IAPUIState
import org.openedx.core.system.AppCookieManager
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseDashboardUpdate
import org.openedx.core.system.notifier.DiscoveryNotifier
import org.openedx.core.system.notifier.NavigationToDiscovery
import org.openedx.core.utils.Logger
import org.openedx.core.utils.TimeUtils
import org.openedx.discovery.domain.interactor.DiscoveryInteractor
import org.openedx.discovery.domain.interactor.ProgramIAPInteractor
import org.openedx.discovery.presentation.DiscoveryRouter
import org.openedx.core.R as CoreR

class ProgramViewModel(
    private val appData: AppData,
    private val config: Config,
    private val networkConnection: NetworkConnection,
    private val router: DiscoveryRouter,
    private val notifier: DiscoveryNotifier,
    private val edxCookieManager: AppCookieManager,
    private val resourceManager: ResourceManager,
    private val interactor: DiscoveryInteractor,
    private val iapInteractor: IAPInteractor,
    private val programIAPInteractor: ProgramIAPInteractor,
    iapAnalytics: IAPAnalytics,
) : BaseViewModel() {
    private val logger = Logger(TAG)

    val uriScheme: String get() = config.getUriScheme()

    val programConfig get() = config.getProgramConfig().webViewConfig

    val cookieManager get() = edxCookieManager

    val hasInternetConnection: Boolean get() = networkConnection.isOnline()

    val appUserAgent get() = appData.appUserAgent

    private val _uiState = MutableStateFlow<ProgramUIState>(ProgramUIState.Loading)
    val uiState: StateFlow<ProgramUIState> get() = _uiState.asStateFlow()

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage: SharedFlow<UIMessage>
        get() = _uiMessage.asSharedFlow()

    // ── IAP state ─────────────────────────────────────────────────────────────
    val purchaseFlowData = PurchaseFlowData()
    val eventLogger = IAPEventLogger(analytics = iapAnalytics, purchaseFlowData = purchaseFlowData)

    private val _iapState = MutableStateFlow<IAPUIState>(IAPUIState.Clear)
    val iapState: StateFlow<IAPUIState> = _iapState.asStateFlow()

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

    fun showLoading(isLoading: Boolean) {
        viewModelScope.launch {
            _uiState.emit(if (isLoading) ProgramUIState.Loading else ProgramUIState.Loaded)
        }
    }

    fun enrollInACourse(courseId: String) {
        showLoading(true)
        viewModelScope.launch {
            try {
                interactor.enrollInACourse(courseId)
                _uiState.emit(ProgramUIState.CourseEnrolled(courseId, true))
                notifier.send(CourseDashboardUpdate())
            } catch (e: Exception) {
                logger.e(throwable = e, metadata = mapOf("courseId" to courseId))
                if (e.isInternetError()) {
                    _uiState.emit(
                        ProgramUIState.UiMessage(
                            UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                        )
                    )
                } else {
                    _uiState.emit(ProgramUIState.CourseEnrolled(courseId, false))
                }
            }
        }
    }

    fun onProgramCardClick(fragmentManager: FragmentManager, pathId: String) {
        if (pathId.isNotEmpty()) {
            router.navigateToEnrolledProgramInfo(fm = fragmentManager, pathId = pathId)
        }
    }

    fun onViewCourseClick(fragmentManager: FragmentManager, courseId: String, infoType: String) {
        if (courseId.isNotEmpty() && infoType.isNotEmpty()) {
            router.navigateToCourseInfo(
                fm = fragmentManager,
                courseId = courseId,
                infoType = infoType
            )
        }
    }

    fun onEnrolledCourseClick(fragmentManager: FragmentManager, courseId: String, showTrackSelection: Boolean = false) {
        if (courseId.isNotEmpty()) {
            router.navigateToCourseOutline(
                fm = fragmentManager,
                courseId = courseId,
                courseTitle = "",
                showTrackSelection = showTrackSelection
            )
        }
        viewModelScope.launch {
            _uiState.emit(ProgramUIState.Loaded)
        }
    }

    fun navigateToDiscovery() {
        viewModelScope.launch { notifier.send(NavigationToDiscovery()) }
    }

    fun onPageLoadError() {
        viewModelScope.launch {
            _uiState.emit(ProgramUIState.Error(if (networkConnection.isOnline()) ErrorType.UNKNOWN_ERROR else ErrorType.CONNECTION_ERROR))
        }
    }

    // ── IAP purchase flow ─────────────────────────────────────────────────────

    fun setupAndLoadPurchase(rawLink: String, pathId: String) {
        logger.d({ "setupAndLoadPurchase: rawLink = $rawLink" })

        try {
            val programPurchaseData = programIAPInteractor.parsePurchaseLink(rawLink, pathId)

            purchaseFlowData.apply {
                this.courseId = programPurchaseData.courseId
                this.courseName = programPurchaseData.courseName
                this.iapFlow = programPurchaseData.iapFlow
                this.screenName = programPurchaseData.screenName
                this.productInfo = programPurchaseData.productInfo
            }

            shouldAutoStartPurchase = true
            loadPrice()

        } catch (e: Exception) {
            logger.e(throwable = e)
            updateErrorState(e)
        }
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

    fun clearIAPState() {
        _iapState.value = IAPUIState.Clear
        purchaseFlowData.resetSessionData()
        shouldAutoStartPurchase = false
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

    companion object {
        private const val TAG = "ProgramViewModel"
    }
}
