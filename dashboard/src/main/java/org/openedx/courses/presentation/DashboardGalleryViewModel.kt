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
import org.openedx.core.ui.WindowSize
import org.openedx.core.utils.FileUtil
import org.openedx.dashboard.domain.CourseStatusFilter
import org.openedx.dashboard.domain.interactor.DashboardInteractor
import org.openedx.dashboard.presentation.DashboardRouter

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
    private val iapInteractor: IAPInteractor,
    private val windowSize: WindowSize,
    iapAnalytics: IAPAnalytics,
) : BaseViewModel() {

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
        collectDiscoveryNotifier()
        collectIapNotifier()
        getCourses()
    }

    fun getCourses(isIAPFlow: Boolean = false) {
        viewModelScope.launch {
            try {
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

    fun navigateToAllEnrolledCourses(fragmentManager: FragmentManager) {
        dashboardRouter.navigateToAllEnrolledCourses(fragmentManager)
    }

    fun navigateToCourseOutline(
        fragmentManager: FragmentManager,
        enrolledCourse: EnrolledCourse,
        openDates: Boolean = false,
        resumeBlockId: String = "",
    ) {
        dashboardRouter.navigateToCourseOutline(
            fm = fragmentManager,
            courseId = enrolledCourse.course.id,
            courseTitle = enrolledCourse.course.name,
            openTab = if (openDates) CourseTab.DATES.name else CourseTab.HOME.name,
            resumeBlockId = resumeBlockId
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
            val enrolledCourses =
                interactor.getAllUserCourses(status = CourseStatusFilter.ALL).courses
            iapInteractor.detectUnfulfilledPurchase(
                enrolledCourses = enrolledCourses,
                purchaseVerified = { purchaseFlowData ->
                    eventLogger.apply {
                        this.purchaseFlowData = purchaseFlowData
                        this.logUnfulfilledPurchaseInitiatedEvent()
                    }
                },
                onSuccess = {
                    _iapUiState.tryEmit(IAPUIState.PurchasesFulfillmentCompleted)
                },
                onFailure = {
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

    companion object {
        private const val PAGE_SIZE_TABLET = 7
        private const val PAGE_SIZE_PHONE = 5
    }
}
