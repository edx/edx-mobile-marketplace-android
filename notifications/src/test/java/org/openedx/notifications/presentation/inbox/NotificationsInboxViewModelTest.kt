package org.openedx.notifications.presentation.inbox

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.FragmentManager
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.core.presentation.global.FullScreenState
import org.openedx.core.system.ResourceManager
import org.openedx.core.utils.Logger
import org.openedx.notifications.domain.interactor.NotificationsInteractor
import org.openedx.notifications.domain.model.InboxNotifications
import org.openedx.notifications.domain.model.InboxSection
import org.openedx.notifications.domain.model.NotificationContent
import org.openedx.notifications.domain.model.NotificationItem
import org.openedx.notifications.domain.model.Pagination
import org.openedx.notifications.presentation.NotificationsAnalytics
import org.openedx.notifications.presentation.NotificationsRouter
import java.net.UnknownHostException
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsInboxViewModelTest {

    @get:Rule
    val taskInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: NotificationsInboxViewModel
    private lateinit var message: Deferred<UIMessage.SnackBarMessage?>
    private val interactor: NotificationsInteractor = mockk(relaxed = true)
    private val router: NotificationsRouter = mockk(relaxed = true)
    private val resourceManager: ResourceManager = mockk(relaxed = true)
    private val analytics: NotificationsAnalytics = mockk(relaxed = true)
    private val fragmentManager = mockk<FragmentManager>(relaxed = true)

    private val noInternet = "Slow or no internet connection"
    private val somethingWrong = "Something went wrong"

    private val mockNotificationITem = NotificationItem(
        id = 1,
        appName = "",
        notificationType = "",
        content = "",
        contentUrl = "",
        courseId = "",
        lastRead = null,
        lastSeen = null,
        created = Date(),
        contentContext = NotificationContent(
            paragraph = "",
            strongText = "",
            topicId = "",
            threadId = "",
            responseId = "",
            commentId = "",
            postTitle = "",
            courseName = "",
            replierName = "",
            emailContent = "",
            authorName = "",
            authorPronoun = "",
        ),
    )

    private val mockNotificationItems = listOf(
        mockNotificationITem.copy(id = 1),
        mockNotificationITem.copy(id = 2),
        mockNotificationITem.copy(id = 3),
    )

    private val mockInboxSections = mapOf(
        InboxSection.RECENT to mockNotificationItems,
        InboxSection.THIS_WEEK to mockNotificationItems,
        InboxSection.OLDER to mockNotificationItems,
    )

    private val mockInboxNotifications = InboxNotifications(
        pagination = Pagination("url", "", 3, 2, 1, 1),
        notifications = mockInboxSections,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        coEvery { interactor.getInboxNotifications(any()) } returns mockInboxNotifications
        coEvery { interactor.markAllNotificationsAsRead() } returns true
        coEvery { interactor.markNotificationAsRead(any()) } returns true

        every { resourceManager.getString(R.string.core_error_no_connection) } returns noInternet
        every { resourceManager.getString(R.string.core_error_unknown_error) } returns somethingWrong

        mockkConstructor(Logger::class)
        every { anyConstructed<Logger>().e(any(), any()) } returns Unit
    }

    @Test
    fun `init onViewModelCreation`() = runTest {
        viewModel = NotificationsInboxViewModel(interactor, router, resourceManager, analytics)

        coVerify(exactly = 1) { interactor.markNotificationsAsSeen() }
        coVerify(exactly = 1) { interactor.getInboxNotifications(any()) }
        coVerify(exactly = 1) { analytics.logScreenEvent(any(), any()) }
        coVerify(exactly = 0) { analytics.logEvent(any(), any()) }

        assertTrue(viewModel.canLoadMore.value)
    }

    @Test
    fun `internalLoadNotifications dataState`() = runTest {
        viewModel = NotificationsInboxViewModel(interactor, router, resourceManager, analytics)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getInboxNotifications(1) }
        assertEquals(InboxUIState.Data(mockInboxSections), viewModel.uiState.value)
        assertTrue(viewModel.canLoadMore.value)
    }

    @Test
    fun `internalLoadNotifications emptyState`() = runTest {
        coEvery { interactor.getInboxNotifications(1) } returns mockInboxNotifications.copy(
            notifications = emptyMap()
        )
        viewModel = NotificationsInboxViewModel(interactor, router, resourceManager, analytics)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getInboxNotifications(1) }
        assertEquals(InboxUIState.Fallback(InboxFullScreenState.Empty), viewModel.uiState.value)
    }

    @Test
    fun `internalLoadNotifications networkError`() = runTest(UnconfinedTestDispatcher()) {
        coEvery { interactor.getInboxNotifications(1) } throws UnknownHostException()

        viewModel = NotificationsInboxViewModel(interactor, router, resourceManager, analytics)
        message = async {
            withTimeoutOrNull(5000) {
                viewModel.uiMessage.first() as UIMessage.SnackBarMessage
            }
        }
        viewModel.fetchMore()

        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getInboxNotifications(1) }
        assertEquals(InboxUIState.Fallback(FullScreenState.NetworkError), viewModel.uiState.value)
        assertNull(message.await()?.message)
    }

    @Test
    fun `internalLoadNotifications serverError`() = runTest(UnconfinedTestDispatcher()) {
        coEvery { interactor.getInboxNotifications(1) } throws Exception()

        viewModel = NotificationsInboxViewModel(interactor, router, resourceManager, analytics)
        message = async {
            withTimeoutOrNull(5000) {
                viewModel.uiMessage.first() as UIMessage.SnackBarMessage
            }
        }
        viewModel.fetchMore()

        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getInboxNotifications(1) }
        assertEquals(InboxUIState.Fallback(FullScreenState.ServerError), viewModel.uiState.value)
        assertNull(message.await()?.message)
    }

    @Test
    fun `fetchMore success`() = runTest(UnconfinedTestDispatcher()) {
        viewModel = NotificationsInboxViewModel(interactor, router, resourceManager, analytics)
        viewModel.fetchMore()
        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getInboxNotifications(any()) }
        assertTrue(viewModel.uiState.value is InboxUIState.Data)
    }

    @Test
    fun `fetchMore noInternet`() = runTest(UnconfinedTestDispatcher()) {
        coEvery { interactor.getInboxNotifications(1) } returns mockInboxNotifications
        coEvery { interactor.getInboxNotifications(2) } throws UnknownHostException()

        viewModel = NotificationsInboxViewModel(interactor, router, resourceManager, analytics)
        message = async { viewModel.uiMessage.first() as? UIMessage.SnackBarMessage }
        viewModel.fetchMore()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getInboxNotifications(1) }
        coVerify(exactly = 1) { interactor.getInboxNotifications(2) }

        assertEquals(InboxUIState.Data(mockInboxSections), viewModel.uiState.value)
        assertEquals(noInternet, message.await()?.message)
    }

    @Test
    fun `fetchMore serverError`() = runTest(UnconfinedTestDispatcher()) {
        coEvery { interactor.getInboxNotifications(1) } returns mockInboxNotifications
        coEvery { interactor.getInboxNotifications(2) } throws Exception()

        viewModel = NotificationsInboxViewModel(interactor, router, resourceManager, analytics)
        message = async { viewModel.uiMessage.first() as? UIMessage.SnackBarMessage }
        viewModel.fetchMore()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getInboxNotifications(1) }
        coVerify(exactly = 1) { interactor.getInboxNotifications(2) }

        assertEquals(InboxUIState.Data(mockInboxSections), viewModel.uiState.value)
        assertEquals(somethingWrong, message.await()?.message)
    }

    @Test
    fun `markNotificationAsSeen success`() = runTest {
        viewModel = NotificationsInboxViewModel(interactor, router, resourceManager, analytics)

        coVerify(exactly = 1) { interactor.markNotificationsAsSeen() }
    }

    @Test
    fun `markNotificationAsSeen noInternet`() = runTest {
        coEvery { interactor.markNotificationsAsSeen() } throws UnknownHostException()
        viewModel = NotificationsInboxViewModel(interactor, router, resourceManager, analytics)
        message = async {
            withTimeoutOrNull(5000) {
                viewModel.uiMessage.first() as UIMessage.SnackBarMessage
            }
        }
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.markNotificationsAsSeen() }
        assertNull(message.await()?.message)
    }

    @Test
    fun `markNotificationAsRead success`() = runTest(UnconfinedTestDispatcher()) {
        viewModel = NotificationsInboxViewModel(interactor, router, resourceManager, analytics)
        viewModel.markNotificationAsRead(
            fm = fragmentManager,
            notification = mockNotificationITem,
            inboxSection = InboxSection.RECENT,
        )
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.markNotificationAsRead(any()) }
        coVerify(exactly = 1) { analytics.logEvent(any(), any()) }
    }

    @Test
    fun `markNotificationAsRead alreadyRead noInternet`() = runTest(UnconfinedTestDispatcher()) {
        coEvery { interactor.markNotificationAsRead(any()) } throws UnknownHostException()

        viewModel = NotificationsInboxViewModel(interactor, router, resourceManager, analytics)
        message = async {
            withTimeoutOrNull(5000) {
                viewModel.uiMessage.first() as? UIMessage.SnackBarMessage
            }
        }
        viewModel.markNotificationAsRead(
            fm = fragmentManager,
            notification = mockNotificationITem.copy(lastRead = Date()),
            inboxSection = InboxSection.RECENT,
        )
        advanceUntilIdle()

        coVerify(exactly = 0) { interactor.markNotificationAsRead(any()) }
        coVerify(exactly = 1) { analytics.logEvent(any(), any()) }
        assertNull(message.await()?.message)
    }

    @Test
    fun `markNotificationAsRead noInternet`() = runTest(UnconfinedTestDispatcher()) {
        coEvery { interactor.markNotificationAsRead(any()) } throws UnknownHostException()

        viewModel = NotificationsInboxViewModel(interactor, router, resourceManager, analytics)
        message = async { viewModel.uiMessage.first() as? UIMessage.SnackBarMessage }
        viewModel.markNotificationAsRead(
            fm = fragmentManager,
            notification = mockNotificationITem,
            inboxSection = InboxSection.RECENT,
        )
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.markNotificationAsRead(any()) }
        coVerify(exactly = 0) { analytics.logEvent(any(), any()) }
        assertEquals(noInternet, message.await()?.message)
    }

    @Test
    fun `markAllNotificationsAsRead success`() = runTest(UnconfinedTestDispatcher()) {
        viewModel = NotificationsInboxViewModel(interactor, router, resourceManager, analytics)
        val notificationsBefore = mockInboxNotifications.notifications.values.flatten()
        notificationsBefore.forEach {
            assertNull(it.lastRead)
        }

        viewModel.markAllNotificationsAsRead()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.markAllNotificationsAsRead() }
        coVerify(exactly = 1) { analytics.logEvent(any(), any()) }

        val updatedState = viewModel.uiState.value as InboxUIState.Data
        updatedState.notifications.values.flatten().forEach {
            assertNotNull(it.lastRead)
        }
    }

    @Test
    fun `markAllNotificationsAsRead noInternet`() = runTest(UnconfinedTestDispatcher()) {
        coEvery { interactor.markAllNotificationsAsRead() } throws UnknownHostException()

        viewModel = NotificationsInboxViewModel(interactor, router, resourceManager, analytics)
        message = async { viewModel.uiMessage.first() as? UIMessage.SnackBarMessage }
        viewModel.markAllNotificationsAsRead()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.markAllNotificationsAsRead() }
        coVerify(exactly = 1) { analytics.logEvent(any(), any()) }

        assertEquals(noInternet, message.await()?.message)
    }

    @Test
    fun `onRefreshNotifications success`() = runTest {
        viewModel = NotificationsInboxViewModel(interactor, router, resourceManager, analytics)
        viewModel.onRefreshNotifications()
        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getInboxNotifications(1) }
        assertEquals(InboxUIState.Data(mockInboxSections), viewModel.uiState.value)
    }

    @Test
    fun `onRefreshNotifications noInternet`() = runTest(UnconfinedTestDispatcher()) {
        viewModel = NotificationsInboxViewModel(interactor, router, resourceManager, analytics)
        coEvery { interactor.getInboxNotifications(1) } throws UnknownHostException()

        message = async { viewModel.uiMessage.first() as? UIMessage.SnackBarMessage }
        viewModel.onRefreshNotifications()
        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getInboxNotifications(1) }
        assertEquals(InboxUIState.Data(mockInboxSections), viewModel.uiState.value)
        assertEquals(noInternet, message.await()?.message)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }
}
