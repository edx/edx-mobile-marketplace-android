package org.openedx.notifications.presentation.settings

sealed class NotificationsSettingsUiState {
    data class Configuration(
        val discussionsPushEnabled: Boolean = false,
    ) : NotificationsSettingsUiState()
}

sealed class NotificationsSettingsUiEvent {
    data object RequestPermission : NotificationsSettingsUiEvent()
    data object ShowPermissionDialogRationale : NotificationsSettingsUiEvent()
    data object None : NotificationsSettingsUiEvent()
}
