package org.openedx.course.presentation.container

import android.graphics.Bitmap
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.base.Verify.verify
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.spyk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.openedx.core.CoreMocks
import org.openedx.core.R
import org.openedx.core.config.Config
import org.openedx.core.data.api.CourseApi
import org.openedx.core.data.model.User
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.interactor.IAPInteractor
import org.openedx.core.domain.model.AppConfig
import org.openedx.core.domain.model.CourseAccessDetails
import org.openedx.core.domain.model.CourseDatesCalendarSync
import org.openedx.core.domain.model.CourseEnrollmentDetails
import org.openedx.core.domain.model.CourseEnrollmentDetailsSource
import org.openedx.core.domain.model.CourseInfoOverview
import org.openedx.core.domain.model.CourseSharingUtmParameters
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.domain.model.CoursewareAccess
import org.openedx.core.domain.model.EnrollmentDetails
import org.openedx.core.presentation.IAPAnalytics
import org.openedx.core.domain.model.CourseAccessError
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseStructureUpdated
import org.openedx.core.worker.CalendarSyncScheduler
import org.openedx.core.system.notifier.IAPNotifier
import org.openedx.core.utils.Logger
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.CourseAnalyticsEvent
import org.openedx.course.presentation.CourseRouter
import org.openedx.course.utils.ImageProcessor
import org.openedx.foundation.system.ResourceManager
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class CourseContainerViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    private val resourceManager = mockk<ResourceManager>()
    private val config = mockk<Config>()
    private val interactor = mockk<CourseInteractor>()
    private val networkConnection = mockk<NetworkConnection>()
    private val iapNotifier = spyk<IAPNotifier>()
    private val iapInteractor = mockk<IAPInteractor>()
    private val courseAnalytics = mockk<CourseAnalytics>()
    private val iapAnalytics = mockk<IAPAnalytics>()
    private val courseNotifier = spyk<CourseNotifier>()
    private val analytics = mockk<CourseAnalytics>()
    private val corePreferences = mockk<CorePreferences>()
    private val mockBitmap = mockk<Bitmap>()
    private val imageProcessor = mockk<ImageProcessor>()
    private val courseRouter = mockk<CourseRouter>()
    private val courseApi = mockk<CourseApi>()
    private val calendarSyncScheduler = mockk<CalendarSyncScheduler>()

    private val openEdx = "OpenEdx"
    private val noInternet = "Slow or no internet connection"
    private val somethingWrong = "Something went wrong"

    private val user = User(
        id = 0,
        username = "",
        email = "",
        name = "",
    )
    private val appConfig = AppConfig(
        CourseDatesCalendarSync(
            isEnabled = true,
            isSelfPacedEnabled = true,
            isInstructorPacedEnabled = true,
            isDeepLinkEnabled = false,
        )
    )

    private val courseStructure = CourseStructure(
        root = "",
        blockData = listOf(),
        id = "id",
        name = "Course name",
        number = "",
        org = "Org",
        start = Date(0),
        startDisplay = "",
        startType = "",
        end = null,
        media = null,
        courseAccessDetails = CourseAccessDetails(
            hasUnmetPrerequisites = false,
            isTooEarly = false,
            isStaff = false,
            auditAccessExpires = Date(),
            coursewareAccess = CoursewareAccess(
                true,
                "",
                "",
                "",
                "",
                ""
            )
        ),
        certificate = null,
        isSelfPaced = false,
        progress = null,
        enrollmentDetails = EnrollmentDetails(
            created = Date(),
            mode = "audit",
            isActive = false,
            upgradeDeadline = Date()
        ),
        productInfo = null
    )

    private val enrollmentDetails = CourseEnrollmentDetails(
        id = "",
        courseUpdates = "",
        courseHandouts = "",
        discussionUrl = "",
        courseAccessDetails = CourseAccessDetails(
            hasUnmetPrerequisites = false,
            isTooEarly = false,
            isStaff = false,
            auditAccessExpires = null,
            coursewareAccess = CoursewareAccess(
                false, "", "", "",
                "", ""
            )
        ),
        certificate = null,
        enrollmentDetails = EnrollmentDetails(
            null, "", false, null
        ),
        courseInfoOverview = CourseInfoOverview(
            "Open edX Demo Course", "", "OpenedX", "", null,
            "", "", null, false, null,
            CourseSharingUtmParameters("", ""),
            "", listOf(), null
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { resourceManager.getString(id = R.string.platform_name) } returns openEdx
        every { resourceManager.getString(R.string.core_error_no_connection) } returns noInternet
        every { resourceManager.getString(R.string.core_error_unknown_error) } returns somethingWrong
        every { corePreferences.user } returns CoreMocks.mockUser
        every { corePreferences.appConfig } returns CoreMocks.mockAppConfig
        every { courseNotifier.notifier } returns emptyFlow()
        every { corePreferences.user } returns user
        every { corePreferences.appConfig } returns appConfig
        every { courseNotifier.notifier } returns emptyFlow()
        every { calendarManager.getCourseCalendarTitle(any()) } returns calendarTitle
        every { config.getApiHostURL() } returns "baseUrl"
        coEvery { interactor.getEnrollmentDetails(any()) } returns CoreMocks.mockCourseEnrollmentDetails
        every { imageProcessor.loadImage(any(), any(), any()) } returns Unit
        every { imageProcessor.applyBlur(any(), any()) } returns mockBitmap
        every { courseAnalytics.logScreenEvent(any(), any()) } returns Unit

        mockkConstructor(Logger::class)
        every { anyConstructed<Logger>().e(any(), any()) } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Suppress("TooGenericExceptionThrown")
    @Test
    fun `getCourseEnrollmentDetails unknown exception`() = runTest {
        val viewModel = CourseContainerViewModel(
            courseId = "",
            courseName = "",
            showTrackSelection = false,
            resumeBlockId = "",
            openTab = "",
            config = config,
            interactor = interactor,
            calendarManager = calendarManager,
            resourceManager = resourceManager,
            courseNotifier = courseNotifier,
            iapNotifier = iapNotifier,
            iapInteractor = iapInteractor,
            networkConnection = networkConnection,
            corePreferences = corePreferences,
            coursePreferences = coursePreferences,
            courseAnalytics = courseAnalytics,
            iapAnalytics = iapAnalytics,
            imageProcessor = imageProcessor,
            courseRouter = courseRouter,
        )
        every { networkConnection.isOnline() } returns true
        every { iapInteractor.isIAPEnabled } returns true
        coEvery { interactor.getEnrollmentDetailsFlow(any()) } returns flow { throw Exception() }

        viewModel.fetchCourseDetails()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getEnrollmentDetailsFlow(any()) }
        assert(!viewModel.refreshing.value)
        assert(viewModel.courseAccessStatus.value == CourseAccessError.UNKNOWN)
    }

    @Test
    fun `getCourseEnrollmentDetails success with internet`() = runTest {
        val viewModel = CourseContainerViewModel(
            courseId = "",
            courseName = "",
            showTrackSelection = false,
            resumeBlockId = "",
            openTab = "",
            config = config,
            interactor = interactor,
            calendarManager = calendarManager,
            resourceManager = resourceManager,
            courseNotifier = courseNotifier,
            iapNotifier = iapNotifier,
            iapInteractor = iapInteractor,
            networkConnection = networkConnection,
            corePreferences = corePreferences,
            coursePreferences = coursePreferences,
            courseAnalytics = courseAnalytics,
            iapAnalytics = iapAnalytics,
            imageProcessor = imageProcessor,
            courseRouter = courseRouter,
        )
        every { networkConnection.isOnline() } returns true
        every { iapInteractor.isIAPEnabled } returns true
        coEvery { interactor.getEnrollmentDetailsFlow(any()) } returns flowOf(
            CourseEnrollmentDetailsSource.Remote(enrollmentDetails)
        )

        viewModel.fetchCourseDetails()
        coEvery { interactor.getCourseStructureFlow(any(), any()) } returns flowOf(
            CoreMocks.mockCourseStructure
        )
        coEvery { interactor.getEnrollmentDetailsFlow(any()) } returns flowOf(
            CoreMocks.mockCourseEnrollmentDetails
        )
        every {
            analytics.logScreenEvent(
                CourseAnalyticsEvent.DASHBOARD.eventName,
                any()
            )
        } returns Unit
        every {
            analytics.logScreenEvent(
                CourseAnalyticsEvent.HOME_TAB.eventName,
                any()
            )
        } returns Unit
        viewModel.fetchCourseDetails()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getEnrollmentDetailsFlow(any()) }
        coVerify(exactly = 1) { interactor.getEnrollmentDetailsFlow(any()) }
        verify(exactly = 1) {
            analytics.logScreenEvent(
                CourseAnalyticsEvent.DASHBOARD.eventName,
                any()
            )
        }
        verify(exactly = 1) {
            analytics.logScreenEvent(
                CourseAnalyticsEvent.HOME_TAB.eventName,
                any()
            )
        }
        assert(viewModel.errorMessage.value == null)
        assert(!viewModel.refreshing.value)
        assert(viewModel.courseAccessStatus.value != null)
    }

    @Test
    fun `getCourseEnrollmentDetails success without internet`() = runTest {
        val viewModel = CourseContainerViewModel(
            courseId = "",
            courseName = "",
            showTrackSelection = false,
            resumeBlockId = "",
            openTab = "",
            config = config,
            interactor = interactor,
            resourceManager = resourceManager,
            courseNotifier = courseNotifier,
            iapNotifier = iapNotifier,
            iapInteractor = iapInteractor,
            networkConnection = networkConnection,
            corePreferences = corePreferences,
            coursePreferences = coursePreferences,
            courseAnalytics = courseAnalytics,
            iapAnalytics = iapAnalytics,
            imageProcessor = imageProcessor,
            courseRouter = courseRouter,
        )
        every { networkConnection.isOnline() } returns false
        every { iapInteractor.isIAPEnabled } returns true
        coEvery { interactor.getEnrollmentDetailsFlow(any()) } returns flowOf(
            CourseEnrollmentDetailsSource.Remote(enrollmentDetails)
        )

        viewModel.fetchCourseDetails()
        coEvery { interactor.getCourseStructureFlow(any(), any()) } returns flowOf(
            CoreMocks.mockCourseStructure
        )
        coEvery { interactor.getEnrollmentDetailsFlow(any()) } returns flowOf(
            CoreMocks.mockCourseEnrollmentDetails
        )
        every {
            analytics.logScreenEvent(
                CourseAnalyticsEvent.DASHBOARD.eventName,
                any()
            )
        } returns Unit
        every {
            analytics.logScreenEvent(
                CourseAnalyticsEvent.HOME_TAB.eventName,
                any()
            )
        } returns Unit
        viewModel.fetchCourseDetails()
        advanceUntilIdle()
        coVerify(exactly = 0) { courseApi.getEnrollmentDetails(any()) }
        coVerify(exactly = 0) { courseApi.getEnrollmentDetails(any()) }
        verify(exactly = 1) {
            analytics.logScreenEvent(
                CourseAnalyticsEvent.DASHBOARD.eventName,
                any()
            )
        }
        verify(exactly = 1) {
            analytics.logScreenEvent(
                CourseAnalyticsEvent.HOME_TAB.eventName,
                any()
            )
        }

        assert(viewModel.errorMessage.value == null)
        assert(!viewModel.refreshing.value)
        assert(viewModel.courseAccessStatus.value != null)
    }

    @Test
    fun `updateData unknown exception`() = runTest {
        val viewModel = CourseContainerViewModel(
            courseId = "",
            courseName = "",
            showTrackSelection = false,
            resumeBlockId = "",
            openTab = "",
            config = config,
            interactor = interactor,
            resourceManager = resourceManager,
            courseNotifier = courseNotifier,
            iapNotifier = iapNotifier,
            iapInteractor = iapInteractor,
            networkConnection = networkConnection,
            corePreferences = corePreferences,
            coursePreferences = coursePreferences,
            courseAnalytics = courseAnalytics,
            iapAnalytics = iapAnalytics,
            imageProcessor = imageProcessor,
            courseRouter = courseRouter,
        )
        coEvery { interactor.getCourseStructure(any(), true) } throws Exception()
        coEvery { courseNotifier.send(CourseStructureUpdated("")) } returns Unit
        viewModel.updateData()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseStructure(any(), true) }

        val message = viewModel.errorMessage.value
        assertEquals(somethingWrong, message)
        assert(!viewModel.refreshing.value)
    }

    @Test
    fun `updateData success`() = runTest {
        val viewModel = CourseContainerViewModel(
            courseId = "",
            courseName = "",
            showTrackSelection = false,
            resumeBlockId = "",
            openTab = "",
            config = config,
            interactor = interactor,
            calendarManager = calendarManager,
            resourceManager = resourceManager,
            courseNotifier = courseNotifier,
            iapNotifier = iapNotifier,
            iapInteractor = iapInteractor,
            networkConnection = networkConnection,
            corePreferences = corePreferences,
            coursePreferences = coursePreferences,
            courseAnalytics = courseAnalytics,
            iapAnalytics = iapAnalytics,
            imageProcessor = imageProcessor,
            courseRouter = courseRouter,
        )
        coEvery { interactor.getCourseStructure(any(), true) } returns courseStructure
        coEvery { courseNotifier.send(CourseStructureUpdated("")) } returns Unit
        viewModel.updateData()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseStructure(any(), true) }

        assert(viewModel.errorMessage.value == null)
        assert(!viewModel.refreshing.value)
    }
}
