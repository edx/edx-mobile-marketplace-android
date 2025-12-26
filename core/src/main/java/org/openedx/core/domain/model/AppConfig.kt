package org.openedx.core.domain.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class AppConfig(
    val courseDatesCalendarSync: CourseDatesCalendarSync = CourseDatesCalendarSync(),
    val iapConfig: IAPConfig = IAPConfig(),
    val feedbackFormUrl: String = "",
) : Serializable

data class CourseDatesCalendarSync(
    @SerializedName("is_enabled")
    val isEnabled: Boolean = false,
    @SerializedName("is_self_paced_enabled")
    val isSelfPacedEnabled: Boolean = false,
    @SerializedName("is_instructor_paced_enabled")
    val isInstructorPacedEnabled: Boolean = false,
    @SerializedName("is_deep_link_enabled")
    val isDeepLinkEnabled: Boolean = false,
) : java.io.Serializable

data class IAPConfig(
    val isEnabled: Boolean = false,
    val productPrefix: String? = null,
    private val disableVersions: List<String> = listOf()
) : java.io.Serializable {

    fun isUpgradeEnabled(versionName: String): Boolean {
        return isEnabled && disableVersions.contains(versionName).not()
    }
}
