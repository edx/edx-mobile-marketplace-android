package org.openedx

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.openedx.app.AppAnalytics
import org.openedx.app.AppViewModel
import org.openedx.app.data.storage.PreferencesManager
import org.openedx.app.deeplink.DeepLinkRouter
import org.openedx.app.room.AppDatabase
import org.openedx.core.DatabaseManager
import org.openedx.core.CoreMocks
import org.openedx.core.config.Config
import org.openedx.core.config.FirebaseConfig
import org.openedx.core.data.model.User
import org.openedx.core.domain.model.AppThemeMode
import org.openedx.core.system.PushGlobalManager
import org.openedx.core.system.notifier.DownloadNotifier
import org.openedx.core.system.notifier.app.AppNotifier
import org.openedx.core.system.notifier.app.LogoutEvent
import org.openedx.core.utils.CrashlyticsHelper
import org.openedx.core.utils.FileUtil

@ExperimentalCoroutinesApi
class AppViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher() // UnconfinedTestDispatcher()

    private val config = mockk<Config>()
    private val notifier = mockk<AppNotifier>()
    private val room = mockk<AppDatabase>()
    private val databaseManager = mockk<DatabaseManager>()
    private val preferencesManager = mockk<PreferencesManager>()
    private val analytics = mockk<AppAnalytics>()
    private val fileUtil = mockk<FileUtil>(relaxed = true)
    private val deepLinkRouter = mockk<DeepLinkRouter>()
    private val context = mockk<Context>()
    private val pushManager = mockk<PushGlobalManager>()

    private val user = User(0, "", "", "")
    private val appThemeMode = AppThemeMode.MATCH_DEVICE
    private val downloadNotifier = mockk<DownloadNotifier>()

    @Before
    fun before() {
        Dispatchers.setMain(dispatcher)
        every { analytics.logEvent(any(), any()) } returns Unit
        every { preferencesManager.user } returns user
        every { preferencesManager.appThemeMode } returns appThemeMode
        mockkObject(CrashlyticsHelper)
        every { CrashlyticsHelper.setUserId(any()) } returns Unit
        every { downloadNotifier.notifier } returns flow { }
    }

    @After
    fun after() {
        Dispatchers.resetMain()
    }

    @Test
    fun setIdSuccess() = runTest {
        every { analytics.setUserIdForSession(any()) } returns Unit
        every { preferencesManager.user } returns CoreMocks.mockUser
        every { notifier.notifier } returns flow { }
        every { preferencesManager.canResetAppDirectory } returns false
        every { preferencesManager.pushToken } returns ""

        val viewModel = AppViewModel(
            config,
            notifier,
            databaseManager,
            notifier,
            room,
            preferencesManager,
            dispatcher,
            analytics,
            deepLinkRouter,
            fileUtil,
            pushManager,
            downloadNotifier,
            context
        )

        val mockLifeCycleOwner: LifecycleOwner = mockk()
        val lifecycleRegistry = LifecycleRegistry(mockLifeCycleOwner)
        lifecycleRegistry.addObserver(viewModel)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        advanceUntilIdle()

        verify(exactly = 1) { analytics.setUserIdForSession(any()) }
    }

    @Test
    fun forceLogout() = runTest {
        every { notifier.notifier } returns flow {
            emit(LogoutEvent(true))
        }
        every { preferencesManager.clearCorePreferences() } returns Unit
        every { analytics.setUserIdForSession(any()) } returns Unit
        every { databaseManager.clearTables() } returns Unit
        every { analytics.logoutEvent(true) } returns Unit
        every { preferencesManager.canResetAppDirectory } returns false
        every { preferencesManager.pushToken } returns ""
        every { config.getFirebaseConfig() } returns FirebaseConfig()

        val viewModel = AppViewModel(
            config,
            notifier,
            databaseManager,
            notifier,
            room,
            preferencesManager,
            dispatcher,
            analytics,
            deepLinkRouter,
            fileUtil,
            pushManager,
            downloadNotifier,
            context,
        )

        val mockLifeCycleOwner: LifecycleOwner = mockk()
        val lifecycleRegistry = LifecycleRegistry(mockLifeCycleOwner)
        lifecycleRegistry.addObserver(viewModel)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        advanceUntilIdle()

        verify(exactly = 1) { analytics.logoutEvent(true) }
        assert(viewModel.logoutUser.value != null)
    }

    @Test
    fun forceLogoutTwice() = runTest {
        every { notifier.notifier } returns flow {
            emit(LogoutEvent(true))
            emit(LogoutEvent(true))
        }
        every { preferencesManager.clearCorePreferences() } returns Unit
        every { analytics.setUserIdForSession(any()) } returns Unit
        every { preferencesManager.user } returns CoreMocks.mockUser
        every { room.clearAllTables() } returns Unit
        every { analytics.logoutEvent(true) } returns Unit
        every { preferencesManager.canResetAppDirectory } returns false
        every { preferencesManager.pushToken } returns ""
        every { config.getFirebaseConfig() } returns FirebaseConfig()

        val viewModel = AppViewModel(
            config,
            notifier,
            databaseManager,
            notifier,
            room,
            preferencesManager,
            dispatcher,
            analytics,
            deepLinkRouter,
            fileUtil,
            pushManager,
            downloadNotifier,
            context
        )

        val mockLifeCycleOwner: LifecycleOwner = mockk()
        val lifecycleRegistry = LifecycleRegistry(mockLifeCycleOwner)
        lifecycleRegistry.addObserver(viewModel)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        advanceUntilIdle()

        verify(exactly = 1) { analytics.logoutEvent(true) }
        verify(exactly = 1) { preferencesManager.clearCorePreferences() }
        verify(exactly = 1) { analytics.setUserIdForSession(any()) }
        verify(exactly = 2) { preferencesManager.user }
        verify(exactly = 1) { databaseManager.clearTables() }
        verify(exactly = 1) { analytics.logoutEvent(true) }
    }
}
