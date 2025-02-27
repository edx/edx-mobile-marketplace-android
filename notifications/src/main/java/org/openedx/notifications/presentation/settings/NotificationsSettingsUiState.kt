package org.openedx.notifications.presentation.settings

sealed class NotificationsSettingsUiState {
    data class Configuration(
        val requestPermission: Boolean = false,
        val showPermissionDialogRationale: Boolean = false,
        val discussionsPushEnabled: Boolean = false,
    ) : NotificationsSettingsUiState()
}
