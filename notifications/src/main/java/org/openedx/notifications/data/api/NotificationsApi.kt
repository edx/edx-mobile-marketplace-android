package org.openedx.notifications.data.api

import org.openedx.notifications.data.model.NotificationsCountResponse
import retrofit2.http.GET

interface NotificationsApi {
    @GET(APIConstants.NOTIFICATION_COUNT)
    suspend fun getUnreadNotificationsCount(): NotificationsCountResponse
}
