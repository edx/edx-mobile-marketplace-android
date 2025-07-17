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
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.core.domain.interactor.IAPInteractor
import org.openedx.core.domain.model.iap.IAPFlow
import org.openedx.core.domain.model.iap.IAPFlowSource
import org.openedx.core.domain.model.iap.PurchaseFlowData
import org.openedx.core.exception.iap.IAPException
import org.openedx.core.module.billing.BillingProcessor
import org.openedx.core.module.billing.getCourseId
import org.openedx.core.module.billing.getPriceAmount
import org.openedx.core.presentation.IAPAnalytics
import org.openedx.core.presentation.iap.IAPAction
import org.openedx.core.presentation.iap.IAPEventLogger
import org.openedx.core.presentation.iap.IAPRequestType
import org.openedx.core.system.ResourceManager
import org.openedx.core.utils.TimeUtils
import org.openedx.course.domain.interactor.CourseInteractor

class UnlockContentViewModel(
    val blockId: String,
    val courseId: String,
    private val courseInteractor: CourseInteractor,
    private val iapInteractor: IAPInteractor,
    private val resourceManager: ResourceManager,
    analytics: IAPAnalytics,
) : BaseViewModel() {

    private val purchaseListeners = object : BillingProcessor.PurchaseListeners {
        override fun onPurchaseComplete(purchase: Purchase) {
            viewModelScope.launch {
                if (purchase.getCourseId() == purchaseData.courseId) {
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
        iapFlow = IAPFlow.USER_INITIATED,
        screenName = IAPFlowSource.COURSE_COMPONENT.screen,
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
            if (iapInteractor.isUpgradeEnabled) {
                loadPrice()
            } else {
                _uiState.value = UnlockContentUIState.Empty
            }
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

    fun startPurchaseFlow(activity: FragmentActivity) {
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
        checkCourseMode(activity)
    }

    private fun checkCourseMode(activity: FragmentActivity) {
        viewModelScope.launch {
            val isAuditMode = try {
                courseInteractor.getEnrollmentDetails(courseId).enrollmentDetails.isAuditMode
            } catch (_: Exception) {
                true
            }

            if (isAuditMode) {
                purchaseItem(activity)
            } else {
                updateErrorState(
                    IAPException(
                        requestType = IAPRequestType.PURCHASE_PRECHECK_CODE,
                        httpErrorCode = 409, // Purchase already completed; enforcing ACTION_REFRESH to update the state.
                        errorMessage = resourceManager.getString(R.string.iap_course_already_paid_for_message)
                    )
                )
            }
        }
    }

    private fun purchaseItem(activity: FragmentActivity) {
        viewModelScope.launch(Dispatchers.IO) {
            takeIf {
                purchaseData.productInfo != null
            }?.apply {
                iapInteractor.purchaseItem(
                    activity,
                    courseId = courseId,
                    productInfo = purchaseData.productInfo!!,
                    purchaseListeners = purchaseListeners
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

    fun reloadPrice(iapException: IAPException) {
        viewModelScope.launch(Dispatchers.IO) {
            eventLogger.logIAPErrorActionEvent(
                alertType = iapException.requestType.request,
                action = IAPAction.ACTION_CLOSE.action
            )
            refreshIAPState()
        }
    }

    fun refreshIAPState() {
        viewModelScope.launch {
            _uiEvent.emit(UnlockContentUIAction.None)
            purchaseData.reset()
            loadPrice()
        }
    }

    fun showFeedbackScreen(context: Context, requestType: String, message: String) {
        iapInteractor.showFeedbackScreen(context, message)
        eventLogger.logIAPErrorActionEvent(requestType, IAPAction.ACTION_GET_HELP.action)
        refreshIAPState()
    }
}
