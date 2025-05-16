package org.openedx.core.domain.model

import androidx.annotation.StringRes
import org.openedx.core.R

enum class AppThemeMode(
    val value: String,
    @StringRes val titleResId: Int,
    @StringRes val descriptionResId: Int = 0,
) {
    LIGHT(
        value = "light",
        titleResId = R.string.core_light_mode,
    ),

    DARK(
        value = "dark",
        titleResId = R.string.core_dark_mode,
    ),

    MATCH_DEVICE(
        value = "auto",
        titleResId = R.string.core_match_device,
        descriptionResId = R.string.core_match_device_description,
    )
}
