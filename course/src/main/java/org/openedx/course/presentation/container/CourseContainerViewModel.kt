package org.openedx.course.presentation.container

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.ImageProcessor
import org.openedx.core.SingleEventLiveData
import org.openedx.core.UIMessage
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.interactor.IAPInteractor
import org.openedx.core.domain.model.CourseAccessError
import org.openedx.core.domain.model.CourseEnrollmentDetails
import org.openedx.core.domain.model.iap.IAPFlow
import org.openedx.core.domain.model.iap.IAPFlowSource
import org.openedx.core.domain.model.iap.PurchaseFlowData
import org.openedx.core.exception.iap.IAPException
import org.openedx.core.extension.isFalse
import org.openedx.core.extension.isNotNull
import org.openedx.core.extension.isTrue
import org.openedx.core.module.billing.BillingProcessor
import org.openedx.core.module.billing.getCourseSku
import org.openedx.core.module.billing.getPriceAmount
import org.openedx.core.presentation.IAPAnalytics
import org.openedx.core.presentation.iap.IAPAction
import org.openedx.core.presentation.iap.IAPEventLogger
import org.openedx.core.presentation.iap.IAPLoaderType
import org.openedx.core.presentation.iap.IAPRequestType
import org.openedx.core.presentation.iap.IAPUIState
import org.openedx.core.presentation.settings.calendarsync.CalendarSyncDialogType
import org.openedx.core.presentation.settings.calendarsync.CalendarSyncUIState
import org.openedx.core.system.CalendarManager
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CalendarSyncEvent.CheckCalendarSyncEvent
import org.openedx.core.system.notifier.CalendarSyncEvent.CreateCalendarSyncEvent
import org.openedx.core.system.notifier.CourseCompletionSet
import org.openedx.core.system.notifier.CourseDataUpdated
import org.openedx.core.system.notifier.CourseDatesShifted
import org.openedx.core.system.notifier.CourseLoading
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseOpenBlock
import org.openedx.core.system.notifier.CourseStructureUpdated
import org.openedx.core.system.notifier.IAPNotifier
import org.openedx.core.system.notifier.RefreshDates
import org.openedx.core.system.notifier.RefreshDiscussions
import org.openedx.core.system.notifier.UpdateCourseData
import org.openedx.core.utils.TimeUtils
import org.openedx.course.DatesShiftedSnackBar
import org.openedx.course.data.storage.CoursePreferences
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CalendarSyncDialog
import org.openedx.course.presentation.CalendarSyncSnackbar
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.CourseAnalyticsEvent
import org.openedx.course.presentation.CourseAnalyticsKey
import org.openedx.course.presentation.CourseRouter
import java.util.concurrent.atomic.AtomicReference
import org.openedx.core.R as CoreR

