package org.openedx.core.domain.model

import java.io.Serializable

data class AppConfig(
    val courseDatesCalendarSync: CourseDatesCalendarSync = CourseDatesCalendarSync(),
    val iapConfig: IAPConfig = IAPConfig(),
    val feedbackFormUrl: String = "",
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
    val disableVersions: List<String> = listOf()
) : Serializable
