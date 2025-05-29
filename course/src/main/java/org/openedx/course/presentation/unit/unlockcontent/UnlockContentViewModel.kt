package org.openedx.course.presentation.unit.unlockcontent

import androidx.fragment.app.FragmentActivity
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.core.domain.interactor.IAPInteractor
import org.openedx.core.domain.model.iap.IAPFlow
import org.openedx.core.domain.model.iap.IAPFlowSource
import org.openedx.core.domain.model.iap.PurchaseFlowData
import org.openedx.core.exception.iap.IAPException
import org.openedx.core.module.billing.BillingProcessor
import org.openedx.core.module.billing.getCourseSku
import org.openedx.core.module.billing.getPriceAmount
import org.openedx.core.presentation.IAPAnalytics
import org.openedx.core.presentation.iap.IAPAction
import org.openedx.core.presentation.iap.IAPEventLogger
import org.openedx.core.presentation.iap.IAPRequestType
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.notifier.CourseDataUpdated
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.IAPNotifier
import org.openedx.core.system.notifier.RefreshCourseComponents
import org.openedx.core.system.notifier.UpdateCourseData
import org.openedx.core.utils.TimeUtils
import org.openedx.course.domain.interactor.CourseInteractor

class UnlockContentViewModel(
    blockId: String,
    private val courseId: String,
    analytics: IAPAnalytics,
    private val courseInteractor: CourseInteractor,
    private val iapInteractor: IAPInteractor,
    private val iapNotifier: IAPNotifier,
    private val courseNotifier: CourseNotifier,
    private val resourceManager: ResourceManager,
) : BaseViewModel() {

    private val purchaseListeners = object : BillingProcessor.PurchaseListeners {
        override fun onPurchaseComplete(purchase: Purchase) {
            if (purchase.getCourseSku() == purchaseData.productInfo?.courseSku) {
                _uiState.value = UnlockContentUIState.FullScreenLoading
                purchaseData.purchaseToken = purchase.purchaseToken
                executeOrder(purchaseData)
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

    private val _uiState = MutableStateFlow<UnlockContentUIState>(UnlockContentUIState.Loading)
    val uiState: StateFlow<UnlockContentUIState>
        get() = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UnlockContentUIAction>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage: SharedFlow<UIMessage>
        get() = _uiMessage.asSharedFlow()

    private val purchaseData: PurchaseFlowData = PurchaseFlowData(
        iapFlow = IAPFlow.USER_INITIATED,
        screenName = IAPFlowSource.COURSE_COMPONENT.name,
        courseId = courseId,
        courseName = null,
        courseExpiresDate = null,
        isSelfPaced = null,
        componentId = blockId,
        productInfo = null,
    )

    private val eventLogger = IAPEventLogger(
        analytics = analytics,
        isSilentIAPFlow = purchaseData.isSilentIAPFlow(),
        purchaseFlowData = purchaseData
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            iapNotifier.notifier.onEach { event ->
                when (event) {
                    is CourseDataUpdated -> {
                        eventLogger.upgradeSuccessEvent()
                        _uiMessage.emit(UIMessage.ToastMessage(resourceManager.getString(R.string.iap_success_message)))
                        courseNotifier.send(RefreshCourseComponents)
                    }
                }
            }.distinctUntilChanged().launchIn(viewModelScope)
            fetchCourseData()
            loadPrice()
            eventLogger.loadIAPScreenEvent()
        }
    }

    private suspend fun fetchCourseData() {
        val courseEnrollmentDetails = courseInteractor.getEnrollmentDetails(courseId)
        purchaseData.apply {
            courseName = courseEnrollmentDetails.courseInfoOverview.name
            isSelfPaced = courseEnrollmentDetails.courseInfoOverview.isSelfPaced
            productInfo = courseEnrollmentDetails.courseInfoOverview.productInfo
        }
    }

    private suspend fun loadPrice() {
        purchaseData.takeIf { it.courseId != null && it.productInfo != null }
            ?.apply {
                _uiState.value = UnlockContentUIState.Loading
                runCatching {
                    iapInteractor.loadPrice(purchaseData.productInfo?.storeSku!!)
                }.onSuccess {
                    this.formattedPrice = it.formattedPrice
                    this.price = it.getPriceAmount()
                    this.currencyCode = it.priceCurrencyCode
                    _uiState.value =
                        UnlockContentUIState.ProductData(formattedPrice = it.formattedPrice)
                }.onFailure {
                    if (it is IAPException) {
                        updateErrorState(it)
                    }
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

    fun reloadPrice(iapException: IAPException) {
        viewModelScope.launch(Dispatchers.IO) {
            eventLogger.logIAPErrorActionEvent(
                iapException.requestType.request,
                IAPAction.ACTION_CLOSE.action
            )
            loadPrice()
        }
    }

    fun startPurchaseFlow() {
        eventLogger.upgradeNowClickedEvent()
        _uiState.value = UnlockContentUIState.Loading
        purchaseData.flowStartTime = TimeUtils.getCurrentTime()
        val productInfo = purchaseData.productInfo

        if (productInfo == null) {
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

        addToBasket(productInfo.courseSku)
    }

    private fun addToBasket(courseSku: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                iapInteractor.addToBasket(courseSku)
            }.onSuccess { basketId ->
                purchaseData.basketId = basketId
                _uiEvent.emit(UnlockContentUIAction.PurchaseProduct)
            }.onFailure {
                if (it is IAPException) {
                    updateErrorState(it)
                }
            }
        }
    }

    fun purchaseItem(activity: FragmentActivity) {
        viewModelScope.launch(Dispatchers.IO) {
            takeIf {
                purchaseData.productInfo != null
            }?.apply {
                iapInteractor.purchaseItem(
                    activity,
                    purchaseData.productInfo!!,
                    purchaseListeners
                )
            }
        }
    }

    private fun executeOrder(purchaseFlowData: PurchaseFlowData) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                iapInteractor.executeOrder(
                    basketId = purchaseFlowData.basketId,
                    purchaseToken = purchaseFlowData.purchaseToken!!,
                    price = purchaseFlowData.price,
                    currencyCode = purchaseFlowData.currencyCode,
                )
            }.onSuccess {
                consumeOrderForFurtherPurchases(purchaseFlowData)
            }.onFailure {
                if (it is IAPException) {
                    updateErrorState(it)
                }
            }
        }
    }

    private fun consumeOrderForFurtherPurchases(purchaseFlowData: PurchaseFlowData) {
        viewModelScope.launch(Dispatchers.IO) {
            purchaseFlowData.purchaseToken?.let {
                runCatching {
                    iapInteractor.consumePurchase(it)
                }.onSuccess {
                    updateCourseData()
                }.onFailure {
                    if (it is IAPException) {
                        updateErrorState(it)
                    }
                }
            }
        }
    }

    private fun updateCourseData() {
        viewModelScope.launch(Dispatchers.IO) {
            purchaseData.courseId?.let {
                iapNotifier.send(UpdateCourseData(false))
            }
        }
    }

    private fun updateErrorState(iapException: IAPException) {
        eventLogger.logExceptionEvent(iapException)
        viewModelScope.launch {
            if (BillingClient.BillingResponseCode.USER_CANCELED != iapException.httpErrorCode) {
                _uiEvent.emit(UnlockContentUIAction.Error(iapException))
            } else {
                _uiEvent.emit(UnlockContentUIAction.Clear)
            }
        }
    }

    fun clearIAPFLow() {
        viewModelScope.launch {
            loadPrice()
        }
    }
}
