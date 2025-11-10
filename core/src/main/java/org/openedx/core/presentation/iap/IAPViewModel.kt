package org.openedx.core.presentation.iap

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.openedx.core.AppDataConstants
import org.openedx.core.BaseViewModel
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.interactor.IAPInteractor
import org.openedx.core.domain.model.iap.IAPFlow
import org.openedx.core.domain.model.iap.IAPFlowSource
import org.openedx.core.domain.model.iap.PurchaseFlowData
import org.openedx.core.exception.iap.IAPException
import org.openedx.core.extension.isNull
import org.openedx.core.extension.toIAPException
import org.openedx.core.feature.FeatureManager
import org.openedx.core.feature.FeatureRequests
import org.openedx.core.module.billing.BillingProcessor
import org.openedx.core.module.billing.getCourseId
import org.openedx.core.module.billing.getPriceAmount
import org.openedx.core.presentation.IAPAnalytics
import org.openedx.core.presentation.global.AppData
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.notifier.CourseDataUpdated
import org.openedx.core.system.notifier.IAPNotifier
import org.openedx.core.system.notifier.UpdateCourseData
import org.openedx.core.utils.Logger
import org.openedx.core.utils.TimeUtils

class IAPViewModel(
    private val purchaseFlowData: PurchaseFlowData,
    private val iapInteractor: IAPInteractor,
    private val resourceManager: ResourceManager,
    private val iapNotifier: IAPNotifier,
    private val config: Config,
    private val featureManager: FeatureManager,
    val appData: AppData,
    corePreferences: CorePreferences,
    analytics: IAPAnalytics,
) : BaseViewModel() {
    private val logger = Logger(TAG)

    private val _uiState = MutableStateFlow<IAPUIState>(IAPUIState.Loading(IAPLoaderType.PRICE))
    val uiState: StateFlow<IAPUIState>
        get() = _uiState.asStateFlow()

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage: SharedFlow<UIMessage>
        get() = _uiMessage.asSharedFlow()

    val purchaseData: PurchaseFlowData
        get() = purchaseFlowData

    val eventLogger = IAPEventLogger(
        analytics = analytics,
        isSilentIAPFlow = purchaseData.isSilentIAPFlow(),
        purchaseFlowData = purchaseData
    )

    val user = corePreferences.user

    private var checkingCourseMode: Boolean = false
    private val remoteConfig: FirebaseRemoteConfig by lazy {
        FirebaseRemoteConfig.getInstance()
    }
   /*
    var isCertificatePreviewEnabled: Boolean = false
        private set
        */
    private val _isCertificatePreviewEnabled = MutableStateFlow(false)
    val isCertificatePreviewEnabled: StateFlow<Boolean> = _isCertificatePreviewEnabled

    private val purchaseListeners = object : BillingProcessor.PurchaseListeners {
        override fun onPurchaseComplete(purchase: Purchase) {
            if (purchase.getCourseId() == purchaseFlowData.courseId) {
                _uiState.value = IAPUIState.Loading(loaderType = IAPLoaderType.FULL_SCREEN)
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
    private fun setupRemoteConfig() {
        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(1) // 1 hour
            .build()
        remoteConfig.setConfigSettingsAsync(settings)
        remoteConfig.setDefaultsAsync(mapOf("show_certificate_preview" to false))
    }

    private fun fetchRemoteConfig() {
        viewModelScope.launch {
            try {
                remoteConfig.fetchAndActivate().await()
                    val value = remoteConfig.getBoolean("show_certificate_preview")
                _isCertificatePreviewEnabled.value = value
            } catch (e: Exception) {
            }
        }
    }
    init {
        viewModelScope.launch {
            setupRemoteConfig()
            fetchRemoteConfig()
//            if (config.getOptimizelyConfig().enabled) {
//                isCertificatePreviewEnabled =
//                    featureManager.getDecision(FeatureRequests.ValuePropCertificatePreview)
//                        ?.getBoolean(FeatureRequests.CertificatePreviewEnabled.key, false) ?: false
//            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            iapNotifier.notifier.onEach { event ->
                when (event) {
                    is CourseDataUpdated.CourseEnrollmentDataUpdated -> {
                        if (purchaseFlowData.screenName in listOf(
                                IAPFlowSource.COURSE_ENROLLMENT.screen,
                                IAPFlowSource.PROFILE.screen,
                            )
                        ) {
                            val isVerifiedMode =
                                event.courseId == purchaseFlowData.courseId && event.isVerifiedMode
                            processCourseModeCheck(isVerifiedMode)
                        }
                    }

                    is CourseDataUpdated.CourseDashboardDataUpdate -> {
                        if (purchaseFlowData.screenName in listOf(
                                IAPFlowSource.COURSE_DASHBOARD.screen,
                                IAPFlowSource.TRACK_SELECTION.screen,
                                IAPFlowSource.COURSE_COMPONENT.screen,
                            )
                        ) {
                            val isVerifiedMode =
                                event.courseId == purchaseFlowData.courseId && event.isVerifiedMode
                            processCourseModeCheck(isVerifiedMode)
                        }
                    }
                }
            }.distinctUntilChanged().launchIn(viewModelScope)
        }

        if (iapInteractor.isUpgradeEnabled) {
            when (purchaseFlowData.iapFlow) {
                IAPFlow.USER_INITIATED -> {
                    if (purchaseFlowData.screenName == IAPFlowSource.COURSE_COMPONENT.screen) {
                        _uiState.value = IAPUIState.Loading(loaderType = IAPLoaderType.FULL_SCREEN)
                        createOrder(purchaseFlowData)
                    } else {
                        eventLogger.loadIAPScreenEvent()
                        loadPrice()
                    }
                }

                in listOf(IAPFlow.SILENT, IAPFlow.RESTORE) -> {
                    _uiState.value = IAPUIState.Loading(IAPLoaderType.FULL_SCREEN)
                    purchaseFlowData.flowStartTime = TimeUtils.getCurrentTime()
                    updateCourseData()
                }

                else -> {}
            }
        } else {
            _uiState.value = IAPUIState.None
        }
    }

    fun loadPrice() {
        viewModelScope.launch(Dispatchers.IO) {
            purchaseFlowData.takeIf { it.courseId != null && it.productInfo != null }
                ?.apply {
                    _uiState.value = IAPUIState.Loading(loaderType = IAPLoaderType.PRICE)
                    runCatching {
                        iapInteractor.loadPrice(purchaseFlowData.productInfo?.storeSku!!)
                    }.onSuccess {
                        this.formattedPrice = it.formattedPrice
                        this.price = it.getPriceAmount()
                        this.currencyCode = it.priceCurrencyCode
                        _uiState.value =
                            IAPUIState.ProductData(formattedPrice = it.formattedPrice)
                    }.onFailure {
                        logger.e(throwable = it)
                        updateErrorState(it, requestType = IAPRequestType.PRICE_CODE)
                    }
                } ?: run {
                updateErrorState(
                    IAPException(
                        requestType = IAPRequestType.PRICE_CODE,
                        httpErrorCode = IAPRequestType.PRICE_CODE.hashCode(),
                        errorMessage = "Product SKU is not provided in the request."
                    )
                )
            }
        }
    }

    fun startPurchaseFlow() {
        eventLogger.upgradeNowClickedEvent()
        _uiState.value = IAPUIState.Loading(loaderType = IAPLoaderType.PURCHASE_FLOW)
        purchaseFlowData.flowStartTime = TimeUtils.getCurrentTime()
        val courseName = purchaseFlowData.courseName
        val productInfo = purchaseFlowData.productInfo

        if (courseName == null || productInfo == null) {
            // Handle missing data error
            updateErrorState(
                IAPException(
                    requestType = IAPRequestType.NO_SKU_CODE,
                    httpErrorCode = IAPRequestType.NO_SKU_CODE.hashCode(),
                    errorMessage = ""
                )
            )
            return
        }
        checkingCourseMode = true
        updateCourseData()
    }

    fun purchaseItem(activity: FragmentActivity) {
        viewModelScope.launch(Dispatchers.IO) {
            takeIf {
                purchaseFlowData.productInfo != null
            }?.apply {
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
                updateCourseData()
            }.onFailure {
                logger.e(throwable = it)
                updateErrorState(it, requestType = IAPRequestType.CREATE_ORDER_CODE)
            }
        }
    }

    private fun consumeOrderForFurtherPurchases(purchaseFlowData: PurchaseFlowData) {
        if (purchaseFlowData.isConsumed) return
        viewModelScope.launch(Dispatchers.IO) {
            purchaseFlowData.run {
                // If purchaseToken is null or empty, try to fetch it from the user's purchases
                // after error dialogs, Unfulfilled/Restore flow, etc.
                runCatching {
                    if (purchaseToken.isNullOrEmpty()) {
                        iapInteractor.consumePurchaseByCourseId(purchaseFlowData.courseId!!)
                    } else {
                        iapInteractor.consumePurchaseByToken(purchaseToken!!)
                    }
                }.onSuccess {
                    if (eventLogger.isSilentIAPFlow.isNull()) {
                        eventLogger.upgradeSuccessEvent()
                    }
                    purchaseFlowData.isConsumed = true
                    // The IAP dialog will be dismissed by `CourseUnitContainerFragment` after
                    // refreshing the course components
                    if (eventLogger.purchaseFlowData?.screenName != IAPFlowSource.COURSE_COMPONENT.screen) {
                        _uiState.value = IAPUIState.CourseDataUpdated
                    } else {
                        iapNotifier.send(CourseDataUpdated.CourseUnitDataUpdate)
                    }
                    _uiMessage.emit(UIMessage.ToastMessage(resourceManager.getString(R.string.iap_success_message)))
                }.onFailure {
                    logger.e(throwable = it)
                    updateErrorState(it, requestType = IAPRequestType.CONSUME_CODE)
                }
            }
        }
    }

    private fun updateCourseData() {
        viewModelScope.launch(Dispatchers.IO) {
            purchaseFlowData.courseId?.let { courseId ->
                if (!checkingCourseMode) {
                    delay(purchaseData.courseModeTransitionRetryCount * AppDataConstants.ENROLLMENT_MODE_RETRY_BASE_DELAY_MS)
                    purchaseFlowData.courseModeTransitionRetryCount++
                }
                iapNotifier.send(UpdateCourseData(courseId = courseId, isFromValueProp = true))
            }
        }
    }

    private fun processCourseModeCheck(isVerifiedMode: Boolean) {
        if (checkingCourseMode) {
            checkingCourseMode = false
            if (!isVerifiedMode) {
                _uiState.value = IAPUIState.PurchaseProduct
            } else {
                updateErrorState(
                    IAPException(
                        requestType = IAPRequestType.PURCHASE_PRECHECK_CODE,
                        httpErrorCode = 409, // Purchase already completed; enforcing ACTION_REFRESH to update the state.
                        errorMessage = resourceManager.getString(R.string.iap_course_already_paid_for_message)
                    )
                )
            }
        } else {
            if (isVerifiedMode) {
                consumeOrderForFurtherPurchases(purchaseFlowData)
            } else {
                retryCourseModeTransition()
            }
        }
    }

    /**
     * Waits for the course mode transition to complete, retrying the update with delay.
     *
     * If the retry count exceeds the threshold (3), it triggers an error state indicating that
     * the course could not be fulfilled. Otherwise, it waits for a delay proportional to the
     * current retry count and then attempts to refresh the course data exponential backoff
     * instead of linear, and reset the count after trigger the error state.
     *
     * Delay = retryCount * 2500ms
     *
     * This method implements a simple retry-with-delay mechanism for handling
     * delayed course mode transitions (e.g., after an in-app purchase).
     */
    private fun retryCourseModeTransition() {
        if (purchaseData.courseModeTransitionRetryCount > AppDataConstants.ENROLLMENT_MODE_RETRY_THRESHOLD) {
            updateErrorState(
                IAPException(
                    requestType = IAPRequestType.COURSE_REFRESH_CODE,
                    httpErrorCode = 409, // Course not fulfilled; enforcing ACTION_REFRESH to update the state
                    errorMessage = resourceManager.getString(R.string.iap_course_not_fullfilled)
                )
            )
            purchaseFlowData.resetTransitionRetryCount()
        } else {
            updateCourseData()
        }
    }

    fun refreshCourse() {
        _uiState.value = IAPUIState.Loading(IAPLoaderType.FULL_SCREEN)
        purchaseFlowData.flowStartTime = TimeUtils.getCurrentTime()
        updateCourseData()
    }

    fun retryCreateOrder() {
        _uiState.value = IAPUIState.Loading(IAPLoaderType.FULL_SCREEN)
        createOrder(purchaseFlowData)
    }

    fun retryToConsumeOrder() {
        _uiState.value = IAPUIState.Loading(IAPLoaderType.FULL_SCREEN)
        consumeOrderForFurtherPurchases(purchaseFlowData)
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
            defaultMessage = resourceManager.getString(R.string.core_error_unknown_error),
        )
        eventLogger.logExceptionEvent(iapException)
        if (BillingClient.BillingResponseCode.USER_CANCELED != iapException.httpErrorCode) {
            _uiState.value = IAPUIState.Error(iapException)
        } else {
            _uiState.value = IAPUIState.Clear
        }
    }

    fun clearIAPFLow() {
        _uiState.value = IAPUIState.Clear
        purchaseFlowData.resetAll()
    }

    companion object {
        private const val TAG = "IAPViewModel"
    }
}
