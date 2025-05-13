package org.openedx.notifications.presentation.settings

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.app.NotificationManagerCompat
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import junit.framework.TestCase.assertEquals
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
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.core.utils.Logger
import org.openedx.notifications.data.storage.NotificationsPreferences
import org.openedx.notifications.domain.interactor.NotificationsInteractor
import org.openedx.notifications.domain.model.NotificationsConfiguration
import org.openedx.notifications.domain.model.NotificationsUpdateResponse
import org.openedx.notifications.presentation.NotificationsAnalytics
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsSettingsViewModelTest {

    @get:Rule
    val taskInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: NotificationsSettingsViewModel
    private lateinit var message: Deferred<UIMessage.SnackBarMessage?>
    private val context: Context = mockk(relaxed = true)
    private val interactor: NotificationsInteractor = mockk(relaxed = true)
    private val analytics: NotificationsAnalytics = mockk(relaxed = true)
    private val preferences: NotificationsPreferences = mockk(relaxed = true)
    private val notificationManager = mockk<NotificationManagerCompat>()

    private val somethingWrong = "Service is unavailable. Please try again later."

    private val mockNotificationsConfiguration = NotificationsConfiguration(true)
    private val mockNotificationsUpdateResponse = NotificationsUpdateResponse(
        status = "",
        updatedValue = true,
        notificationType = "",
        channel = "",
        app = "",
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkStatic(NotificationManagerCompat::class)

        every { NotificationManagerCompat.from(any()) } returns notificationManager
        every { context.getString(R.string.core_service_unavailable_message) } returns somethingWrong

        mockkConstructor(Logger::class)
        every { anyConstructed<Logger>().e(any(), any()) } returns Unit
    }

    @Test
    fun `init onViewModelCreation`() = runTest {
        every { notificationManager.areNotificationsEnabled() } returns true
        every { preferences.notifications.discussionsPushEnabled } returns true
        coEvery { interactor.fetchNotificationsConfiguration() } returns mockNotificationsConfiguration

        viewModel = NotificationsSettingsViewModel(context, interactor, analytics, preferences)
        val uiState = viewModel.uiState.value as? NotificationsSettingsUiState.Configuration

        coVerify(exactly = 1) { interactor.fetchNotificationsConfiguration() }
        coVerify(exactly = 1) { analytics.logScreenEvent(any(), any()) }
        assertEquals(true, uiState?.discussionsPushEnabled)
    }

    @Test
    fun `init onViewModelCreation noInternet`() = runTest {
        every { notificationManager.areNotificationsEnabled() } returns true
        every { preferences.notifications.discussionsPushEnabled } returns true
        coEvery { interactor.fetchNotificationsConfiguration() } throws UnknownHostException()

        viewModel = NotificationsSettingsViewModel(context, interactor, analytics, preferences)
        val uiState = viewModel.uiState.value as? NotificationsSettingsUiState.Configuration

        coVerify(exactly = 1) { interactor.fetchNotificationsConfiguration() }
        coVerify(exactly = 1) { analytics.logScreenEvent(any(), any()) }
        assertEquals(true, uiState?.discussionsPushEnabled)
    }

    @Test
    fun `init onViewModelCreation noInternet noPreference`() = runTest {
        every { notificationManager.areNotificationsEnabled() } returns true
        every { preferences.notifications.discussionsPushEnabled } returns false
        coEvery { interactor.fetchNotificationsConfiguration() } throws UnknownHostException()

        viewModel = NotificationsSettingsViewModel(context, interactor, analytics, preferences)
        val uiState = viewModel.uiState.value as? NotificationsSettingsUiState.Configuration

        coVerify(exactly = 1) { interactor.fetchNotificationsConfiguration() }
        coVerify(exactly = 1) { analytics.logScreenEvent(any(), any()) }
        assertEquals(false, uiState?.discussionsPushEnabled)
    }

    @Test
    fun `init onViewModelCreation noPermission`() = runTest {
        every { notificationManager.areNotificationsEnabled() } returns false

        viewModel = NotificationsSettingsViewModel(context, interactor, analytics, preferences)
        val uiState = viewModel.uiState.value as? NotificationsSettingsUiState.Configuration

        coVerify(exactly = 0) { interactor.fetchNotificationsConfiguration() }
        coVerify(exactly = 1) { analytics.logScreenEvent(any(), any()) }
        assertEquals(false, uiState?.discussionsPushEnabled)
    }

    @Test
    fun `fetchAndUpdateNotificationsSettings success`() = runTest {
        every { notificationManager.areNotificationsEnabled() } returns true
        every { preferences.notifications.discussionsPushEnabled } returns true
        coEvery { interactor.fetchNotificationsConfiguration() } returns mockNotificationsConfiguration

        viewModel = NotificationsSettingsViewModel(context, interactor, analytics, preferences)
        advanceUntilIdle()

        val uiState = viewModel.uiState.value as? NotificationsSettingsUiState.Configuration

        coVerify(exactly = 1) { interactor.fetchNotificationsConfiguration() }
        coVerify(exactly = 0) { analytics.logEvent(any(), any()) }
        assertEquals(true, uiState?.discussionsPushEnabled)
    }

    @Test
    fun `setDiscussionNotificationPreference success`() = runTest {
        every { notificationManager.areNotificationsEnabled() } returns true
        every { preferences.notifications.discussionsPushEnabled } returns true
        coEvery { interactor.fetchNotificationsConfiguration() } returns mockNotificationsConfiguration
        coEvery { interactor.updateNotificationsConfiguration(any()) } returns mockNotificationsUpdateResponse.copy(
            updatedValue = false
        )

        viewModel = NotificationsSettingsViewModel(context, interactor, analytics, preferences)
        viewModel.setDiscussionNotificationPreference(false)
        val uiState = viewModel.uiState.value as? NotificationsSettingsUiState.Configuration
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.updateNotificationsConfiguration(false) }
        coVerify(exactly = 1) { analytics.logEvent(any(), any()) }
        assertEquals(false, uiState?.discussionsPushEnabled)
    }

    @Test
    fun `setDiscussionNotificationPreference noInternet`() = runTest(UnconfinedTestDispatcher()) {
        every { notificationManager.areNotificationsEnabled() } returns true
        every { preferences.notifications.discussionsPushEnabled } returns true
        coEvery { interactor.fetchNotificationsConfiguration() } returns mockNotificationsConfiguration
        coEvery { interactor.updateNotificationsConfiguration(any()) } throws UnknownHostException()

        viewModel = NotificationsSettingsViewModel(context, interactor, analytics, preferences)
        message = async { viewModel.uiMessage.first() as? UIMessage.SnackBarMessage }
        advanceUntilIdle()

        viewModel.setDiscussionNotificationPreference(false)
        val uiState = viewModel.uiState.value as? NotificationsSettingsUiState.Configuration
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.updateNotificationsConfiguration(false) }
        coVerify(exactly = 1) { analytics.logEvent(any(), any()) }

        assertEquals(true, uiState?.discussionsPushEnabled)
        assertEquals(somethingWrong, message.await()?.message)
    }

    @Test
    fun `setDiscussionNotificationPreference noPermission`() = runTest(UnconfinedTestDispatcher()) {
        every { notificationManager.areNotificationsEnabled() } returns false

        viewModel = NotificationsSettingsViewModel(context, interactor, analytics, preferences)
        val uiState = viewModel.uiState.value as? NotificationsSettingsUiState.Configuration
        val uiEvent = async { viewModel.uiEvent.first() }
        advanceUntilIdle()

        viewModel.setDiscussionNotificationPreference(true)

        coVerify(exactly = 0) { interactor.updateNotificationsConfiguration(false) }
        coVerify(exactly = 1) { analytics.logEvent(any(), any()) }

        assertEquals(false, uiState?.discussionsPushEnabled)
        assertEquals(NotificationsSettingsUiEvent.RequestPermission, uiEvent.await())
    }

    @Test
    fun `setDiscussionNotificationPreference enablePermission`() = runTest {
        every { notificationManager.areNotificationsEnabled() } returns false

        viewModel = NotificationsSettingsViewModel(context, interactor, analytics, preferences)
        viewModel.setDiscussionNotificationPreference(true)
        advanceUntilIdle()

        // Permission Disabled
        coVerify(exactly = 0) { interactor.updateNotificationsConfiguration(true) }
        coVerify(exactly = 1) { analytics.logEvent(any(), any()) }
        val uiState = viewModel.uiState.value as? NotificationsSettingsUiState.Configuration
        assertEquals(false, uiState?.discussionsPushEnabled)

        // Permission Enabled
        every { notificationManager.areNotificationsEnabled() } returns true
        coEvery { interactor.updateNotificationsConfiguration(any()) } returns mockNotificationsUpdateResponse

        viewModel.setDiscussionNotificationPreference(true)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.updateNotificationsConfiguration(true) }
        coVerify(exactly = 2) { analytics.logEvent(any(), any()) }
        val newUiState = viewModel.uiState.value as? NotificationsSettingsUiState.Configuration
        assertEquals(true, newUiState?.discussionsPushEnabled)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }
}
