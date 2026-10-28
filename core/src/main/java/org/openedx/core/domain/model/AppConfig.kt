package org.openedx.core.domain.model

import org.openedx.core.config.SubscriptionBannerConfig
import java.io.Serializable

data class AppConfig(
    val courseDatesCalendarSync: CourseDatesCalendarSync = CourseDatesCalendarSync(),
    val iapConfig: IAPConfig = IAPConfig(),
    val feedbackFormUrl: String = "",
    val subscriptionBannerConfig: SubscriptionBannerConfig = SubscriptionBannerConfig()
) : Serializable

data class CourseDatesCalendarSync(
    val isEnabled: Boolean = false,
    val isSelfPacedEnabled: Boolean = false,
    val isInstructorPacedEnabled: Boolean = false,
    val isDeepLinkEnabled: Boolean = false,
) : Serializable

data class IAPConfig(
    val isEnabled: Boolean = false,
    val productPrefix: String? = null,
    private val disableVersions: List<String> = listOf()
) : Serializable {

    fun isUpgradeEnabled(versionName: String): Boolean {
        return isEnabled && disableVersions.contains(versionName).not()
    }
}
