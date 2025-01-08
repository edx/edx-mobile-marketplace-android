package org.openedx.notifications.data.api

import org.openedx.notifications.data.model.InboxNotificationsResponse
import org.openedx.notifications.data.model.NotificationsCountResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface NotificationsApi {
    @GET(APIConstants.NOTIFICATION_COUNT)
    suspend fun getUnreadNotificationsCount(): NotificationsCountResponse

    @GET(APIConstants.NOTIFICATIONS_INBOX)
    suspend fun getInboxNotifications(
        @Query("app_name") appName: String,
        @Query("page") page: Int,
    ): InboxNotificationsResponse
}
