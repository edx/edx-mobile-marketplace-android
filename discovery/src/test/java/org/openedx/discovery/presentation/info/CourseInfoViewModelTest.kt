package org.openedx.discovery.presentation.info

import androidx.fragment.app.FragmentManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.openedx.core.config.Config
import org.openedx.core.config.DiscoveryConfig
import org.openedx.core.config.DiscoveryWebViewConfig
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.presentation.global.AppData
import org.openedx.core.system.AppCookieManager
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.DiscoveryNotifier
import org.openedx.core.utils.Logger
import org.openedx.discovery.domain.interactor.DiscoveryInteractor
import org.openedx.discovery.presentation.DiscoveryAnalytics
import org.openedx.discovery.presentation.DiscoveryRouter

class CourseInfoViewModelTest {

    private val appData = mockk<AppData>(relaxed = true)
    private val config = mockk<Config>()
    private val networkConnection = mockk<NetworkConnection>(relaxed = true)
    private val router = mockk<DiscoveryRouter>(relaxed = true)
    private val interactor = mockk<DiscoveryInteractor>(relaxed = true)
    private val notifier = mockk<DiscoveryNotifier>(relaxed = true)
    private val resourceManager = mockk<ResourceManager>(relaxed = true)
    private val analytics = mockk<DiscoveryAnalytics>(relaxed = true)
    private val cookieManager = mockk<AppCookieManager>(relaxed = true)
    private val corePreferences = mockk<CorePreferences>()
    private val fragmentManager = mockk<FragmentManager>(relaxed = true)

    @Before
    fun setUp() {
        every { config.getDiscoveryConfig() } returns DiscoveryConfig(
            webViewConfig = DiscoveryWebViewConfig(
                baseUrl = "https://courses.edx.org",
                courseUrlTemplate = "https://courses.edx.org/learn/{path_id}",
                programUrlTemplate = "https://courses.edx.org/programs/{path_id}",
            )
        )
        every { config.isPreLoginExperienceEnabled() } returns false
        every { corePreferences.user } returns null
        mockkConstructor(Logger::class)
    }

    @Test
    fun `infoCardClicked routes to course info`() {
        val viewModel = createViewModel()

        viewModel.infoCardClicked(
            fragmentManager = fragmentManager,
            pathId = "course-v1:test+TST101+2026",
            infoType = "PROGRAM_INFO",
        )

        verify(exactly = 1) {
            router.navigateToCourseInfo(
                fm = fragmentManager,
                courseId = "course-v1:test+TST101+2026",
                infoType = "PROGRAM_INFO",
            )
        }
        verify(exactly = 0) { router.navigateToEnrolledProgramInfo(any(), any()) }
    }

    @Test
    fun `enrolledProgramInfoClicked routes to enrolled program info`() {
        val viewModel = createViewModel()

        viewModel.enrolledProgramInfoClicked(
            fragmentManager = fragmentManager,
            pathId = "program-id-123",
        )

        verify(exactly = 1) {
            router.navigateToEnrolledProgramInfo(
                fm = fragmentManager,
                pathId = "program-id-123",
            )
        }
        verify(exactly = 0) { router.navigateToCourseInfo(any(), any(), any()) }
    }

    private fun createViewModel(): CourseInfoViewModel {
        return CourseInfoViewModel(
            pathId = "path-id",
            infoType = "COURSE_INFO",
            appData = appData,
            config = config,
            networkConnection = networkConnection,
            router = router,
            interactor = interactor,
            notifier = notifier,
            resourceManager = resourceManager,
            analytics = analytics,
            edxCookieManager = cookieManager,
            corePreferences = corePreferences,
        )
    }
}

