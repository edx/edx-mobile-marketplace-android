package org.openedx.core.system

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.openedx.core.config.Config
import org.openedx.core.config.SubscriptionBannerConfig
import org.openedx.core.data.storage.SubscriptionBannerStorage

class AppSessionTrackerTest {

    private val config = mockk<Config>()
    private val storage = mockk<SubscriptionBannerStorage>(relaxed = true)

    private val tracker = AppSessionTracker(config, storage)

    @Test
    fun `onAppForegrounded increments session count when feature enabled`() {
        every { config.getSubscriptionBannerConfig() } returns SubscriptionBannerConfig(
            isEnabled = true,
            maxSessions = 4,
        )
        every { storage.getSubscriptionBannerSessionCount() } returns 2

        tracker.onAppForegrounded()

        verify { storage.setSubscriptionBannerSessionCount(3) }
    }

    @Test
    fun `onAppForegrounded does not increment when feature is disabled`() {
        every { config.getSubscriptionBannerConfig() } returns SubscriptionBannerConfig(
            isEnabled = false,
            maxSessions = 4,
        )

        tracker.onAppForegrounded()

        verify(exactly = 0) { storage.setSubscriptionBannerSessionCount(any()) }
    }

    @Test
    fun `onAppForegrounded does not increment when maxSessions is zero`() {
        every { config.getSubscriptionBannerConfig() } returns SubscriptionBannerConfig(
            isEnabled = true,
            maxSessions = 0,
        )

        tracker.onAppForegrounded()

        verify(exactly = 0) { storage.setSubscriptionBannerSessionCount(any()) }
    }

    @Test
    fun `onAppForegrounded increments from zero to one on first session`() {
        every { config.getSubscriptionBannerConfig() } returns SubscriptionBannerConfig(
            isEnabled = true,
            maxSessions = 4,
        )
        every { storage.getSubscriptionBannerSessionCount() } returns 0

        tracker.onAppForegrounded()

        verify { storage.setSubscriptionBannerSessionCount(1) }
    }
}

