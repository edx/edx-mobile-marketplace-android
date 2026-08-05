package org.openedx.core.system

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.openedx.core.config.SubscriptionBannerConfig
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.data.storage.SubscriptionBannerStorage
import org.openedx.core.domain.model.AppConfig
import org.openedx.core.module.subscriptionBanner.SubscriptionAlertBanner

class AppSessionTrackerTest {

    private val corePreferences = mockk<CorePreferences>()
    private val storage = mockk<SubscriptionBannerStorage>(relaxed = true)

    private val tracker = AppSessionTracker(corePreferences, storage)

    @Test
    fun `onAppForegrounded increments global session count when feature enabled`() {
        val appConfig = mockk<AppConfig>()
        val bannerConfig = SubscriptionBannerConfig(
            isEnabled = true,
            maxSessions = 4,
        )
        every { appConfig.subscriptionBannerConfig } returns bannerConfig
        every { corePreferences.appConfig } returns appConfig
        every { storage.getSubscriptionBannerSessionCount() } returns 2

        tracker.onAppForegrounded()

        verify { storage.setSubscriptionBannerSessionCount(3) }
        SubscriptionAlertBanner.Screen.entries.forEach { screen ->
            verify { storage.setSubscriptionBannerDismissed(screen.key, false) }
        }
    }

    @Test
    fun `onAppForegrounded does not increment when feature is disabled`() {
        val appConfig = mockk<AppConfig>()
        val bannerConfig = SubscriptionBannerConfig(
            isEnabled = false,
            maxSessions = 4,
        )
        every { appConfig.subscriptionBannerConfig } returns bannerConfig
        every { corePreferences.appConfig } returns appConfig

        tracker.onAppForegrounded()

        verify(exactly = 0) { storage.setSubscriptionBannerSessionCount(any()) }
    }

    @Test
    fun `onAppForegrounded increments from zero to one on first app session`() {
        val appConfig = mockk<AppConfig>()
        val bannerConfig = SubscriptionBannerConfig(
            isEnabled = true,
            maxSessions = 4,
        )
        every { appConfig.subscriptionBannerConfig } returns bannerConfig
        every { corePreferences.appConfig } returns appConfig
        every { storage.getSubscriptionBannerSessionCount() } returns 0

        tracker.onAppForegrounded()

        verify { storage.setSubscriptionBannerSessionCount(1) }
    }

    @Test
    fun `onAppForegrounded ignores repeated foreground callback until backgrounded`() {
        val appConfig = mockk<AppConfig>()
        val bannerConfig = SubscriptionBannerConfig(
            isEnabled = true,
            maxSessions = 4,
        )
        every { appConfig.subscriptionBannerConfig } returns bannerConfig
        every { corePreferences.appConfig } returns appConfig
        every { storage.getSubscriptionBannerSessionCount() } returns 0

        tracker.onAppForegrounded()
        tracker.onAppForegrounded()

        verify(exactly = 1) { storage.setSubscriptionBannerSessionCount(1) }
    }

    @Test
    fun `onAppBackgrounded allows counting next foreground as new session`() {
        val appConfig = mockk<AppConfig>()
        val bannerConfig = SubscriptionBannerConfig(
            isEnabled = true,
            maxSessions = 4,
        )
        every { appConfig.subscriptionBannerConfig } returns bannerConfig
        every { corePreferences.appConfig } returns appConfig
        every { storage.getSubscriptionBannerSessionCount() } returnsMany listOf(0, 1)

        tracker.onAppForegrounded()
        tracker.onAppBackgrounded()
        tracker.onAppForegrounded()

        verify { storage.setSubscriptionBannerSessionCount(1) }
        verify { storage.setSubscriptionBannerSessionCount(2) }
    }
}

