package org.openedx.courses.presentation

import android.annotation.SuppressLint
import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
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
import org.openedx.core.config.Config
import org.openedx.core.data.model.CourseEnrollments
import org.openedx.core.domain.interactor.IAPInteractor
import org.openedx.core.domain.model.EnrolledCourse
import org.openedx.core.domain.model.iap.IAPFlow
import org.openedx.core.domain.model.iap.IAPFlowSource
import org.openedx.core.exception.iap.IAPException
import org.openedx.core.extension.isInternetError
import org.openedx.core.presentation.IAPAnalytics
import org.openedx.core.presentation.dialog.IAPDialogFragment
import org.openedx.core.presentation.iap.IAPAction
import org.openedx.core.presentation.iap.IAPEventLogger
import org.openedx.core.presentation.iap.IAPRequestType
import org.openedx.core.presentation.iap.IAPUIState
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseDashboardUpdate
import org.openedx.core.system.notifier.CourseDataUpdated
import org.openedx.core.system.notifier.DiscoveryNotifier
import org.openedx.core.system.notifier.IAPNotifier
import org.openedx.core.system.notifier.NavigationToDiscovery
import org.openedx.core.system.notifier.PushEvent
import org.openedx.core.system.notifier.PushNotifier
import org.openedx.core.system.notifier.UpdateCourseData
import org.openedx.core.system.notifier.app.AppNotifier
import org.openedx.core.system.notifier.app.EnrolledCourseEvent
import org.openedx.core.system.notifier.app.RequestEnrolledCourseErrorEvent
import org.openedx.core.system.notifier.app.RequestEnrolledCourseEvent
import org.openedx.core.ui.WindowSize
import org.openedx.core.utils.FileUtil
import org.openedx.core.utils.Logger
import org.openedx.dashboard.domain.CourseStatusFilter
import org.openedx.dashboard.domain.interactor.DashboardInteractor
import org.openedx.dashboard.presentation.DashboardAnalytics
import org.openedx.dashboard.presentation.DashboardAnalyticsEvent
import org.openedx.dashboard.presentation.DashboardAnalyticsKey
import org.openedx.dashboard.presentation.DashboardRouter
import org.openedx.dashboard.presentation.PrimaryCourseCardAction

