package org.openedx.course.presentation.container

import android.graphics.Bitmap
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
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
import org.openedx.core.ImageProcessor
import org.openedx.core.R
import org.openedx.core.config.Config
import org.openedx.core.data.api.CourseApi
import org.openedx.core.data.model.CourseStructureModel
import org.openedx.core.data.model.User
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.interactor.IAPInteractor
import org.openedx.core.domain.model.AppConfig
import org.openedx.core.domain.model.CourseAccessDetails
import org.openedx.core.domain.model.CourseAccessError
import org.openedx.core.domain.model.CourseDatesCalendarSync
import org.openedx.core.domain.model.CourseEnrollmentDetails
import org.openedx.core.domain.model.CourseInfoOverview
import org.openedx.core.domain.model.CourseSharingUtmParameters
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.domain.model.CoursewareAccess
import org.openedx.core.domain.model.EnrollmentDetails
import org.openedx.core.domain.model.iap.ProductInfo
import org.openedx.core.presentation.IAPAnalytics
import org.openedx.core.system.CalendarManager
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseStructureUpdated
import org.openedx.core.system.notifier.IAPNotifier
import org.openedx.course.data.storage.CoursePreferences
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.CourseAnalyticsEvent
import org.openedx.course.presentation.CourseRouter
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class CourseContainerViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    private val resourceManager = mockk<ResourceManager>()
    private val config = mockk<Config>()
    private val interactor = mockk<CourseInteractor>()
    private val calendarManager = mockk<CalendarManager>()
    private val networkConnection = mockk<NetworkConnection>()
    private val courseNotifier = spyk<CourseNotifier>()
    private val iapNotifier = spyk<IAPNotifier>()
    private val iapInteractor = mockk<IAPInteractor>()
    private val courseAnalytics = mockk<CourseAnalytics>()
    private val iapAnalytics = mockk<IAPAnalytics>()
    private val corePreferences = mockk<CorePreferences>()
    private val coursePreferences = mockk<CoursePreferences>()
    private val mockBitmap = mockk<Bitmap>()
    private val imageProcessor = mockk<ImageProcessor>()
    private val courseRouter = mockk<CourseRouter>()
    private val courseApi = mockk<CourseApi>()

    private val openEdx = "OpenEdx"
    private val calendarTitle = "OpenEdx - Abc"
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
    private val courseDetails = CourseEnrollmentDetails(
        id = "id",
        courseUpdates = "",
        courseHandouts = "",
        discussionUrl = "",
        courseAccessDetails = CourseAccessDetails(
            false,
            false,
            false,
            null,
            coursewareAccess = CoursewareAccess(
                false, "", "", "",
                "", ""

            )
        ),
        certificate = null,
        enrollmentDetails = EnrollmentDetails(
            null, "audit", false, Date()
        ),
        courseInfoOverview = CourseInfoOverview(
            "Open edX Demo Course", "", "OpenedX", Date(),
            "", "", null, false, null,
            CourseSharingUtmParameters("", ""),
            "", listOf(), ProductInfo("", "", 1.0)
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

    private val courseStructureModel = CourseStructureModel(
        root = "",
        blockData = mapOf(),
        id = "id",
        name = "Course name",
        number = "",
        org = "Org",
        start = "",
        startDisplay = "",
        startType = "",
        end = null,
        courseAccessDetails = org.openedx.core.data.model.CourseAccessDetails(
            hasUnmetPrerequisites = false,
            isTooEarly = false,
            isStaff = false,
            auditAccessExpires = "",
            coursewareAccess = null
        ),
        media = null,
        certificate = null,
        isSelfPaced = false,
        progress = null,
        enrollmentDetails = org.openedx.core.data.model.EnrollmentDetails("", "", "", false, ""),
        courseModes = arrayListOf()
    )

    private val enrollmentDetails = CourseEnrollmentDetails(
        id = "",
        courseUpdates = "",
        courseHandouts = "",
        discussionUrl = "",
        courseAccessDetails = CourseAccessDetails(
            false,
            false,
            false,
            null,
            CoursewareAccess(
                false, "", "", "",
                "", ""
            )
        ),
        certificate = null,
        enrollmentDetails = EnrollmentDetails(
            null, "", false, null
        ),
        courseInfoOverview = CourseInfoOverview(
            "Open edX Demo Course", "", "OpenedX", null,
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
        every { corePreferences.user } returns user
        every { corePreferences.appConfig } returns appConfig
        every { courseNotifier.notifier } returns emptyFlow()
        every { calendarManager.getCourseCalendarTitle(any()) } returns calendarTitle
        every { config.getApiHostURL() } returns "baseUrl"
        coEvery { interactor.getEnrollmentDetails(any()) } returns courseDetails
        every { imageProcessor.loadImage(any(), any(), any()) } returns Unit
        every { imageProcessor.applyBlur(any(), any()) } returns mockBitmap
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getCourseEnrollmentDetails unknown exception`() = runTest {
        val viewModel = CourseContainerViewModel(
            courseId = "",
            courseName = "",
            resumeBlockId = "",
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
        coEvery { interactor.getEnrollmentDetails(any()) } throws Exception()
        every {
            courseAnalytics.logScreenEvent(
                CourseAnalyticsEvent.DASHBOARD.eventName,
                any()
            )
        } returns Unit
        every {
            courseAnalytics.logScreenEvent(
                CourseAnalyticsEvent.HOME_TAB.eventName,
                any()
            )
        } returns Unit
        viewModel.fetchCourseDetails()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getEnrollmentDetails(any()) }
        verify(exactly = 1) {
            courseAnalytics.logScreenEvent(
                CourseAnalyticsEvent.DASHBOARD.eventName,
                any()
            )
        }
        verify(exactly = 1) {
            courseAnalytics.logScreenEvent(
                CourseAnalyticsEvent.HOME_TAB.eventName,
                any()
            )
        }
        assert(!viewModel.refreshing.value)
        assert(viewModel.courseAccessStatus.value == CourseAccessError.UNKNOWN)
    }

    @Test
    fun `getCourseEnrollmentDetails success with internet`() = runTest {
        val viewModel = CourseContainerViewModel(
            courseId = "",
            courseName = "",
            resumeBlockId = "",
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
        coEvery { interactor.getEnrollmentDetails(any()) } returns enrollmentDetails
        every {
            courseAnalytics.logScreenEvent(
                CourseAnalyticsEvent.DASHBOARD.eventName,
                any()
            )
        } returns Unit
        every {
            courseAnalytics.logScreenEvent(
                CourseAnalyticsEvent.HOME_TAB.eventName,
                any()
            )
        } returns Unit
        viewModel.fetchCourseDetails()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getEnrollmentDetails(any()) }
        verify(exactly = 1) {
            courseAnalytics.logScreenEvent(
                CourseAnalyticsEvent.DASHBOARD.eventName,
                any()
            )
        }
        verify(exactly = 1) {
            courseAnalytics.logScreenEvent(
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
            resumeBlockId = "",
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
        every { networkConnection.isOnline() } returns false
        coEvery { interactor.getEnrollmentDetails(any()) } returns enrollmentDetails
        every {
            courseAnalytics.logScreenEvent(
                CourseAnalyticsEvent.DASHBOARD.eventName,
                any()
            )
        } returns Unit
        every {
            courseAnalytics.logScreenEvent(
                CourseAnalyticsEvent.HOME_TAB.eventName,
                any()
            )
        } returns Unit
        viewModel.fetchCourseDetails()
        advanceUntilIdle()
        coVerify(exactly = 0) { courseApi.getEnrollmentDetails(any()) }
        verify(exactly = 1) {
            courseAnalytics.logScreenEvent(
                CourseAnalyticsEvent.DASHBOARD.eventName,
                any()
            )
        }
        verify(exactly = 1) {
            courseAnalytics.logScreenEvent(
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
            resumeBlockId = "",
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
            resumeBlockId = "",
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
        coEvery { interactor.getEnrollmentDetails(any()) } returns courseDetails
        coEvery { interactor.getCourseStructure(any(), true) } returns courseStructure
        coEvery { courseNotifier.send(CourseStructureUpdated("")) } returns Unit
        viewModel.updateData()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseStructure(any(), true) }

        assert(viewModel.errorMessage.value == null)
        assert(!viewModel.refreshing.value)
    }
}
