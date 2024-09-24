package org.openedx.core.presentation.iap

import android.content.Context
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
import org.openedx.core.domain.model.iap.PurchaseFlowData
import org.openedx.core.exception.iap.IAPException
import org.openedx.core.module.billing.BillingProcessor
import org.openedx.core.module.billing.getCourseSku
import org.openedx.core.module.billing.getPriceAmount
import org.openedx.core.presentation.IAPAnalytics
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.notifier.CourseDataUpdated
import org.openedx.core.system.notifier.IAPNotifier
import org.openedx.core.system.notifier.UpdateCourseData
import org.openedx.core.utils.TimeUtils

class IAPViewModel(
    iapFlow: IAPFlow,
    private val purchaseFlowData: PurchaseFlowData,
    private val iapInteractor: IAPInteractor,
    private val analytics: IAPAnalytics,
    private val resourceManager: ResourceManager,
    private val iapNotifier: IAPNotifier,
) : BaseViewModel() {

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
        purchaseFlowData = purchaseData
    )

    private val purchaseListeners = object : BillingProcessor.PurchaseListeners {
        override fun onPurchaseComplete(purchase: Purchase) {
            if (purchase.getCourseSku() == purchaseFlowData.productInfo?.courseSku) {
                _uiState.value =
                    IAPUIState.Loading(loaderType = IAPLoaderType.FULL_SCREEN)
                purchaseFlowData.purchaseToken = purchase.purchaseToken
                executeOrder(purchaseFlowData)
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

    init {
        viewModelScope.launch(Dispatchers.IO) {
            iapNotifier.notifier.onEach { event ->
                when (event) {
                    is CourseDataUpdated -> {
                        eventLogger.upgradeSuccessEvent()
                        _uiMessage.emit(UIMessage.ToastMessage(resourceManager.getString(R.string.iap_success_message)))
                        _uiState.value = IAPUIState.CourseDataUpdated
                    }
                }
            }.distinctUntilChanged().launchIn(viewModelScope)
        }

        when (iapFlow) {
            IAPFlow.USER_INITIATED -> {
                eventLogger.loadIAPScreenEvent()
                loadPrice()
            }

            IAPFlow.SILENT, IAPFlow.RESTORE -> {
                _uiState.value = IAPUIState.Loading(IAPLoaderType.FULL_SCREEN)
                purchaseFlowData.flowStartTime = TimeUtils.getCurrentTime()
                updateCourseData()
            }
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

        addToBasket(productInfo.courseSku)
    }

    private fun addToBasket(courseSku: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                iapInteractor.addToBasket(courseSku)
            }.onSuccess { basketId ->
                purchaseFlowData.basketId = basketId
                _uiState.value = IAPUIState.PurchaseProduct
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
                purchaseFlowData.productInfo != null
            }?.apply {
                iapInteractor.purchaseItem(
                    activity,
                    purchaseFlowData.productInfo!!,
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

    fun refreshCourse() {
        _uiState.value = IAPUIState.Loading(IAPLoaderType.FULL_SCREEN)
        purchaseFlowData.flowStartTime = TimeUtils.getCurrentTime()
        updateCourseData()
    }

    fun retryExecuteOrder() {
        executeOrder(purchaseFlowData)
    }

    fun retryToConsumeOrder() {
        consumeOrderForFurtherPurchases(purchaseFlowData)
    }

    private fun updateCourseData() {
        viewModelScope.launch(Dispatchers.IO) {
            purchaseFlowData.courseId?.let { courseId ->
                iapNotifier.send(UpdateCourseData(courseId))
            }
        }
    }

    fun showFeedbackScreen(context: Context, flowType: String, message: String) {
        iapInteractor.showFeedbackScreen(context, message)
        eventLogger.logIAPErrorActionEvent(flowType, IAPAction.ACTION_GET_HELP.action)
    }

    private fun updateErrorState(iapException: IAPException) {
        eventLogger.logExceptionEvent(iapException)
        if (BillingClient.BillingResponseCode.USER_CANCELED != iapException.httpErrorCode) {
            _uiState.value = IAPUIState.Error(iapException)
        } else {
            _uiState.value = IAPUIState.Clear
        }
    }

    fun clearIAPFLow() {
        _uiState.value = IAPUIState.Clear
        purchaseFlowData.reset()
    }
}