@SuppressLint("StaticFieldLeak")
class DashboardGalleryViewModel(
    private val context: Context,
    private val config: Config,
    private val interactor: DashboardInteractor,
    private val resourceManager: ResourceManager,
    private val discoveryNotifier: DiscoveryNotifier,
    private val networkConnection: NetworkConnection,
    private val fileUtil: FileUtil,
    private val dashboardRouter: DashboardRouter,
    private val iapNotifier: IAPNotifier,
    private val pushNotifier: PushNotifier,
    private val appNotifier: AppNotifier,
    private val iapInteractor: IAPInteractor,
    private val windowSize: WindowSize,
    private val analytics: DashboardAnalytics,
    iapAnalytics: IAPAnalytics,
) : BaseViewModel() {

    private val logger = Logger(TAG)

    val apiHostUrl get() = config.getApiHostURL()

    private val _uiState =
        MutableStateFlow<DashboardGalleryUIState>(DashboardGalleryUIState.Loading)
    val uiState: StateFlow<DashboardGalleryUIState>
        get() = _uiState.asStateFlow()

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage: SharedFlow<UIMessage?>
        get() = _uiMessage.asSharedFlow()

    private val _updating = MutableStateFlow(false)
    val updating: StateFlow<Boolean>
        get() = _updating.asStateFlow()

    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

    private val _iapUiState = MutableSharedFlow<IAPUIState?>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val iapUiState: SharedFlow<IAPUIState?>
        get() = _iapUiState.asSharedFlow()

    private val eventLogger = IAPEventLogger(analytics = iapAnalytics, isSilentIAPFlow = true)

    private var isLoading = false

    init {
        collectAppEvent()
        collectDiscoveryNotifier()
        collectIapNotifier()
        getCourses()
    }

    private fun collectAppEvent() {
        appNotifier.notifier
            .onEach {
                if (it is RequestEnrolledCourseEvent) {
                    runCatching {
                        interactor.getAllUserCourses(status = CourseStatusFilter.ALL).courses
                    }.onSuccess { enrolledCourses ->
                        appNotifier.send(EnrolledCourseEvent(enrolledCourses))
                    }.onFailure {
                        logger.d { "Error getting enrolled courses: $it" }
                        appNotifier.send(RequestEnrolledCourseErrorEvent)
                    }
                }
            }
            .distinctUntilChanged()
            .launchIn(viewModelScope)
    }

    fun getCourses(isIAPFlow: Boolean = false) {
        viewModelScope.launch {
            try {
                val cachedCourseEnrollments = fileUtil.getObjectFromFile<CourseEnrollments>()
                if (cachedCourseEnrollments == null) {
                    if (networkConnection.isOnline()) {
                        _uiState.value = DashboardGalleryUIState.Loading
                    } else {
                        _uiState.value = DashboardGalleryUIState.Empty
                    }
                } else {
                    _uiState.value =
                        DashboardGalleryUIState.Courses(cachedCourseEnrollments.mapToDomain())
                }
                if (networkConnection.isOnline()) {
                    isLoading = true
                    val pageSize = if (windowSize.isTablet) {
                        PAGE_SIZE_TABLET
                    } else {
                        PAGE_SIZE_PHONE
                    }
                    val response = interactor.getMainUserCourses(pageSize)
                    if (response.primary == null && response.enrollments.courses.isEmpty()) {
                        _uiState.value = DashboardGalleryUIState.Empty
                    } else {
                        _uiState.value = DashboardGalleryUIState.Courses(response)
                    }
                    if (isIAPFlow) {
                        iapNotifier.send(CourseDataUpdated())
                    }
                } else {
                    val courseEnrollments = fileUtil.getObjectFromFile<CourseEnrollments>()
                    if (courseEnrollments == null) {
                        _uiState.value = DashboardGalleryUIState.Empty
                    } else {
                        _uiState.value =
                            DashboardGalleryUIState.Courses(courseEnrollments.mapToDomain())
                    }
                }
            } catch (e: Exception) {
                logger.e(throwable = e, metadata = mapOf("isIAPFlow" to isIAPFlow))
                if (e.isInternetError()) {
                    _uiMessage.emit(UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection)))
                } else {
                    _uiMessage.emit(UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error)))
                }
            } finally {
                _updating.value = false
                isLoading = false
            }
        }
    }

    fun updateCourses(isUpdating: Boolean = true, isIAPFlow: Boolean = false) {
        if (isLoading) {
            return
        }
        _updating.value = isUpdating
        getCourses(isIAPFlow = isIAPFlow)
    }

    fun refreshPushBadgeCount() {
        viewModelScope.launch { pushNotifier.send(PushEvent.RefreshBadgeCount) }
    }

    fun navigateToDiscovery() {
        viewModelScope.launch { discoveryNotifier.send(NavigationToDiscovery()) }
    }

    fun navigateToAllEnrolledCourses(fragmentManager: FragmentManager, isCardClicked: Boolean) {
        dashboardRouter.navigateToAllEnrolledCourses(fragmentManager)
        val event = if (isCardClicked) {
            DashboardAnalyticsEvent.VIEW_ALL_CARD_CLICKED
        } else {
            DashboardAnalyticsEvent.VIEW_ALL_COURSES_CLICKED
        }
        logEvent(event)
    }

    fun navigateToCourseOutline(
        fragmentManager: FragmentManager,
        enrolledCourse: EnrolledCourse,
        openDates: Boolean = false,
        resumeBlockId: String = "",
        action: DashboardGalleryScreenAction,
    ) {
        dashboardRouter.navigateToCourseOutline(
            fm = fragmentManager,
            courseId = enrolledCourse.course.id,
            courseTitle = enrolledCourse.course.name,
            openTab = if (openDates) CourseTab.DATES.name else CourseTab.HOME.name,
            resumeBlockId = resumeBlockId
        )
        logDashboardGalleryActionEvent(
            courseId = enrolledCourse.course.id,
            resumeBlockId = resumeBlockId,
            action = action
        )
    }

    fun processIAPAction(
        fragmentManager: FragmentManager,
        action: IAPAction,
        course: EnrolledCourse?,
        iapException: IAPException?,
    ) {
        when (action) {
            IAPAction.ACTION_USER_INITIATED -> {
                if (course != null) {
                    IAPDialogFragment.newInstance(
                        iapFlow = IAPFlow.USER_INITIATED,
                        screenName = IAPFlowSource.COURSE_ENROLLMENT.screen,
                        courseId = course.course.id,
                        courseName = course.course.name,
                        isSelfPaced = course.course.isSelfPaced,
                        productInfo = course.productInfo
                    ).show(
                        fragmentManager,
                        IAPDialogFragment.TAG
                    )
                    logPrimaryCourseCardClicked(
                        courseId = course.course.id,
                        action = PrimaryCourseCardAction.UPGRADE_VALUE_PROP
                    )
                }
            }

            IAPAction.ACTION_COMPLETION -> {
                IAPDialogFragment.newInstance(
                    IAPFlow.SILENT,
                    IAPFlowSource.COURSE_ENROLLMENT.screen
                ).show(
                    fragmentManager,
                    IAPDialogFragment.TAG
                )
                clearIAPState()
            }

            IAPAction.ACTION_UNFULFILLED -> {
                detectUnfulfilledPurchase()
            }

            IAPAction.ACTION_CLOSE -> {
                clearIAPState()
            }

            IAPAction.ACTION_ERROR_CLOSE -> {
                eventLogger.logIAPCancelEvent()
                clearIAPState()
            }

            IAPAction.ACTION_GET_HELP -> {
                iapException?.getFormattedErrorMessage()?.let {
                    showFeedbackScreen(it)
                }
                clearIAPState()
            }

            else -> {
            }
        }
    }

    private fun collectDiscoveryNotifier() {
        viewModelScope.launch {
            discoveryNotifier.notifier.collect {
                if (it is CourseDashboardUpdate) {
                    updateCourses()
                }
            }
        }
    }

    private fun collectIapNotifier() {
        iapNotifier.notifier.onEach { event ->
            when (event) {
                is UpdateCourseData -> {
                    updateCourses(isIAPFlow = event.isPurchasedFromCourseDashboard.not())
                }
            }
        }.distinctUntilChanged().launchIn(viewModelScope)
    }

    private fun detectUnfulfilledPurchase() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val enrolledCourses =
                    interactor.getAllUserCourses(status = CourseStatusFilter.ALL).courses
                iapInteractor.detectUnfulfilledPurchase(
                    enrolledCourses = enrolledCourses,
                    verificationInitiated = { purchaseFlowData ->
                        eventLogger.apply {
                            this.purchaseFlowData = purchaseFlowData
                            this.logUnfulfilledPurchaseInitiatedEvent()
                        }
                    },
                    onSuccess = { purchaseFlowData ->
                        eventLogger.apply {
                            this.purchaseFlowData = purchaseFlowData
                            this.upgradeSuccessEvent()
                        }
                        _iapUiState.tryEmit(IAPUIState.PurchasesFulfillmentCompleted)
                    },
                    onFailure = {
                        logger.e(throwable = it)
                        _iapUiState.tryEmit(
                            IAPUIState.Error(
                                IAPException(
                                    IAPRequestType.UNFULFILLED_CODE,
                                    it.httpErrorCode,
                                    it.errorMessage
                                )
                            )
                        )
                    }
                )
            } catch (e: Exception) {
                logger.e(throwable = e)
            }
        }
    }

    private fun showFeedbackScreen(message: String) {
        iapInteractor.showFeedbackScreen(context, message)
        eventLogger.logGetHelpEvent()
    }

    private fun clearIAPState() {
        viewModelScope.launch {
            _iapUiState.emit(null)
        }
    }

    private fun logPrimaryCourseCardClicked(
        courseId: String,
        action: PrimaryCourseCardAction,
        resumeBlockId: String? = null
    ) {
        logEvent(
            event = DashboardAnalyticsEvent.PRIMARY_COURSE_CARD_CLICKED,
            params = buildMap {
                put(DashboardAnalyticsKey.COURSE_ID.key, courseId)
                put(DashboardAnalyticsKey.ACTION.key, action.action)
                resumeBlockId?.let { put(DashboardAnalyticsKey.BLOCK_ID.key, it) }
            }
        )
    }

    private fun logDashboardGalleryActionEvent(
        courseId: String,
        resumeBlockId: String,
        action: DashboardGalleryScreenAction,
    ) {
        when (action) {
            is DashboardGalleryScreenAction.OpenCourse -> {
                if (action.isPrimaryCourse) {
                    logPrimaryCourseCardClicked(
                        courseId = courseId,
                        action = getPrimaryCourseCardAction(action.source),
                    )
                } else {
                    logSecondaryCourseCardClicked(courseId)
                }
            }

            is DashboardGalleryScreenAction.OpenBlock -> {
                logPrimaryCourseCardClicked(
                    courseId = courseId,
                    action = getPrimaryCourseCardAction(action.source),
                    resumeBlockId = resumeBlockId
                )
            }

            is DashboardGalleryScreenAction.NavigateToDates -> {
                if (action.source == ActionSource.PAST_ASSIGNMENT) {
                    logPrimaryCourseCardClicked(
                        courseId = courseId,
                        action = PrimaryCourseCardAction.PAST_ASSIGNMENT
                    )
                } else {
                    logPrimaryCourseCardClicked(
                        courseId = courseId,
                        action = PrimaryCourseCardAction.UPCOMING_ASSIGNMENT
                    )
                }
            }

            else -> {}
        }
    }

    private fun getPrimaryCourseCardAction(action: ActionSource): PrimaryCourseCardAction {
        return when (action) {
            ActionSource.PAST_ASSIGNMENT -> PrimaryCourseCardAction.PAST_ASSIGNMENT
            ActionSource.UPCOMING_ASSIGNMENT -> PrimaryCourseCardAction.UPCOMING_ASSIGNMENT
            ActionSource.RESUME_BLOCK -> PrimaryCourseCardAction.RESUME_COURSE
            ActionSource.START_COURSE -> PrimaryCourseCardAction.START_COURSE
            ActionSource.CARD -> PrimaryCourseCardAction.CARD
        }
    }

    private fun logSecondaryCourseCardClicked(courseId: String) {
        logEvent(
            event = DashboardAnalyticsEvent.SECONDARY_COURSE_CARD_CLICKED,
            params = buildMap {
                put(DashboardAnalyticsKey.COURSE_ID.key, courseId)
            }
        )
    }

    private fun logEvent(
        event: DashboardAnalyticsEvent,
        params: Map<String, Any?> = mutableMapOf(),
    ) {
        analytics.logEvent(
            event = event.eventName,
            params = buildMap {
                put(DashboardAnalyticsKey.NAME.key, event.biValue)
                put(DashboardAnalyticsKey.CATEGORY.key, DashboardAnalyticsKey.LEARN.key)
                putAll(params)
            }
        )
    }

    companion object {
        private const val TAG = "DashboardGalleryViewModel"
        private const val PAGE_SIZE_TABLET = 7
        private const val PAGE_SIZE_PHONE = 5
    }
}
