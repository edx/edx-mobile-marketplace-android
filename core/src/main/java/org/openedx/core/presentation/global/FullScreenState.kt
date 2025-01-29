package org.openedx.core.presentation.global

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material.icons.outlined.SignalWifiStatusbarConnectedNoInternet4
import androidx.compose.ui.graphics.vector.ImageVector
import org.openedx.core.R

open class FullScreenState(
    val imageVector: ImageVector,
    @StringRes val titleResId: Int,
    @StringRes val descriptionResId: Int,
    @StringRes val actionButtonResId: Int? = null,
) {
    companion object {
        val NetworkError = FullScreenState(
            imageVector = Icons.Outlined.SignalWifiStatusbarConnectedNoInternet4,
            titleResId = R.string.core_no_internet_connection,
            descriptionResId = R.string.core_no_internet_connection_description,
            actionButtonResId = R.string.core_reload
        )

        val ServerError = FullScreenState(
            imageVector = Icons.Outlined.PriorityHigh,
            titleResId = R.string.core_server_error,
            descriptionResId = R.string.core_server_error_description,
            actionButtonResId = R.string.core_reload
        )
    }
}
