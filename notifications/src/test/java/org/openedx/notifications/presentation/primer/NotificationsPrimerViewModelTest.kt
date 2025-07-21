package org.openedx.notifications.presentation.primer

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.openedx.notifications.data.storage.NotificationsPreferences
import org.openedx.notifications.domain.interactor.NotificationsInteractor
import org.openedx.notifications.domain.model.NotificationsConfiguration
import org.openedx.notifications.presentation.NotificationsAnalytics


@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsPrimerViewModelTest {

    @get:Rule
    val taskInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: NotificationsPrimerViewModel
    private val interactor: NotificationsInteractor = mockk(relaxed = true)
    private val analytics: NotificationsAnalytics = mockk(relaxed = true)
    private val preferences: NotificationsPreferences = mockk(relaxed = true)

    private val mockNotificationsConfiguration = NotificationsConfiguration(
        discussionsPushEnabled = true,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        coEvery { interactor.updateNotificationsConfiguration(any()) } returns mockNotificationsConfiguration
    }

    @Test
    fun `init onViewModelCreation`() = runTest {
        viewModel = NotificationsPrimerViewModel(interactor, preferences, analytics)

        coVerify(exactly = 1) { analytics.logScreenEvent(any(), any()) }
        assertEquals(PrimerUIState.ShowDialog, viewModel.uiState.value)
    }

    @Test
    fun `onDiscussionPrimer noThanks`() = runTest {
        viewModel = NotificationsPrimerViewModel(interactor, preferences, analytics)
        viewModel.dismissDialog()

        coVerify(exactly = 1) { analytics.logScreenEvent(any(), any()) }
        coVerify(exactly = 0) { analytics.logEvent(any(), any()) }
        assertEquals(PrimerUIState.DismissDialog, viewModel.uiState.value)
    }

    @Test
    fun `onDiscussionPrimer notifyMe`() = runTest {
        viewModel = NotificationsPrimerViewModel(interactor, preferences, analytics)
        viewModel.hideDialog()

        coVerify(exactly = 1) { analytics.logScreenEvent(any(), any()) }
        coVerify(exactly = 0) { analytics.logEvent(any(), any()) }
        assertEquals(PrimerUIState.HideDialog, viewModel.uiState.value)
    }

    @Test
    fun `enableDiscussionNotificationsPreference success`() = runTest {
        viewModel = NotificationsPrimerViewModel(interactor, preferences, analytics)
        viewModel.enableDiscussionNotificationsPreference()

        coVerify(exactly = 0) { analytics.logEvent(any(), any()) }
        assertEquals(PrimerUIState.DismissDialog, viewModel.uiState.value)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }
}