class CourseContainerViewModel(
    val courseId: String,
    var courseName: String,
    private var resumeBlockId: String,
    private val config: Config,
    private val interactor: CourseInteractor,
    private val calendarManager: CalendarManager,
    private val resourceManager: ResourceManager,
    private val courseNotifier: CourseNotifier,
    private val iapNotifier: IAPNotifier,
    private val iapInteractor: IAPInteractor,
    private val networkConnection: NetworkConnection,
    private val corePreferences: CorePreferences,
    private val coursePreferences: CoursePreferences,
    private val courseAnalytics: CourseAnalytics,
    private val iapAnalytics: IAPAnalytics,
    private val imageProcessor: ImageProcessor,
    val courseRouter: CourseRouter,
) : BaseViewModel() {

    private val _dataReady = MutableLiveData<Boolean?>()
    val dataReady: LiveData<Boolean?>
        get() = _dataReady

    private val _courseAccessStatus = MutableLiveData<CourseAccessError>()
    val courseAccessStatus: LiveData<CourseAccessError>
        get() = _courseAccessStatus

    private val _errorMessage = SingleEventLiveData<String>()
    val errorMessage: LiveData<String>
        get() = _errorMessage

    private val _iapState = MutableStateFlow<IAPUIState>(IAPUIState.Clear)
    val iapState: StateFlow<IAPUIState> = _iapState.asStateFlow()

    private val _showProgress = MutableStateFlow(true)
    val showProgress: StateFlow<Boolean> =
        _showProgress.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> =
        _refreshing.asStateFlow()

    private val _isNavigationEnabled = MutableStateFlow(false)
    val isNavigationEnabled: StateFlow<Boolean> =
        _isNavigationEnabled.asStateFlow()

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage: SharedFlow<UIMessage>
        get() = _uiMessage.asSharedFlow()

    val purchaseFlowData: PurchaseFlowData = PurchaseFlowData()

    val eventLogger = IAPEventLogger(analytics = iapAnalytics, purchaseFlowData = purchaseFlowData)

    private var _canShowUpgradeButton = MutableStateFlow(false)
    val canShowUpgradeButton: StateFlow<Boolean>
        get() = _canShowUpgradeButton.asStateFlow()

    private var _courseDetails: CourseEnrollmentDetails? = null
    val courseDetails: CourseEnrollmentDetails?
        get() = _courseDetails

    val calendarPermissions: Array<String>
        get() = calendarManager.permissions

    private val _calendarSyncUIState = MutableStateFlow(
        CalendarSyncUIState(
            isCalendarSyncEnabled = false,
            calendarTitle = calendarManager.getCourseCalendarTitle(courseName),
            courseDates = emptyList(),
            dialogType = CalendarSyncDialogType.NONE,
            checkForOutOfSync = AtomicReference(false),
            uiMessage = AtomicReference(""),
        )
    )
    val calendarSyncUIState: StateFlow<CalendarSyncUIState> =
        _calendarSyncUIState.asStateFlow()

    private var _courseImage = MutableStateFlow(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
    val courseImage: StateFlow<Bitmap> = _courseImage.asStateFlow()

    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

    private val purchaseListeners = object : BillingProcessor.PurchaseListeners {
        override fun onPurchaseComplete(purchase: Purchase) {
            if (purchase.getCourseSku() == purchaseFlowData.productInfo?.courseSku) {
                _iapState.value =
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
        viewModelScope.launch {
            courseNotifier.notifier.collect { event ->
                when (event) {
                    is CourseCompletionSet -> {
                        updateData()
                    }

                    is CreateCalendarSyncEvent -> {
                        // Skip out-of-sync check if any calendar dialog is visible
                        if (event.checkOutOfSync && _calendarSyncUIState.value.isDialogVisible) {
                            return@collect
                        }
                        _calendarSyncUIState.update {
                            val dialogType = CalendarSyncDialogType.valueOf(event.dialogType)
                            it.copy(
                                courseDates = event.courseDates,
                                dialogType = dialogType,
                                checkForOutOfSync = AtomicReference(event.checkOutOfSync)
                            )
                        }
                    }

                    is CourseDatesShifted -> {
                        _uiMessage.emit(DatesShiftedSnackBar())
                    }

                    is CourseLoading -> {
                        _showProgress.value = event.isLoading
                        if (!event.isLoading) {
                            _refreshing.value = false
                        }
                    }
                }
            }
        }

        iapNotifier.notifier.onEach { event ->
            when (event) {
                is UpdateCourseData -> {
                    fetchCourseDetails(
                        isIAPFlow = event.isPurchasedFromCourseDashboard,
                        isExpiredCoursePurchase = event.isExpiredCoursePurchase
                    )
                }
            }
        }.distinctUntilChanged().launchIn(viewModelScope)
    }

    fun fetchCourseDetails(isIAPFlow: Boolean = false, isExpiredCoursePurchase: Boolean = false) {
        if (isIAPFlow.not()) {
            courseDashboardViewed()
        }
        _showProgress.value = true
        viewModelScope.launch {
            try {
                _courseDetails = interactor.getEnrollmentDetails(courseId)
                _courseDetails?.let { courseDetails ->
                    val courseInfoOverview = courseDetails.courseInfoOverview
                    courseName = courseInfoOverview.name
                    _canShowUpgradeButton.value =
                        iapInteractor.isIAPEnabled && courseDetails.isUpgradeable
                    loadCourseImage(courseInfoOverview.media?.image?.large)
                    _showProgress.value = false
                    if (courseDetails.hasAccess.isFalse()) {
                        _dataReady.value = false
                        if (courseDetails.isAuditAccessExpired) {
                            if (_canShowUpgradeButton.value && courseInfoOverview.productInfo.isNotNull()) {
                                purchaseFlowData.apply {
                                    courseId = courseDetails.id
                                    courseName = courseInfoOverview.name
                                    isSelfPaced = courseInfoOverview.isSelfPaced
                                    productInfo = courseInfoOverview.productInfo
                                    screenName = IAPFlowSource.COURSE_DASHBOARD.screen
                                    iapFlow = IAPFlow.USER_INITIATED
                                }
                                loadPrice()
                                _courseAccessStatus.value =
                                    CourseAccessError.AUDIT_EXPIRED_UPGRADABLE
                            } else {
                                _courseAccessStatus.value =
                                    CourseAccessError.AUDIT_EXPIRED_NOT_UPGRADABLE
                            }
                        } else if (courseDetails.courseInfoOverview.isStarted.not()) {
                            _courseAccessStatus.value = CourseAccessError.NOT_YET_STARTED
                        } else {
                            _courseAccessStatus.value = CourseAccessError.UNKNOWN
                        }
                    } else {
                        _courseAccessStatus.value = CourseAccessError.NONE
                        _isNavigationEnabled.value = true
                        _calendarSyncUIState.update { state ->
                            state.copy(isCalendarSyncEnabled = isCalendarSyncEnabled())
                        }
                        if (resumeBlockId.isNotEmpty()) {
                            delay(500L)
                            courseNotifier.send(CourseOpenBlock(resumeBlockId))
                        }
                        if (isIAPFlow) {
                            if (isExpiredCoursePurchase) {
                                eventLogger.upgradeSuccessEvent()
                                _uiMessage.emit(
                                    UIMessage.ToastMessage(
                                        resourceManager.getString(CoreR.string.iap_success_message)
                                    )
                                )
                            } else {
                                iapNotifier.send(CourseDataUpdated())
                            }
                            _iapState.value = IAPUIState.CourseDataUpdated
                        }
                        _dataReady.value = true
                    }
                } ?: run {
                    _courseAccessStatus.value = CourseAccessError.UNKNOWN
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _courseAccessStatus.value = CourseAccessError.UNKNOWN
                _showProgress.value = false
            }
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
                        _iapState.value =
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
                        errorMessage = ""
                    )
                )
            }
        }
    }

    private fun loadCourseImage(imageUrl: String?) {
        imageProcessor.loadImage(
            imageUrl = config.getApiHostURL() + imageUrl,
            defaultImage = CoreR.drawable.core_no_image_course,
            onComplete = { drawable ->
                val bitmap = (drawable as BitmapDrawable).bitmap.apply {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                        imageProcessor.applyBlur(this@apply, 10f)
                    }
                }
                viewModelScope.launch {
                    _courseImage.emit(bitmap)
                }
            }
        )
    }

    fun startPurchaseFlow() {
        eventLogger.upgradeNowClickedEvent()
        _iapState.value = IAPUIState.Loading(loaderType = IAPLoaderType.PURCHASE_FLOW)
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

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                iapInteractor.addToBasket(productInfo.courseSku)
            }.onSuccess { basketId ->
                purchaseFlowData.basketId = basketId
                _iapState.value = IAPUIState.PurchaseProduct
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
        _iapState.value = IAPUIState.Loading(IAPLoaderType.FULL_SCREEN)
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
            purchaseFlowData.courseId?.let {
                iapNotifier.send(
                    UpdateCourseData(
                        isPurchasedFromCourseDashboard = true,
                        isExpiredCoursePurchase = true
                    )
                )
            }
        }
    }

    fun showFeedbackScreen(context: Context, flowType: String, message: String) {
        iapInteractor.showFeedbackScreen(context, message)
        eventLogger.logIAPErrorActionEvent(flowType, IAPAction.ACTION_GET_HELP.action)
    }

    fun onRefresh(courseContainerTab: CourseContainerTab) {
        _refreshing.value = true
        when (courseContainerTab) {
            CourseContainerTab.HOME -> {
                updateData()
            }

            CourseContainerTab.VIDEOS -> {
                updateData()
            }

            CourseContainerTab.DATES -> {
                viewModelScope.launch {
                    courseNotifier.send(RefreshDates)
                }
            }

            CourseContainerTab.DISCUSSIONS -> {
                viewModelScope.launch {
                    courseNotifier.send(RefreshDiscussions)
                }
            }

            else -> {
                _refreshing.value = false
            }
        }
    }

    fun updateData() {
        viewModelScope.launch {
            try {
                interactor.getCourseStructure(courseId, isNeedRefresh = true)
            } catch (e: Exception) {
                _errorMessage.value =
                    resourceManager.getString(CoreR.string.core_error_unknown_error)
            }
            _refreshing.value = false
            courseNotifier.send(CourseStructureUpdated(courseId))
        }
    }

    private fun updateErrorState(iapException: IAPException) {
        eventLogger.logExceptionEvent(iapException)
        if (BillingClient.BillingResponseCode.USER_CANCELED != iapException.httpErrorCode) {
            _iapState.value = IAPUIState.Error(iapException)
        } else {
            clearIAPState()
        }
    }

    fun isFullScreenLoading(): Boolean {
        return _iapState.value is IAPUIState.Loading &&
                (_iapState.value as IAPUIState.Loading).loaderType == IAPLoaderType.FULL_SCREEN
    }

    fun clearIAPState() {
        _iapState.value = IAPUIState.Clear
        purchaseFlowData.formattedPrice?.let {
            _iapState.value = IAPUIState.ProductData(it)
        }
    }

    fun courseContainerTabClickedEvent(index: Int) {
        when (CourseContainerTab.entries[index]) {
            CourseContainerTab.HOME -> courseTabClickedEvent()
            CourseContainerTab.VIDEOS -> videoTabClickedEvent()
            CourseContainerTab.DISCUSSIONS -> discussionTabClickedEvent()
            CourseContainerTab.DATES -> datesTabClickedEvent()
            CourseContainerTab.MORE -> moreTabClickedEvent()
        }
    }

    fun setCalendarSyncDialogType(dialogType: CalendarSyncDialogType) {
        val currentState = _calendarSyncUIState.value
        if (currentState.dialogType != dialogType) {
            _calendarSyncUIState.value = currentState.copy(dialogType = dialogType)
        }
    }

    fun addOrUpdateEventsInCalendar(
        updatedEvent: Boolean,
    ) {
        setCalendarSyncDialogType(CalendarSyncDialogType.LOADING_DIALOG)

        val startSyncTime = TimeUtils.getCurrentTime()
        val calendarId = getCalendarId()

        if (calendarId == CalendarManager.CALENDAR_DOES_NOT_EXIST) {
            setUiMessage(CoreR.string.core_snackbar_course_calendar_error)
            setCalendarSyncDialogType(CalendarSyncDialogType.NONE)

            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val courseDates = _calendarSyncUIState.value.courseDates
            if (courseDates.isNotEmpty()) {
                courseDates.forEach { courseDateBlock ->
                    calendarManager.addEventsIntoCalendar(
                        calendarId = calendarId,
                        courseId = courseId,
                        courseName = courseName,
                        courseDateBlock = courseDateBlock
                    )
                }
            }
            val elapsedSyncTime = TimeUtils.getCurrentTime() - startSyncTime
            val delayRemaining = maxOf(0, 1000 - elapsedSyncTime)

            // Ensure minimum 1s delay to prevent flicker for rapid event creation
            if (delayRemaining > 0) {
                delay(delayRemaining)
            }

            setCalendarSyncDialogType(CalendarSyncDialogType.NONE)
            updateCalendarSyncState()

            if (updatedEvent) {
                logCalendarSyncSnackbar(CalendarSyncSnackbar.UPDATED)
                setUiMessage(CoreR.string.core_snackbar_course_calendar_updated)
            } else if (coursePreferences.isCalendarSyncEventsDialogShown(courseName)) {
                logCalendarSyncSnackbar(CalendarSyncSnackbar.ADDED)
                setUiMessage(CoreR.string.core_snackbar_course_calendar_added)
            } else {
                coursePreferences.setCalendarSyncEventsDialogShown(courseName)
                setCalendarSyncDialogType(CalendarSyncDialogType.EVENTS_DIALOG)
            }
        }
    }

    private fun updateCalendarSyncState() {
        viewModelScope.launch {
            val isCalendarSynced = calendarManager.isCalendarExists(
                calendarTitle = _calendarSyncUIState.value.calendarTitle
            )
            courseNotifier.send(CheckCalendarSyncEvent(isSynced = isCalendarSynced))
        }
    }

    fun checkIfCalendarOutOfDate() {
        val courseDates = _calendarSyncUIState.value.courseDates
        if (courseDates.isNotEmpty()) {
            _calendarSyncUIState.value.checkForOutOfSync.set(false)
            val outdatedCalendarId = calendarManager.isCalendarOutOfDate(
                calendarTitle = _calendarSyncUIState.value.calendarTitle,
                courseDateBlocks = courseDates
            )
            if (outdatedCalendarId != CalendarManager.CALENDAR_DOES_NOT_EXIST) {
                setCalendarSyncDialogType(CalendarSyncDialogType.OUT_OF_SYNC_DIALOG)
            }
        }
    }

    fun deleteCourseCalendar() {
        if (calendarManager.hasPermissions()) {
            viewModelScope.launch(Dispatchers.IO) {
                val calendarId = getCalendarId()
                if (calendarId != CalendarManager.CALENDAR_DOES_NOT_EXIST) {
                    calendarManager.deleteCalendar(
                        calendarId = calendarId,
                    )
                }
                updateCalendarSyncState()

            }
            logCalendarSyncSnackbar(CalendarSyncSnackbar.REMOVED)
            setUiMessage(CoreR.string.core_snackbar_course_calendar_removed)
        }
    }

    fun openCalendarApp() {
        calendarManager.openCalendarApp()
    }

    private fun setUiMessage(@StringRes stringResId: Int) {
        _calendarSyncUIState.update {
            it.copy(uiMessage = AtomicReference(resourceManager.getString(stringResId)))
        }
    }

    private fun getCalendarId(): Long {
        return calendarManager.createOrUpdateCalendar(
            calendarTitle = _calendarSyncUIState.value.calendarTitle
        )
    }

    private fun isCalendarSyncEnabled(): Boolean {
        val calendarSync = corePreferences.appConfig.courseDatesCalendarSync
        return calendarSync.isEnabled && ((calendarSync.isSelfPacedEnabled && _courseDetails?.courseInfoOverview?.isSelfPaced.isTrue()) ||
                (calendarSync.isInstructorPacedEnabled && _courseDetails?.courseInfoOverview?.isSelfPaced.isFalse()))
    }

    private fun courseDashboardViewed() {
        logCourseContainerEvent(CourseAnalyticsEvent.DASHBOARD)
        courseTabClickedEvent()
    }

    private fun courseTabClickedEvent() {
        logCourseContainerEvent(CourseAnalyticsEvent.HOME_TAB)
    }

    private fun videoTabClickedEvent() {
        logCourseContainerEvent(CourseAnalyticsEvent.VIDEOS_TAB)
    }

    private fun discussionTabClickedEvent() {
        logCourseContainerEvent(CourseAnalyticsEvent.DISCUSSION_TAB)
    }

    private fun datesTabClickedEvent() {
        logCourseContainerEvent(CourseAnalyticsEvent.DATES_TAB)
    }

    private fun moreTabClickedEvent() {
        logCourseContainerEvent(CourseAnalyticsEvent.MORE_TAB)
    }

    private fun logCourseContainerEvent(event: CourseAnalyticsEvent) {
        courseAnalytics.logScreenEvent(
            screenName = event.eventName,
            params = buildMap {
                put(CourseAnalyticsKey.NAME.key, event.biValue)
                put(CourseAnalyticsKey.COURSE_ID.key, courseId)
                put(CourseAnalyticsKey.COURSE_NAME.key, courseName)
            }
        )
    }

    fun logCalendarPermissionAccess(isAllowed: Boolean) {
        logCalendarSyncEvent(
            CourseAnalyticsEvent.DATES_CALENDAR_SYNC_DIALOG_ACTION,
            CalendarSyncDialog.PERMISSION.getBuildMap(isAllowed)
        )
    }

    fun logCalendarAddDates(action: Boolean) {
        logCalendarSyncEvent(
            CourseAnalyticsEvent.DATES_CALENDAR_SYNC_DIALOG_ACTION,
            CalendarSyncDialog.ADD.getBuildMap(action)
        )
    }

    fun logCalendarRemoveDates(action: Boolean) {
        logCalendarSyncEvent(
            CourseAnalyticsEvent.DATES_CALENDAR_SYNC_DIALOG_ACTION,
            CalendarSyncDialog.REMOVE.getBuildMap(action)
        )
    }

    fun logCalendarSyncedConfirmation(action: Boolean) {
        logCalendarSyncEvent(
            CourseAnalyticsEvent.DATES_CALENDAR_SYNC_DIALOG_ACTION,
            CalendarSyncDialog.CONFIRMED.getBuildMap(action)
        )
    }

    fun logCalendarSyncUpdate(action: Boolean) {
        logCalendarSyncEvent(
            CourseAnalyticsEvent.DATES_CALENDAR_SYNC_DIALOG_ACTION,
            CalendarSyncDialog.UPDATE.getBuildMap(action)
        )
    }

    private fun logCalendarSyncSnackbar(snackbar: CalendarSyncSnackbar) {
        logCalendarSyncEvent(
            CourseAnalyticsEvent.DATES_CALENDAR_SYNC_SNACKBAR,
            snackbar.getBuildMap()
        )
    }

    private fun logCalendarSyncEvent(
        event: CourseAnalyticsEvent,
        param: Map<String, Any> = emptyMap(),
    ) {
        courseAnalytics.logEvent(
            event = event.eventName,
            params = buildMap {
                put(CourseAnalyticsKey.NAME.key, event.biValue)
                put(CourseAnalyticsKey.COURSE_ID.key, courseId)
                put(
                    CourseAnalyticsKey.ENROLLMENT_MODE.key,
                    _courseDetails?.enrollmentDetails?.mode ?: ""
                )
                put(
                    CourseAnalyticsKey.PACING.key,
                    if (_courseDetails?.courseInfoOverview?.isSelfPaced.isTrue()) CourseAnalyticsKey.SELF_PACED.key
                    else CourseAnalyticsKey.INSTRUCTOR_PACED.key
                )
                putAll(param)
            }
        )
    }
}
