package org.openedx.course.presentation.unit.unlockcontent

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
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
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
import org.openedx.core.utils.TimeUtils
import org.openedx.course.domain.interactor.CourseInteractor

class UnlockContentViewModel(
    val blockId: String,
    val courseId: String,
    private val courseInteractor: CourseInteractor,
    private val iapInteractor: IAPInteractor,
    analytics: IAPAnalytics,
) : BaseViewModel() {

    private val purchaseListeners = object : BillingProcessor.PurchaseListeners {
        override fun onPurchaseComplete(purchase: Purchase) {
            viewModelScope.launch {
                if (purchase.getCourseSku() == purchaseData.productInfo?.courseSku) {
                    purchaseData.purchaseToken = purchase.purchaseToken
                    // execute order, consume order and course data update will performed behind the fullscreen loader
                    _uiEvent.emit(UnlockContentUIAction.FullScreenLoader(purchaseData))
                }
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
        iapFlow = IAPFlow.UNLOCK_COMPONENT_USER_INITIATED,
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
        isSilentIAPFlow = false,
        purchaseFlowData = purchaseData
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
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
            refreshIAPState()
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

    fun showFeedbackScreen(context: Context, requestType: String, message: String) {
        iapInteractor.showFeedbackScreen(context, message)
        eventLogger.logIAPErrorActionEvent(requestType, IAPAction.ACTION_GET_HELP.action)
        refreshIAPState()
    }

    fun refreshIAPState() {
        viewModelScope.launch {
            _uiEvent.emit(UnlockContentUIAction.None)
            purchaseData.reset()
            loadPrice()
        }
    }
}
