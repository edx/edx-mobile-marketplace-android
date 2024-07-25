package org.openedx.core.domain.model

import java.io.Serializable

data class AppConfig(
    val courseDatesCalendarSync: CourseDatesCalendarSync,
    val iapConfig: IAPConfig = IAPConfig(),
) : Serializable

data class CourseDatesCalendarSync(
    val isEnabled: Boolean,
    val isSelfPacedEnabled: Boolean,
    val isInstructorPacedEnabled: Boolean,
    val isDeepLinkEnabled: Boolean,
) : Serializable

data class IAPConfig(
    val isEnabled: Boolean = false,
    val productPrefix: String? = null,
    val disableVersions: List<String> = listOf()
) : Serializable
